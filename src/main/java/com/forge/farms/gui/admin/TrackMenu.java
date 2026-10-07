package com.forge.farms.gui.admin;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.FarmType;
import com.forge.farms.config.TrackType;
import com.forge.farms.config.TypeConfigWriter;
import com.forge.farms.config.UpgradeLevel;
import com.forge.farms.config.UpgradeTrack;
import com.forge.farms.core.Text;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Admin: levels of one upgrade track. Click a level to edit it. */
public final class TrackMenu extends TypeMenu {
    private final TrackType track;

    public TrackMenu(ForgeFarms plugin, String typeId, TrackType track) {
        super(plugin, typeId);
        this.track = track;
    }

    @Override
    public void build(Player viewer) {
        FarmType type = type();
        create(viewer, 54, "<dark_red><bold>" + plugin.upgrades().trackTitle(track)
                + "</bold></dark_red> <gray>· " + typeId + "</gray>");
        fill(filler());
        if (type == null) {
            navRow();
            return;
        }
        UpgradeTrack t = type.upgradeTrack(track);
        int max = t == null ? 0 : t.maxLevel();
        int slot = 10;
        for (int lvl = 1; lvl <= max && slot <= 34; lvl++) {
            UpgradeLevel level = t.level(lvl);
            List<String> lore = new ArrayList<>();
            String effect = level.description().isEmpty() ? describeEffect(type, lvl) : level.description();
            lore.add("<gray>Effect: <white>" + effect + "</white></gray>");
            lore.add("<gray>Cost: " + level.cost().describe(plugin.output()::formatMoney) + "</gray>");
            lore.add("");
            lore.add("<yellow>Click to edit.</yellow>");
            inventory.setItem(slot++, button(Material.EXPERIENCE_BOTTLE, "<gold>Level " + lvl + "</gold>", lore));
        }
        if (slot <= 34) {
            inventory.setItem(slot, button(Material.EMERALD, "<green>Add level</green>",
                    "<yellow>Click to append a new level.</yellow>"));
        }
        navRow();
    }

    private String describeEffect(FarmType type, int lvl) {
        UpgradeTrack t = type.upgradeTrack(track);
        UpgradeLevel level = t == null ? null : t.level(lvl);
        if (level == null) {
            return "?";
        }
        return switch (track) {
            case RADIUS -> (int) level.effect() + " blocks";
            case SPEED -> (level.effect() / 20.0) + "s";
            case STORAGE -> (int) level.effect() + " slots";
            case EFFICIENCY -> "x" + level.effect();
            case TILLING -> tillingName((int) level.effect());
        };
    }

    private static String tillingName(int level) {
        return switch (level) {
            case 1 -> "Auto-plow";
            case 2 -> "Auto-plow + hydrate";
            default -> "Tilling " + level;
        };
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
        UpgradeTrack t = type.upgradeTrack(track);
        int max = t == null ? 0 : t.maxLevel();
        int raw = event.getRawSlot();
        int idx = raw - 10;
        if (idx >= 0 && idx < max) {
            plugin.menus().open(viewer, new LevelMenu(plugin, typeId, track, idx + 1).withParent(this));
            return;
        }
        if (idx == max && raw <= 34) {
            File f = typeFile();
            if (f == null) {
                return;
            }
            try {
                String flow = defaultFlow(type);
                if (TypeConfigWriter.addUpgradeLevel(f, track.key(), flow) && reload(viewer)) {
                    viewer.sendMessage(Text.mm("<green>Level " + (max + 1) + " added — click it to tune.</green>"));
                }
            } catch (IOException e) {
                viewer.sendMessage(Text.mm("<red>Could not write the type file.</red>"));
            }
        }
    }

    private String defaultFlow(FarmType type) {
        UpgradeTrack t = type.upgradeTrack(track);
        double effect = t == null || t.maxLevel() == 0 ? defaultEffect()
                : t.level(t.maxLevel()).effect();
        return "effect: " + effect + ", cost-money: 0.0, description: \"Level " + (t == null ? 1 : t.maxLevel() + 1) + "\"";
    }

    private double defaultEffect() {
        return switch (track) {
            case RADIUS -> 4;
            case SPEED -> 100;
            case STORAGE -> 27;
            case EFFICIENCY -> 1.0;
            case TILLING -> 1;
        };
    }
}
