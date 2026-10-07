package com.forge.farms.gui.admin;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.FarmType;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Admin: hologram toggle and text lines. */
public final class HologramMenu extends TypeMenu {
    public HologramMenu(ForgeFarms plugin, String typeId) {
        super(plugin, typeId);
    }

    @Override
    public void build(Player viewer) {
        FarmType type = type();
        create(viewer, 27, "<dark_red><bold>Hologram</bold></dark_red> <gray>· " + typeId + "</gray>");
        fill(filler());
        if (type == null) {
            navRow();
            return;
        }
        List<String> enLore = new ArrayList<>();
        enLore.add("<gray>Floating info hologram above the core.</gray>");
        enLore.add("");
        enLore.add("<gray>Currently: <white>" + (type.hologramDefault() ? "ON" : "OFF") + "</white></gray>");
        enLore.add("");
        enLore.add("<yellow>Click to toggle.</yellow>");
        inventory.setItem(10, button(type.hologramDefault() ? Material.SEA_LANTERN : Material.GRAY_DYE,
                "<gold>Hologram</gold>", enLore));

        List<String> lineLore = new ArrayList<>();
        lineLore.add("<gray>" + type.hologramLines().size() + " line(s).</gray>");
        lineLore.add("");
        lineLore.add("<yellow>Click to edit lines.</yellow>");
        inventory.setItem(12, button(Material.PAPER, "<gold>Text lines</gold>", lineLore));

        List<String> refLore = new ArrayList<>();
        refLore.add("<gray>Available placeholders:</gray>");
        refLore.add("<white>{owner} {type}</white>");
        refLore.add("<white>{fuel_percent} {fuel_time}</white>");
        refLore.add("<white>{storage_used} {storage_slots}</white>");
        refLore.add("<white>{harvested} {radius}</white>");
        refLore.add("<white>{interval}</white>");
        inventory.setItem(14, button(Material.BOOK, "<gold>Placeholders</gold>", refLore));
        navRow();
    }

    @Override
    public void click(Player viewer, InventoryClickEvent event) {
        if (!isTopClick(event) || navClick(viewer, event)) {
            return;
        }
        FarmType type = type();
        if (type == null) {
            return;
        }
        switch (event.getRawSlot()) {
            case 10 -> {
                if (saveScalar(viewer, "hologram.enabled", String.valueOf(!type.hologramDefault()))) {
                    plugin.holograms().refreshAll(typeId);
                }
            }
            case 12 -> plugin.menus().open(viewer, new StringListMenu(plugin, typeId,
                    "hologram.lines", "Hologram lines", "Type a hologram line (MiniMessage):",
                    type.hologramLines()).withParent(this));
            default -> {
            }
        }
    }
}
