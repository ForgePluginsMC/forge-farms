package com.forge.farms.gui;

import com.forge.farms.ForgeFarms;
import com.forge.farms.core.Text;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

/** Base class for ForgeFarms chest GUIs. */
public abstract class Menu {
    protected final ForgeFarms plugin;
    protected Inventory inventory = null;
    private @Nullable Menu parent = null;

    protected Menu(ForgeFarms plugin) {
        this.plugin = plugin;
    }

    /**
     * The menu that opened this one. Enables the back button via
     * {@link #navRow()}. Set before {@code plugin.menus().open(...)}.
     */
    public Menu withParent(@Nullable Menu parent) {
        this.parent = parent;
        return this;
    }

    /** The menu that opened this one (may be null). */
    protected @Nullable Menu parent() {
        return parent;
    }

    /** Standard nav row: back (bottom-left, when there is a parent) + close (bottom-right). */
    protected void navRow() {
        int size = inventory.getSize();
        if (parent != null) {
            inventory.setItem(size - 9, button(Material.ARROW, "<yellow>Back</yellow>",
                    "<gray>Return to the previous menu.</gray>"));
        }
        inventory.setItem(size - 1, button(Material.BARRIER, "<red>Close</red>",
                "<gray>Close this menu.</gray>"));
    }

    /**
     * Handle back/close clicks. Call first in {@link #click}; returns true
     * when the click was consumed.
     */
    protected boolean navClick(Player viewer, InventoryClickEvent event) {
        int size = inventory.getSize();
        int slot = event.getRawSlot();
        if (slot == size - 1) {
            plugin.menus().close(viewer);
            return true;
        }
        if (parent != null && slot == size - 9) {
            plugin.menus().open(viewer, parent);
            return true;
        }
        return false;
    }

    /** Build (or rebuild) the inventory for the viewer. */
    public abstract void build(Player viewer);

    /** Handle a cancelled click inside this menu. */
    public abstract void click(Player viewer, InventoryClickEvent event);

    public Inventory inventory() {
        if (inventory == null) {
            throw new IllegalStateException("Menu not built");
        }
        return inventory;
    }

    protected Inventory create(Player viewer, int size, String title) {
        inventory = Bukkit.createInventory(null, size, Text.mm(Text.smallCaps(title)));
        return inventory;
    }

    protected ItemStack button(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.mm(Text.smallCaps(name)));
        List<Component> lines = new ArrayList<>();
        for (String line : lore) {
            lines.add(Text.mm(Text.smallCaps(line)));
        }
        meta.lore(lines);
        item.setItemMeta(meta);
        return item;
    }

    protected ItemStack button(Material material, String name, String... lore) {
        return button(material, name, List.of(lore));
    }

    protected void fill(ItemStack filler) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    protected ItemStack filler() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.empty());
        pane.setItemMeta(meta);
        return pane;
    }

    protected boolean isTopClick(InventoryClickEvent event) {
        return event.getRawSlot() >= 0 && event.getRawSlot() < inventory.getSize();
    }
}
