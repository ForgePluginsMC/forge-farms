package com.forge.farms.gui.admin;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.FarmType;
import com.forge.farms.core.Text;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Admin hub: every section of one farm type's config. */
public final class TypeEditorMenu extends TypeMenu {
    public TypeEditorMenu(ForgeFarms plugin, String typeId) {
        super(plugin, typeId);
    }

    @Override
    public void build(Player viewer) {
        FarmType type = type();
        create(viewer, 36, "<dark_red><bold>" + (type == null ? typeId : Text.plain(type.displayName()))
                + "</bold></dark_red> <gray>· editor</gray>");
        fill(filler());
        if (type == null) {
            navRow();
            return;
        }
        List<String> head = new ArrayList<>();
        head.add("<gray>id: <white>" + type.id() + "</white></gray>");
        head.add("<gray>Radius <white>" + type.baseRadius() + "</white> · Tick <white>"
                + (type.baseTickInterval() / 20.0) + "s</white> · Storage <white>"
                + type.baseStorageSlots() + "</white></gray>");
        inventory.setItem(4, button(type.itemMaterial(), type.displayName(), head));

        sectionButton(10, Material.COMPARATOR, "<gold>Behavior</gold>",
                "<gray>Radius, tick speed, growth, harvest,</gray>",
                "<gray>storage, offline hours, hologram.</gray>",
                "<yellow>Click to open.</yellow>");
        sectionButton(11, Material.FIREWORK_STAR, "<gold>Effects</gold>",
                "<gray>Particles and sounds, with pickers.</gray>",
                "<yellow>Click to open.</yellow>");
        sectionButton(12, Material.COAL, "<gold>Fuel</gold>",
                "<gray>Burn time and accepted fuel items.</gray>",
                "<yellow>Click to open.</yellow>");
        sectionButton(13, Material.WHEAT, "<gold>Harvest</gold>",
                "<gray>Which blocks this farm grows.</gray>",
                "<yellow>Click to open.</yellow>");
        sectionButton(14, Material.ANVIL, "<gold>Upgrades</gold>",
                "<gray>Radius / speed / storage / efficiency</gray>",
                "<gray>levels and their costs.</gray>",
                "<yellow>Click to open.</yellow>");
        sectionButton(15, Material.GOLD_INGOT, "<gold>Shop</gold>",
                "<gray>Shop listing and price.</gray>",
                "<yellow>Click to open.</yellow>");
        sectionButton(16, Material.SEA_LANTERN, "<gold>Hologram</gold>",
                "<gray>Toggle and text lines.</gray>",
                "<yellow>Click to open.</yellow>");
        navRow();
    }

    @Override
    public void click(Player viewer, InventoryClickEvent event) {
        if (!isTopClick(event) || navClick(viewer, event)) {
            return;
        }
        switch (event.getRawSlot()) {
            case 10 -> plugin.menus().open(viewer, new BehaviorMenu(plugin, typeId).withParent(this));
            case 11 -> plugin.menus().open(viewer, new EffectsMenu(plugin, typeId).withParent(this));
            case 12 -> plugin.menus().open(viewer, new FuelMenu(plugin, typeId).withParent(this));
            case 13 -> plugin.menus().open(viewer, new HarvestMenu(plugin, typeId).withParent(this));
            case 14 -> plugin.menus().open(viewer, new UpgradesMenu(plugin, typeId).withParent(this));
            case 15 -> plugin.menus().open(viewer, new TypeShopMenu(plugin, typeId).withParent(this));
            case 16 -> plugin.menus().open(viewer, new HologramMenu(plugin, typeId).withParent(this));
            default -> {
            }
        }
    }
}
