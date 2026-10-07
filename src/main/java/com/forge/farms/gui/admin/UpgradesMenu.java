package com.forge.farms.gui.admin;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.FarmType;
import com.forge.farms.config.TrackType;
import com.forge.farms.config.UpgradeTrack;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Admin: pick an upgrade track to edit. */
public final class UpgradesMenu extends TypeMenu {
    public UpgradesMenu(ForgeFarms plugin, String typeId) {
        super(plugin, typeId);
    }

    @Override
    public void build(Player viewer) {
        FarmType type = type();
        create(viewer, 36, "<dark_red><bold>Upgrades</bold></dark_red> <gray>· " + typeId + "</gray>");
        fill(filler());
        if (type == null) {
            navRow();
            return;
        }
        trackButton(10, type, TrackType.RADIUS, Material.COMPASS, "Working radius in blocks.");
        trackButton(12, type, TrackType.SPEED, Material.FEATHER, "Growth tick interval.");
        trackButton(14, type, TrackType.STORAGE, Material.CHEST, "Virtual storage slots.");
        trackButton(16, type, TrackType.EFFICIENCY, Material.COAL, "Fuel efficiency multiplier.");
        trackButton(22, type, TrackType.TILLING, Material.IRON_HOE, "Auto-plow soil, hydrate farmland.");
        navRow();
    }

    private void trackButton(int slot, FarmType type, TrackType track, Material icon, String hint) {
        UpgradeTrack t = type.upgradeTrack(track);
        List<String> lore = new ArrayList<>();
        lore.add("<gray>" + hint + "</gray>");
        lore.add("<gray>Levels: <white>" + (t == null ? 0 : t.maxLevel()) + "</white></gray>");
        lore.add("");
        lore.add("<yellow>Click to edit levels.</yellow>");
        inventory.setItem(slot, button(icon, "<gold>" + plugin.upgrades().trackTitle(track) + "</gold>", lore));
    }

    @Override
    public void click(Player viewer, InventoryClickEvent event) {
        if (!isTopClick(event) || navClick(viewer, event)) {
            return;
        }
        TrackType track = switch (event.getRawSlot()) {
            case 10 -> TrackType.RADIUS;
            case 12 -> TrackType.SPEED;
            case 14 -> TrackType.STORAGE;
            case 16 -> TrackType.EFFICIENCY;
            case 22 -> TrackType.TILLING;
            default -> null;
        };
        if (track != null) {
            plugin.menus().open(viewer, new TrackMenu(plugin, typeId, track).withParent(this));
        }
    }
}
