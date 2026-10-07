package com.forge.farms.listener;

import com.forge.farms.ForgeFarms;
import com.forge.farms.api.ForgeFarmsAPI;
import com.forge.farms.config.FarmType;
import com.forge.farms.core.Text;
import com.forge.farms.farm.Farm;
import com.forge.farms.gui.FarmMainMenu;
import com.forge.farms.members.FarmFlag;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Placement, interaction, protection, tree capture, and join catch-up.
 */
public final class FarmListener implements Listener {
    private final ForgeFarms plugin;

    public FarmListener(ForgeFarms plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack held = event.getItem();

        // Placing a farm item creates the farm core.
        if (held != null && ForgeFarmsAPI.isFarmItem(held)) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
                event.setCancelled(true);
                tryPlaceFarm(player, held, event.getClickedBlock(), event.getBlockFace());
            }
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        Farm farm = plugin.farms().getByCore(event.getClickedBlock());
        if (farm == null) {
            return;
        }
        event.setCancelled(true);
        FarmType type = plugin.config().getType(farm.typeId());
        if (type == null) {
            return;
        }
        ItemStack main = player.getInventory().getItemInMainHand();
        if (plugin.fuel().isFuelItem(type, main)) {
            if (!plugin.trust().can(player, farm, FarmFlag.FUEL)) {
                player.sendMessage(Text.mm("<red>You can't add fuel to this farm.</red>"));
                return;
            }
            plugin.fuel().addFuel(player, farm, type, main);
            return;
        }
        if (!plugin.trust().can(player, farm, FarmFlag.INTERACT)) {
            player.sendMessage(Text.mm("<red>You can't use this farm.</red>"));
            return;
        }
        plugin.menus().open(player, new FarmMainMenu(plugin, farm));
    }

    private void tryPlaceFarm(Player player, ItemStack item, Block clicked, BlockFace face) {
        String typeId = ForgeFarmsAPI.farmItemType(item);
        FarmType type = plugin.config().getType(typeId);
        if (type == null) {
            player.sendMessage(Text.mm("<red>Unknown farm type on this item.</red>"));
            return;
        }
        int owned = plugin.farms().countByOwner(player.getUniqueId());
        if (owned >= plugin.config().maxFarmsPerPlayer()
                && !player.hasPermission("forgefarms.bypass.limits")) {
            player.sendMessage(Text.mm("<red>Farm limit reached (<white>" + owned + "</white>).</red>"));
            return;
        }
        Block at = clicked.getRelative(face);
        if (!at.getType().isAir()) {
            player.sendMessage(Text.mm("<red>Not enough space there.</red>"));
            return;
        }
        at.setType(type.coreBlock());
        Farm farm = plugin.farms().createFarm(player, type, at.getLocation());
        if (farm == null) {
            at.setType(Material.AIR);
            return;
        }
        if (player.getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }
        player.sendMessage(Text.mm("<green>Farm placed! Right-click it to manage.</green>"));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Farm core = plugin.farms().getByCore(block);
        if (core != null) {
            if (!plugin.trust().can(event.getPlayer(), core, FarmFlag.DELETE)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Text.mm("<red>You can't break this farm.</red>"));
                return;
            }
            // Drop the farm item plus everything in virtual storage.
            ItemStack[] stored = core.storageSnapshot();
            ItemStack farmItem = ForgeFarmsAPI.createFarmItem(core.typeId(), 1);
            if (!plugin.farms().removeFarm(core.id())) {
                return; // removal was vetoed by another plugin
            }
            event.setDropItems(false);
            if (farmItem != null) {
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), farmItem);
            }
            for (ItemStack item : stored) {
                if (item != null && !item.isEmpty()) {
                    block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), item);
                }
            }
            event.getPlayer().sendMessage(Text.mm("<yellow>Farm removed.</yellow>"));
            return;
        }
        Farm farm = plugin.farms().farmContaining(block.getLocation());
        if (farm != null && !plugin.trust().can(event.getPlayer(), farm, FarmFlag.BLOCK_BREAK)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Text.mm("<red>This land belongs to a farm.</red>"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Farm farm = plugin.farms().farmContaining(event.getBlock().getLocation());
        if (farm != null && !plugin.trust().can(event.getPlayer(), farm, FarmFlag.BLOCK_PLACE)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Text.mm("<red>This land belongs to a farm.</red>"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGrow(StructureGrowEvent event) {
        for (Farm farm : plugin.farms().all()) {
            FarmType type = plugin.config().getType(farm.typeId());
            if (type == null || !type.treeFarming()) {
                continue;
            }
            if (!farm.worldId().equals(event.getWorld().getUID())) {
                continue;
            }
            int r = type.radiusAt(farm.radiusLevel());
            int dx = event.getLocation().getBlockX() - farm.x();
            int dz = event.getLocation().getBlockZ() - farm.z();
            if (dx * dx + dz * dz <= r * r) {
                plugin.growth().handleTreeGrow(farm, type, event);
                return;
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.scheduler().async(() -> plugin.farms().applyOfflineCatchup(player));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(b -> plugin.farms().getByCore(b) != null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(b -> plugin.farms().getByCore(b) != null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) {
            if (plugin.farms().getByCore(b) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

}
