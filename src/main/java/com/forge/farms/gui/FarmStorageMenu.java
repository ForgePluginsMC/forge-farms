package com.forge.farms.gui;

import com.forge.farms.ForgeFarms;
import com.forge.farms.core.Text;
import com.forge.farms.farm.Farm;
import java.util.HashMap;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Virtual storage view. Click an item to withdraw it into your inventory.
 * Shows up to 54 slots; larger farms show the first 54.
 */
public final class FarmStorageMenu extends Menu {
    private final Farm farm;

    public FarmStorageMenu(ForgeFarms plugin, Farm farm) {
        super(plugin);
        this.farm = farm;
    }

    @Override
    public void build(Player viewer) {
        create(viewer, 54, "<dark_green><bold>Farm Storage</bold></dark_green> <gray>("
                + farm.usedSlots() + "/" + farm.storageSlots() + ")</gray>");
        int shown = Math.min(54, farm.storageSlots());
        for (int i = 0; i < shown; i++) {
            ItemStack item = farm.getSlot(i);
            inventory.setItem(i, item);
        }
        if (farm.storageSlots() > 54) {
            inventory.setItem(53, button(Material.PAPER, "<yellow>More slots</yellow>",
                    "<gray>This farm has <white>" + farm.storageSlots()
                            + "</white> slots; showing the first 54.</gray>"));
        }
    }

    @Override
    public void click(Player viewer, InventoryClickEvent event) {
        if (!isTopClick(event)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= Math.min(54, farm.storageSlots())) {
            return;
        }
        ItemStack stored = farm.getSlot(slot);
        if (stored == null) {
            return;
        }
        HashMap<Integer, ItemStack> leftover = viewer.getInventory().addItem(stored.clone());
        if (leftover.isEmpty()) {
            farm.setSlot(slot, null);
        } else {
            ItemStack rest = leftover.values().iterator().next();
            stored.setAmount(rest.getAmount());
            farm.setSlot(slot, stored);
        }
        plugin.farms().saveFarm(farm);
        plugin.menus().refresh(viewer);
        viewer.sendMessage(Text.mm("<gray>Withdrew items from farm storage.</gray>"));
    }
}
