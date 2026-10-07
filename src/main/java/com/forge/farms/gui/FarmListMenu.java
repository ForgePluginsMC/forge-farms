package com.forge.farms.gui;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.FarmType;
import com.forge.farms.core.Text;
import com.forge.farms.farm.Farm;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** /farm list as a GUI: left-click opens the farm, right-click teleports. */
public final class FarmListMenu extends Menu {
    private final List<Farm> farms;

    public FarmListMenu(ForgeFarms plugin, Player viewer) {
        super(plugin);
        farms = plugin.farms().getByOwner(viewer.getUniqueId());
    }

    @Override
    public void build(Player viewer) {
        int size = Math.max(27, Math.min(54, ((farms.size() + 8) / 9) * 9 + 9));
        create(viewer, size, "<dark_green><bold>Your Farms</bold></dark_green>");
        fill(filler());
        int max = size - 9;
        for (int i = 0; i < farms.size() && i < max; i++) {
            Farm farm = farms.get(i);
            FarmType type = plugin.config().getType(farm.typeId());
            List<String> lore = new ArrayList<>();
            lore.add("<gray>At <white>" + farm.x() + ", " + farm.y() + ", " + farm.z() + "</white></gray>");
            lore.add("<gray>Fuel: <white>" + plugin.fuel().formatDuration(farm.fuelTicks()) + "</white></gray>");
            lore.add("<gray>Storage: <white>" + farm.usedSlots() + "/" + farm.storageSlots() + "</white></gray>");
            lore.add("");
            lore.add("<yellow>Left-click: manage.</yellow>");
            lore.add("<yellow>Right-click: teleport.</yellow>");
            String name = type == null ? farm.typeId() : type.displayName();
            Material icon = type == null ? Material.COMPOSTER : type.itemMaterial();
            inventory.setItem(i, button(icon, name, lore));
        }
        navRow();
    }

    @Override
    public void click(Player viewer, InventoryClickEvent event) {
        if (!isTopClick(event) || navClick(viewer, event)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= farms.size()) {
            return;
        }
        Farm farm = farms.get(slot);
        if (event.isRightClick()) {
            org.bukkit.Location loc = plugin.farms().coreLocation(farm);
            if (loc != null) {
                viewer.teleport(loc.clone().add(0.5, 1, 0.5));
                viewer.sendMessage(Text.mm("<green>Teleported to your farm.</green>"));
            }
            return;
        }
        plugin.menus().open(viewer, new FarmMainMenu(plugin, farm).withParent(this));
    }
}
