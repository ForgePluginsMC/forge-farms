package com.forge.farms.gui;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.FarmType;
import com.forge.farms.farm.Farm;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Fuel status + accepted fuels. Fuel is added by right-clicking the core. */
public final class FarmFuelMenu extends Menu {
    private final Farm farm;

    public FarmFuelMenu(ForgeFarms plugin, Farm farm) {
        super(plugin);
        this.farm = farm;
    }

    @Override
    public void build(Player viewer) {
        FarmType type = plugin.config().getType(farm.typeId());
        create(viewer, 27, "<dark_green><bold>Farm Fuel</bold></dark_green>");
        fill(filler());
        if (type == null) {
            return;
        }
        double frac = plugin.fuel().fuelFraction(farm, type);
        int bars = 20;
        int filled = (int) Math.round(frac * bars);
        StringBuilder bar = new StringBuilder("<green>");
        for (int i = 0; i < filled; i++) {
            bar.append('█');
        }
        bar.append("<gray>");
        for (int i = filled; i < bars; i++) {
            bar.append('█');
        }
        inventory.setItem(4, button(Material.FURNACE, "<gold>Fuel tank</gold>",
                bar.toString(),
                "<gray>Remaining: <white>" + plugin.fuel().formatDuration(farm.fuelTicks()) + "</white></gray>",
                "<gray>Efficiency: <white>x"
                        + String.format("%.2f", type.efficiencyAt(farm.efficiencyLevel())) + "</white></gray>"));
        List<String> fuels = new ArrayList<>();
        fuels.add("<gray>Accepted fuel:</gray>");
        for (Material m : type.fuelItems()) {
            String[] parts = m.name().toLowerCase(java.util.Locale.ROOT).split("_");
            StringBuilder name = new StringBuilder();
            for (String p : parts) {
                if (!name.isEmpty()) {
                    name.append(' ');
                }
                name.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            }
            fuels.add("<gray>- <white>" + name + "</white> ("
                    + plugin.fuel().formatDuration(
                            (long) (type.fuelTicksPerItem() * type.efficiencyAt(farm.efficiencyLevel())))
                    + " each)</gray>");
        }
        fuels.add("");
        fuels.add("<yellow>Hold fuel and right-click the farm core to add it.</yellow>");
        inventory.setItem(22, button(Material.COAL, "<gold>How to refuel</gold>", fuels));
    }

    @Override
    public void click(Player viewer, InventoryClickEvent event) {
        // Display-only.
    }
}
