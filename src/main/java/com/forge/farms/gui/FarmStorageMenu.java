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
 * 45 slots per page; the bottom row is navigation.
 */
public final class FarmStorageMenu extends Menu {
    private static final int PER_PAGE = 45;
    private final Farm farm;
    private final int page;

    public FarmStorageMenu(ForgeFarms plugin, Farm farm) {
        this(plugin, farm, 0);
    }

    private FarmStorageMenu(ForgeFarms plugin, Farm farm, int page) {
        super(plugin);
        this.farm = farm;
        this.page = page;
    }

    @Override
    public void build(Player viewer) {
        int pages = Math.max(1, (farm.storageSlots() + PER_PAGE - 1) / PER_PAGE);
        int p = Math.min(page, pages - 1);
        create(viewer, 54, "<dark_green><bold>Farm Storage</bold></dark_green> <gray>("
                + farm.usedSlots() + "/" + farm.storageSlots()
                + (pages > 1 ? " · " + (p + 1) + "/" + pages : "") + ")</gray>");
        int start = p * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < farm.storageSlots(); i++) {
            ItemStack item = farm.getSlot(start + i);
            inventory.setItem(i, item);
        }
        if (p > 0) {
            inventory.setItem(48, button(Material.ARROW, "<yellow>Previous page</yellow>"));
        }
        if (p < pages - 1) {
            inventory.setItem(50, button(Material.ARROW, "<yellow>Next page</yellow>"));
        }
        navRow();
    }

    @Override
    public void click(Player viewer, InventoryClickEvent event) {
        if (!isTopClick(event)) {
            return;
        }
        int raw = event.getRawSlot();
        int pages = Math.max(1, (farm.storageSlots() + PER_PAGE - 1) / PER_PAGE);
        if (raw == 48 && page > 0) {
            plugin.menus().open(viewer, new FarmStorageMenu(plugin, farm, page - 1).withParent(parent()));
            return;
        }
        if (raw == 50 && page < pages - 1) {
            plugin.menus().open(viewer, new FarmStorageMenu(plugin, farm, page + 1).withParent(parent()));
            return;
        }
        if (navClick(viewer, event)) {
            return;
        }
        if (raw < 0 || raw >= PER_PAGE) {
            return;
        }
        int slot = page * PER_PAGE + raw;
        if (slot >= farm.storageSlots()) {
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
