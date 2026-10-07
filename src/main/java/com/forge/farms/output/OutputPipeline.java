package com.forge.farms.output;

import com.forge.farms.ForgeFarms;
import com.forge.farms.api.event.FarmStorageFullEvent;
import com.forge.farms.core.Text;
import com.forge.farms.farm.Farm;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Composable harvest outputs, run in the farm's priority order:
 * virtual storage, then adjacent containers (built-in hopper automation),
 * then economy auto-sell. Anything left over drops at the farm core.
 * Replaces the paid UpgradeableHoppers dependency.
 */
public final class OutputPipeline {
    private static final BlockFace[] FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    };

    private final ForgeFarms plugin;
    private final EconomyBridge economy;

    public OutputPipeline(ForgeFarms plugin) {
        this.plugin = plugin;
        this.economy = new EconomyBridge(plugin);
    }

    public EconomyBridge economy() {
        return economy;
    }

    /** Route freshly harvested drops through the farm's output modes. */
    public void handleHarvest(Farm farm, List<ItemStack> drops) {
        List<ItemStack> remaining = new ArrayList<>(drops);
        for (OutputMode mode : farm.outputPriority()) {
            if (remaining.isEmpty()) {
                break;
            }
            remaining = switch (mode) {
                case STORAGE -> toStorage(farm, remaining);
                case HOPPER -> toHoppers(farm, remaining);
                case SELL -> autoSell(farm, remaining);
            };
        }
        if (!remaining.isEmpty()) {
            Bukkit.getPluginManager().callEvent(new FarmStorageFullEvent(farm, remaining));
            dropAtCore(farm, remaining);
        }
    }

    private List<ItemStack> toStorage(Farm farm, List<ItemStack> items) {
        return farm.addItems(items);
    }

    /**
     * Push items into any container adjacent to the farm core —
     * hoppers, chests, barrels. No hopper-upgrade item needed.
     */
    private List<ItemStack> toHoppers(Farm farm, List<ItemStack> items) {
        Location core = plugin.farms().coreLocation(farm);
        if (core == null || core.getWorld() == null) {
            return items;
        }
        Block coreBlock = core.getBlock();
        List<ItemStack> remaining = new ArrayList<>(items);
        for (BlockFace face : FACES) {
            if (remaining.isEmpty()) {
                break;
            }
            BlockState state = coreBlock.getRelative(face).getState();
            if (state instanceof InventoryHolder holder) {
                Inventory inv = holder.getInventory();
                // Never feed the farm's own storage back into itself — core is not a holder, but be safe.
                List<ItemStack> leftover = new ArrayList<>();
                for (ItemStack item : remaining) {
                    var unstored = inv.addItem(item);
                    leftover.addAll(unstored.values());
                }
                remaining = leftover;
            }
        }
        return remaining;
    }

    /**
     * Sell priced items straight into the owner's economy balance.
     * Unpriced materials pass through untouched.
     */
    private List<ItemStack> autoSell(Farm farm, List<ItemStack> items) {
        if (!farm.autoSell() || !economy.isAvailable()) {
            return items;
        }
        OfflinePlayer owner = Bukkit.getOfflinePlayer(farm.owner());
        List<ItemStack> remaining = new ArrayList<>();
        double earned = 0;
        for (ItemStack item : items) {
            Double price = plugin.config().sellPrice(item.getType());
            if (price == null || price <= 0) {
                remaining.add(item);
                continue;
            }
            earned += price * item.getAmount();
        }
        if (earned > 0 && economy.deposit(owner, earned)) {
            // Sold — nothing passes through.
        } else if (earned > 0) {
            // Deposit failed — keep everything rather than void it.
            return items;
        }
        return remaining;
    }

    private void dropAtCore(Farm farm, List<ItemStack> items) {
        Location core = plugin.farms().coreLocation(farm);
        if (core == null || core.getWorld() == null) {
            return;
        }
        Location at = core.clone().add(0, 1, 0);
        for (ItemStack item : items) {
            core.getWorld().dropItemNaturally(at, item);
        }
    }

    /** Human-readable money for GUIs. */
    public String formatMoney(double amount) {
        return String.format("$%,.2f", amount);
    }

    /** Sell price for one item, or null when unpriced. */
    public @Nullable Double priceOf(Material material) {
        return plugin.config().sellPrice(material);
    }

    public void tellNoEconomy(org.bukkit.entity.Player player) {
        player.sendMessage(Text.mm("<red>Money features need Vault + an economy plugin.</red>"));
    }
}
