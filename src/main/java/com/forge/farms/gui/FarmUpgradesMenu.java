package com.forge.farms.gui;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.FarmType;
import com.forge.farms.config.TrackType;
import com.forge.farms.config.UpgradeLevel;
import com.forge.farms.farm.Farm;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Four independent upgrade tracks. Click a track to buy its next level. */
public final class FarmUpgradesMenu extends Menu {
    private final Farm farm;

    public FarmUpgradesMenu(ForgeFarms plugin, Farm farm) {
        super(plugin);
        this.farm = farm;
    }

    @Override
    public void build(Player viewer) {
        FarmType type = plugin.config().getType(farm.typeId());
        create(viewer, 27, "<dark_green><bold>Farm Upgrades</bold></dark_green>");
        fill(filler());
        if (type == null) {
            return;
        }
        inventory.setItem(10, trackButton(type, TrackType.RADIUS, Material.COMPASS));
        inventory.setItem(12, trackButton(type, TrackType.SPEED, Material.CLOCK));
        inventory.setItem(14, trackButton(type, TrackType.STORAGE, Material.CHEST));
        inventory.setItem(16, trackButton(type, TrackType.EFFICIENCY, Material.COAL_BLOCK));
    }

    private org.bukkit.inventory.ItemStack trackButton(FarmType type, TrackType track, Material icon) {
        int current = farm.getLevel(track);
        int max = plugin.upgrades().maxLevel(type, track);
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Level: <white>" + current + "/" + max + "</white></gray>");
        lore.add("<gray>Now: <white>" + plugin.upgrades().describeEffect(type, track, current) + "</white></gray>");
        UpgradeLevel next = plugin.upgrades().nextLevel(farm, type, track);
        if (next == null) {
            lore.add("<yellow>MAXED</yellow>");
        } else {
            lore.add("<gray>Next: <white>" + plugin.upgrades().describeEffect(type, track, next.level())
                    + "</white></gray>");
            lore.add("<gray>Cost: " + costLine(next) + "</gray>");
            lore.add("");
            lore.add("<yellow>Click to buy level " + next.level() + ".</yellow>");
        }
        return button(icon, "<gold>" + plugin.upgrades().trackTitle(track) + "</gold>", lore);
    }

    private String costLine(UpgradeLevel next) {
        return next.cost().describe(plugin.output()::formatMoney);
    }

    @Override
    public void click(Player viewer, InventoryClickEvent event) {
        if (!isTopClick(event)) {
            return;
        }
        TrackType track = switch (event.getRawSlot()) {
            case 10 -> TrackType.RADIUS;
            case 12 -> TrackType.SPEED;
            case 14 -> TrackType.STORAGE;
            case 16 -> TrackType.EFFICIENCY;
            default -> null;
        };
        if (track == null) {
            return;
        }
        if (!plugin.trust().can(viewer, farm, com.forge.farms.members.FarmFlag.UPGRADE)) {
            viewer.sendMessage(com.forge.farms.core.Text.mm("<red>You can't buy upgrades on this farm.</red>"));
            return;
        }
        if (plugin.upgrades().purchase(viewer, farm, track)) {
            plugin.menus().refresh(viewer);
        }
    }
}
