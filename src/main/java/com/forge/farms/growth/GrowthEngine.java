package com.forge.farms.growth;

import com.forge.farms.ForgeFarms;
import com.forge.farms.api.event.FarmFuelEmptyEvent;
import com.forge.farms.api.event.FarmHarvestEvent;
import com.forge.farms.config.FarmType;
import com.forge.farms.config.HarvestBehavior;
import com.forge.farms.config.Harvestable;
import com.forge.farms.farm.Farm;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.Player;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Per-farm growth ticks. Runs on the farm core's region thread (Folia-safe):
 * fuel is consumed, random blocks in the radius are sampled, mature crops
 * are harvested into the output pipeline, and holograms refresh on a
 * throttle.
 */
public final class GrowthEngine {
    private final ForgeFarms plugin;
    private final Random random = new Random();
    private final Set<UUID> fuelEmptyNotified = new HashSet<>();
    private final Map<UUID, Long> lastHologramRefresh = new HashMap<>();
    private final Map<UUID, Integer> tickCount = new HashMap<>();

    /** Blocks the tilling upgrade can turn into farmland. */
    private static final Set<Material> TILLABLE = EnumSet.of(
            Material.DIRT, Material.GRASS_BLOCK, Material.DIRT_PATH,
            Material.COARSE_DIRT, Material.ROOTED_DIRT);
    private final Map<UUID, Integer> effectTick = new HashMap<>();

    public GrowthEngine(ForgeFarms plugin) {
        this.plugin = plugin;
    }

    /** One growth tick for a farm. Called on the farm's region thread. */
    public void tickFarm(Farm farm) {
        FarmType type = plugin.config().getType(farm.typeId());
        if (type == null) {
            return;
        }
        Location core = plugin.farms().coreLocation(farm);
        if (core == null) {
            return;
        }
        World world = core.getWorld();
        farm.setLastTickAt(System.currentTimeMillis());

        // Fuel first: no fuel, no growth (unless bypassed).
        long interval = type.intervalAt(farm.speedLevel());
        double efficiency = type.efficiencyAt(farm.efficiencyLevel());
        long cost = Math.max(1L, (long) (interval / efficiency));
        boolean bypassFuel = hasBypassFuel(farm);
        if (!bypassFuel && !farm.consumeFuel(cost)) {
            if (fuelEmptyNotified.add(farm.id())) {
                Bukkit.getPluginManager().callEvent(new FarmFuelEmptyEvent(farm));
                notifyOwner(farm, "<red>Your " + type.displayName() + " <red>ran out of fuel.</red>");
            }
            return;
        }
        fuelEmptyNotified.remove(farm.id());

        int radius = type.radiusAt(farm.radiusLevel());
        workingEffects(type, world, core, farm, radius);
        int budget = type.maxHarvestsPerTick() <= 0 ? Integer.MAX_VALUE : type.maxHarvestsPerTick();
        List<ItemStack> harvested = new ArrayList<>();
        for (int i = 0; i < type.growthAttempts(); i++) {
            budget = sample(farm, type, world, core, radius, harvested, budget);
        }
        if (type.harvestSweep() && budget > 0) {
            budget = sweep(type, world, core, radius, harvested, budget);
        }
        tillSoil(farm, type, world, core, radius);
        if (!harvested.isEmpty()) {
            FarmHarvestEvent event = new FarmHarvestEvent(farm, harvested);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.getDrops().isEmpty()) {
                plugin.output().handleHarvest(farm, event.getDrops());
                farm.addHarvested(event.getDrops().size());
            }
        }

        // Throttled hologram refresh + periodic save.
        long now = System.currentTimeMillis();
        long refreshMs = plugin.config().hologramRefreshTicks() * 50L;
        Long last = lastHologramRefresh.get(farm.id());
        if (last == null || now - last >= refreshMs) {
            lastHologramRefresh.put(farm.id(), now);
            plugin.holograms().refresh(farm);
        }
        int n = tickCount.getOrDefault(farm.id(), 0) + 1;
        if (n >= 5) {
            n = 0;
            plugin.farms().saveFarm(farm);
        }
        tickCount.put(farm.id(), n);
    }

    /**
     * Tilling upgrade: level 1 plows tillable soil into farmland, level 2
     * also keeps farmland fully hydrated (no water channels needed).
     */
    private void tillSoil(Farm farm, FarmType type, World world, Location core, int radius) {
        int level = farm.tillingLevel();
        if (level <= 0) {
            return;
        }
        for (int i = 0; i < type.tillAttempts(); i++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            int dy = random.nextInt(5) - 2;
            Block block = world.getBlockAt(
                    core.getBlockX() + dx, core.getBlockY() + dy, core.getBlockZ() + dz);
            Material m = block.getType();
            if (TILLABLE.contains(m)) {
                // Only plow open soil — never under existing blocks or crops.
                if (block.getRelative(BlockFace.UP).getType().isAir()) {
                    block.setType(Material.FARMLAND, false);
                    if (level >= 2) {
                        hydrate(block);
                    }
                }
            } else if (level >= 2 && m == Material.FARMLAND) {
                hydrate(block);
            }
        }
    }

    private static void hydrate(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Farmland farmland && farmland.getMoisture() < farmland.getMaximumMoisture()) {
            farmland.setMoisture(farmland.getMaximumMoisture());
            block.setBlockData(farmland, false);
        }
    }

    private boolean hasBypassFuel(Farm farm) {
        Player owner = Bukkit.getPlayer(farm.owner());
        return owner != null && owner.hasPermission("forgefarms.bypass.fuel");
    }

    private void notifyOwner(Farm farm, String miniMessage) {
        Player owner = Bukkit.getPlayer(farm.owner());
        if (owner != null) {
            owner.sendMessage(com.forge.farms.core.Text.mm(miniMessage));
        }
    }

    /** One random sample. Returns the remaining harvest budget. */
    private int sample(Farm farm, FarmType type, World world, Location core, int radius,
            List<ItemStack> harvested, int budget) {
        int dx = random.nextInt(radius * 2 + 1) - radius;
        int dz = random.nextInt(radius * 2 + 1) - radius;
        if (dx * dx + dz * dz > radius * radius) {
            return budget;
        }
        int dy = random.nextInt(7) - 3;
        Block block = world.getBlockAt(core.getBlockX() + dx, core.getBlockY() + dy, core.getBlockZ() + dz);
        for (Harvestable h : type.harvestables()) {
            if (block.getType() == h.material()) {
                if (budget > 0 && tryHarvest(type, h, block, harvested)) {
                    budget--;
                } else {
                    tryGrow(type, h, block);
                }
                return budget;
            }
        }
        return budget;
    }

    /**
     * Full-radius sweep for harvest-ready blocks. This is what makes a
     * maxed farm look dramatic: whole waves of crops pop at once.
     * Returns the remaining harvest budget.
     */
    private int sweep(FarmType type, World world, Location core, int radius,
            List<ItemStack> harvested, int budget) {
        int cx = core.getBlockX();
        int cy = core.getBlockY();
        int cz = core.getBlockZ();
        for (int dx = -radius; dx <= radius && budget > 0; dx++) {
            for (int dz = -radius; dz <= radius && budget > 0; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                for (int dy = -2; dy <= 3 && budget > 0; dy++) {
                    Block block = world.getBlockAt(cx + dx, cy + dy, cz + dz);
                    for (Harvestable h : type.harvestables()) {
                        if (block.getType() == h.material() && tryHarvest(type, h, block, harvested)) {
                            budget--;
                            break;
                        }
                    }
                }
            }
        }
        return budget;
    }

    /** Harvest the block if it is ready. Returns true when something was harvested. */
    private boolean tryHarvest(FarmType type, Harvestable h, Block block,
            List<ItemStack> harvested) {
        boolean got;
        switch (h.behavior()) {
            case AGEABLE_CROP -> {
                BlockData data = block.getBlockData();
                if (!(data instanceof Ageable ageable) || ageable.getAge() < ageable.getMaximumAge()) {
                    return false;
                }
                harvested.addAll(block.getDrops());
                ageable.setAge(0);
                block.setBlockData(ageable);
                got = true;
            }
            case STEM_FRUIT -> {
                harvested.addAll(block.getDrops());
                block.setType(Material.AIR);
                got = true;
            }
            case STALK -> {
                Block below = block.getRelative(org.bukkit.block.BlockFace.DOWN);
                if (below.getType() != block.getType()) {
                    return false;
                }
                Material stalk = block.getType();
                Block cursor = block;
                while (cursor.getType() == stalk) {
                    harvested.addAll(cursor.getDrops());
                    cursor.setType(Material.AIR);
                    cursor = cursor.getRelative(org.bukkit.block.BlockFace.UP);
                }
                got = true;
            }
            default -> {
                return false;
            }
        }
        if (got) {
            harvestEffects(type, block);
        }
        return got;
    }

    /** Accelerate a growing crop. Returns true when it aged up. */
    private boolean tryGrow(FarmType type, Harvestable h, Block block) {
        if (h.behavior() != HarvestBehavior.AGEABLE_CROP) {
            return false;
        }
        BlockData data = block.getBlockData();
        if (!(data instanceof Ageable ageable) || ageable.getAge() >= ageable.getMaximumAge()) {
            return false;
        }
        ageable.setAge(Math.min(ageable.getMaximumAge(), ageable.getAge() + type.agePerSample()));
        block.setBlockData(ageable);
        if (type.growthParticle() != null) {
            block.getWorld().spawnParticle(type.growthParticle(),
                    block.getLocation().add(0.5, 0.6, 0.5), type.growthParticleCount());
        }
        return true;
    }

    /**
     * "When and where it's working": an ambient aura at the core that only
     * appears while the farm is fueled and ticking, plus a particle ring
     * drawn at the working radius on an interval.
     */
    private void workingEffects(FarmType type, World world, Location core, Farm farm, int radius) {
        if (type.workingParticle() != null) {
            world.spawnParticle(type.workingParticle(), core.clone().add(0, 1.3, 0),
                    type.workingParticleCount());
        }
        if (type.radiusParticle() != null) {
            int n = effectTick.getOrDefault(farm.id(), 0) + 1;
            if (n >= Math.max(1, type.radiusParticleInterval())) {
                n = 0;
                drawRadiusRing(type, world, core, radius);
            }
            effectTick.put(farm.id(), n);
        }
    }

    private void drawRadiusRing(FarmType type, World world, Location core, int radius) {
        int points = Math.max(8, type.radiusParticleCount());
        double y = core.getY() + 0.6;
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            world.spawnParticle(type.radiusParticle(),
                    new Location(world, core.getX() + radius * Math.cos(a), y,
                            core.getZ() + radius * Math.sin(a)),
                    1);
        }
    }

    private void harvestEffects(FarmType type, Block block) {
        World world = block.getWorld();
        Location at = block.getLocation().add(0.5, 0.5, 0.5);
        if (type.harvestParticle() != null) {
            world.spawnParticle(type.harvestParticle(), at, type.harvestParticleCount());
        }
        if (type.harvestSound() != null) {
            world.playSound(at, type.harvestSound(), type.harvestSoundVolume(), type.harvestSoundPitch());
        }
    }

    /**
     * Tree farm harvest: when a sapling inside a tree farm's radius matures,
     * the growth is captured — logs go to storage and the sapling is left
     * to grow again. Called from the StructureGrowEvent listener.
     */
    public void handleTreeGrow(Farm farm, FarmType type, StructureGrowEvent event) {
        Location core = plugin.farms().coreLocation(farm);
        if (core == null) {
            return;
        }
        Location grown = event.getLocation();
        int radius = type.radiusAt(farm.radiusLevel());
        int dx = grown.getBlockX() - farm.x();
        int dz = grown.getBlockZ() - farm.z();
        if (dx * dx + dz * dz > radius * radius) {
            return;
        }
        Material sapling = grown.getBlock().getType();
        if (!type.allowedSaplings().isEmpty() && !type.allowedSaplings().contains(sapling)) {
            return;
        }
        List<ItemStack> drops = new ArrayList<>();
        for (BlockState state : event.getBlocks()) {
            String name = state.getType().name();
            if (name.endsWith("_LOG") || name.endsWith("_WOOD") || name.endsWith("_STEM")
                    || name.endsWith("_HYPHAE")) {
                drops.add(new ItemStack(state.getType()));
            } else if (name.endsWith("_LEAVES") && random.nextDouble() < 0.06) {
                drops.add(new ItemStack(sapling));
            }
        }
        if (drops.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        harvestEffects(type, event.getLocation().getBlock());
        FarmHarvestEvent harvest = new FarmHarvestEvent(farm, drops);
        Bukkit.getPluginManager().callEvent(harvest);
        if (!harvest.getDrops().isEmpty()) {
            plugin.output().handleHarvest(farm, harvest.getDrops());
            farm.addHarvested(harvest.getDrops().size());
        }
    }

    /**
     * Conservative offline yield estimate: cycles the type's harvestables
     * into plausible drop stacks. Used by offline catch-up.
     */
    public List<ItemStack> estimateOfflineYield(FarmType type, long count) {
        List<ItemStack> out = new ArrayList<>();
        List<Harvestable> harvestables = type.harvestables();
        if (harvestables.isEmpty()) {
            if (type.treeFarming()) {
                Material log = Material.OAK_LOG;
                if (!type.allowedSaplings().isEmpty()) {
                    Material s = type.allowedSaplings().iterator().next();
                    Material mapped = Material.matchMaterial(s.name().replace("SAPLING", "LOG")
                            .replace("PROPAGULE", "LOG"));
                    if (mapped != null) {
                        log = mapped;
                    }
                }
                while (out.size() < count) {
                    out.add(new ItemStack(log, 1 + random.nextInt(4)));
                }
            }
            return out;
        }
        int i = 0;
        while (out.size() < count) {
            Harvestable h = harvestables.get(i % harvestables.size());
            i++;
            for (ItemStack drop : dropFor(h.material(), h.behavior())) {
                if (out.size() < count) {
                    out.add(drop);
                }
            }
        }
        return out;
    }

    /** Plausible drops for one mature block, mirroring vanilla behavior. */
    private List<ItemStack> dropFor(Material crop, HarvestBehavior behavior) {
        return switch (behavior) {
            case AGEABLE_CROP -> switch (crop) {
                case WHEAT -> List.of(new ItemStack(Material.WHEAT), new ItemStack(Material.WHEAT_SEEDS, 2));
                case CARROTS -> List.of(new ItemStack(Material.CARROT, 3));
                case POTATOES -> List.of(new ItemStack(Material.POTATO, 3));
                case BEETROOTS -> List.of(new ItemStack(Material.BEETROOT, 2), new ItemStack(Material.BEETROOT_SEEDS));
                case NETHER_WART -> List.of(new ItemStack(Material.NETHER_WART, 3));
                case SWEET_BERRY_BUSH -> List.of(new ItemStack(Material.SWEET_BERRIES, 3));
                case COCOA -> List.of(new ItemStack(Material.COCOA_BEANS, 3));
                case PITCHER_CROP -> List.of(new ItemStack(Material.PITCHER_PLANT));
                case TORCHFLOWER_CROP -> List.of(new ItemStack(Material.TORCHFLOWER));
                default -> List.of(new ItemStack(crop));
            };
            case STEM_FRUIT -> switch (crop) {
                case MELON -> List.of(new ItemStack(Material.MELON_SLICE, 5));
                case PUMPKIN -> List.of(new ItemStack(Material.PUMPKIN));
                default -> List.of(new ItemStack(crop));
            };
            case STALK -> switch (crop) {
                case SUGAR_CANE -> List.of(new ItemStack(Material.SUGAR_CANE, 2));
                case CACTUS -> List.of(new ItemStack(Material.CACTUS, 2));
                case BAMBOO -> List.of(new ItemStack(Material.BAMBOO, 3));
                case KELP_PLANT -> List.of(new ItemStack(Material.KELP, 2));
                default -> List.of(new ItemStack(crop));
            };
        };
    }
}
