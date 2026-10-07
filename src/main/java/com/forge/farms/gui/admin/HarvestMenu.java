package com.forge.farms.gui.admin;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.FarmType;
import com.forge.farms.config.Harvestable;
import com.forge.farms.config.TypeConfigWriter;
import com.forge.farms.core.Text;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Admin: which blocks this farm type grows and harvests. */
public final class HarvestMenu extends TypeMenu {
    public HarvestMenu(ForgeFarms plugin, String typeId) {
        super(plugin, typeId);
    }

    @Override
    public void build(Player viewer) {
        FarmType type = type();
        create(viewer, 54, "<dark_red><bold>Harvest</bold></dark_red> <gray>· " + typeId + "</gray>");
        fill(filler());
        if (type == null) {
            navRow();
            return;
        }
        List<Harvestable> list = type.harvestables();
        int slot = 10;
        for (Harvestable h : list) {
            if (slot > 34) {
                break;
            }
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Material: <white>" + h.material().name() + "</white></gray>");
            lore.add("<gray>Behavior: <white>" + h.behavior().name() + "</white></gray>");
            lore.add("");
            lore.add("<red>Click to remove.</red>");
            inventory.setItem(slot++, button(blockIcon(h.material()), "<gold>" + pretty(h.material()) + "</gold>", lore));
        }
        if (slot <= 34) {
            inventory.setItem(slot, button(Material.EMERALD, "<green>Add crop</green>",
                    "<gray>Type the block's material name.</gray>",
                    "<yellow>Click to add.</yellow>"));
        }
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
        List<Harvestable> list = type.harvestables();
        int raw = event.getRawSlot();
        int idx = raw - 10;
        if (idx >= 0 && idx < list.size()) {
            Harvestable h = list.get(idx);
            File f = typeFile();
            if (f == null) {
                return;
            }
            try {
                if (TypeConfigWriter.removeHarvestable(f, h.material().name()) && reload(viewer)) {
                    viewer.sendMessage(Text.mm("<yellow>Removed <white>" + pretty(h.material()) + "</white>.</yellow>"));
                }
            } catch (IOException e) {
                viewer.sendMessage(Text.mm("<red>Could not write the type file.</red>"));
            }
            return;
        }
        if (idx == list.size() && raw <= 34) {
            plugin.prompts().askMaterial(viewer, "Block material to grow (e.g. WHEAT):", mat -> {
                for (Harvestable h : list) {
                    if (h.material() == mat) {
                        viewer.sendMessage(Text.mm("<yellow>Already grown by this farm.</yellow>"));
                        plugin.menus().open(viewer, this);
                        return;
                    }
                }
                plugin.menus().open(viewer,
                        new HarvestBehaviorMenu(plugin, typeId, mat).withParent(this));
            }, () -> plugin.menus().open(viewer, this));
        }
    }

    /** Block material -> something renderable as a GUI icon. */
    static Material blockIcon(Material m) {
        return switch (m) {
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case BEETROOTS -> Material.BEETROOT;
            case SWEET_BERRY_BUSH -> Material.SWEET_BERRIES;
            case PITCHER_CROP -> Material.PITCHER_PLANT;
            case TORCHFLOWER_CROP -> Material.TORCHFLOWER;
            case MELON_STEM -> Material.MELON_SEEDS;
            case PUMPKIN_STEM -> Material.PUMPKIN_SEEDS;
            case COCOA -> Material.COCOA_BEANS;
            default -> m.isItem() ? m : Material.WHEAT;
        };
    }

    static String pretty(Material m) {
        String s = m.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
