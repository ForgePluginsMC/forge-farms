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

/** Base class for ForgeFarms chest GUIs. */
public abstract class Menu {
    protected final ForgeFarms plugin;
    protected Inventory inventory = null;

    protected Menu(ForgeFarms plugin) {
        this.plugin = plugin;
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
        inventory = Bukkit.createInventory(null, size, Text.mm(title));
        return inventory;
    }

    protected ItemStack button(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.mm(name));
        List<Component> lines = new ArrayList<>();
        for (String line : lore) {
            lines.add(Text.mm(line));
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
