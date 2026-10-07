package com.forge.farms.gui;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.FarmType;
import com.forge.farms.config.TrackType;
import com.forge.farms.config.UpgradeLevel;
import com.forge.farms.core.Text;
import com.forge.farms.farm.Farm;
import com.forge.farms.members.FarmFlag;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/** Five independent upgrade tracks. Click a track to buy its next level. */
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
            navRow();
            return;
        }
        inventory.setItem(9, trackButton(type, TrackType.RADIUS, Material.COMPASS));
        inventory.setItem(11, trackButton(type, TrackType.SPEED, Material.CLOCK));
        inventory.setItem(13, trackButton(type, TrackType.STORAGE, Material.CHEST));
        inventory.setItem(15, trackButton(type, TrackType.EFFICIENCY, Material.COAL_BLOCK));
        inventory.setItem(17, trackButton(type, TrackType.TILLING, Material.IRON_HOE));
        navRow();
    }

    private ItemStack trackButton(FarmType type, TrackType track, Material icon) {
        int current = farm.getLevel(track);
        int max = plugin.upgrades().maxLevel(type, track);
        List<String> lore = new ArrayList<>();
        if (max == 0) {
            lore.add("<gray>Not available for this farm type.</gray>");
            return button(icon, "<gold>" + plugin.upgrades().trackTitle(track) + "</gold>", lore);
        }
        lore.add("<gray>Level: <white>" + current + "/" + max + "</white></gray>");
        lore.add("<gray>Now: <white>" + plugin.upgrades().describeEffect(type, track, current) + "</white></gray>");
        UpgradeLevel next = plugin.upgrades().nextLevel(farm, type, track);
        if (next == null) {
            lore.add("<yellow>MAXED</yellow>");
        } else {
            lore.add("<gray>Next: <white>" + plugin.upgrades().describeEffect(type, track, next.level())
                    + "</white></gray>");
            lore.add("<gray>Cost: " + next.cost().describe(plugin.output()::formatMoney) + "</gray>");
            lore.add("");
            lore.add("<yellow>Click to buy level " + next.level() + ".</yellow>");
        }
        return button(icon, "<gold>" + plugin.upgrades().trackTitle(track) + "</gold>", lore);
    }

    @Override
    public void click(Player viewer, InventoryClickEvent event) {
        if (!isTopClick(event) || navClick(viewer, event)) {
            return;
        }
        TrackType track = switch (event.getRawSlot()) {
            case 9 -> TrackType.RADIUS;
            case 11 -> TrackType.SPEED;
            case 13 -> TrackType.STORAGE;
            case 15 -> TrackType.EFFICIENCY;
            case 17 -> TrackType.TILLING;
            default -> null;
        };
        if (track == null) {
            return;
        }
        if (!plugin.trust().can(viewer, farm, FarmFlag.UPGRADE)) {
            viewer.sendMessage(Text.mm("<red>You can't buy upgrades on this farm.</red>"));
            return;
        }
        if (plugin.upgrades().purchase(viewer, farm, track)) {
            plugin.menus().refresh(viewer);
        }
    }
}
