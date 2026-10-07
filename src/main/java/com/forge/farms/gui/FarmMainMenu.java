package com.forge.farms.gui;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.FarmType;
import com.forge.farms.core.Text;
import com.forge.farms.farm.Farm;
import com.forge.farms.output.OutputMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Hub menu: info + shortcuts to storage, fuel, upgrades, members, toggles. */
public final class FarmMainMenu extends Menu {
    private final Farm farm;

    public FarmMainMenu(ForgeFarms plugin, Farm farm) {
        super(plugin);
        this.farm = farm;
    }

    @Override
    public void build(Player viewer) {
        FarmType type = plugin.config().getType(farm.typeId());
        String title = type == null ? "Farm" : Text.plain(type.displayName());
        create(viewer, 27, "<dark_green><bold>" + title + "</bold></dark_green>");
        fill(filler());

        if (type != null) {
            List<String> info = new ArrayList<>();
            info.add("<gray>" + type.description() + "</gray>");
            info.add("");
            info.add("<gray>Radius: <white>" + type.radiusAt(farm.radiusLevel()) + "</white></gray>");
            info.add("<gray>Tick: <white>" + (type.intervalAt(farm.speedLevel()) / 20.0) + "s</white></gray>");
            info.add("<gray>Fuel: <white>" + plugin.fuel().formatDuration(farm.fuelTicks()) + "</white></gray>");
            info.add("<gray>Storage: <white>" + farm.usedSlots() + "/" + farm.storageSlots()
                    + "</white> (" + farm.totalItemCount() + " items)</gray>");
            info.add("<gray>Harvested all-time: <white>" + farm.totalHarvested() + "</white></gray>");
            inventory.setItem(4, button(type.itemMaterial(), type.displayName(), info));
        }

        inventory.setItem(10, button(Material.CHEST, "<gold>Storage</gold>",
                "<gray>View and withdraw harvested items.</gray>", "<yellow>Click to open.</yellow>"));
        inventory.setItem(11, button(Material.COAL, "<gold>Fuel</gold>",
                "<gray>Remaining: <white>" + plugin.fuel().formatDuration(farm.fuelTicks()) + "</white></gray>",
                "<gray>Hold fuel and right-click the farm core to add more.</gray>",
                "<yellow>Click to open.</yellow>"));
        inventory.setItem(12, button(Material.ANVIL, "<gold>Upgrades</gold>",
                "<gray>Radius <white>" + farm.radiusLevel() + "</white> | Speed <white>" + farm.speedLevel()
                        + "</white></gray>",
                "<gray>Storage <white>" + farm.storageLevel() + "</white> | Efficiency <white>"
                        + farm.efficiencyLevel() + "</white></gray>",
                "<yellow>Click to open.</yellow>"));
        inventory.setItem(13, button(Material.PLAYER_HEAD, "<gold>Members</gold>",
                "<gray>Manage trusted players and roles.</gray>", "<yellow>Click to open.</yellow>"));
        List<String> outputLore = farm.outputPriority().stream()
                .map(m -> "<gray>- <white>" + m.name() + "</white></gray>")
                .collect(Collectors.toCollection(ArrayList::new));
        outputLore.add("");
        outputLore.add("<yellow>Click to cycle priority.</yellow>");
        inventory.setItem(14, button(Material.HOPPER, "<gold>Output order</gold>", outputLore));

        inventory.setItem(15, button(farm.hologramEnabled() ? Material.SEA_LANTERN : Material.GLASS,
                "<gold>Hologram</gold>",
                "<gray>Currently: <white>" + (farm.hologramEnabled() ? "ON" : "OFF") + "</white></gray>",
                "<yellow>Click to toggle.</yellow>"));
        inventory.setItem(16, button(farm.autoSell() ? Material.GOLD_INGOT : Material.IRON_INGOT,
                "<gold>Auto-sell</gold>",
                "<gray>Currently: <white>" + (farm.autoSell() ? "ON" : "OFF") + "</white></gray>",
                "<yellow>Click to toggle.</yellow>"));
        inventory.setItem(17, button(Material.COMPARATOR, "<gold>Tuning</gold>",
                "<gray>Growth, harvest, and effect knobs</gray>",
                "<gray>for this farm. Overrides the type defaults.</gray>",
                "<yellow>Click to open.</yellow>"));
    }

    @Override
    public void click(Player viewer, InventoryClickEvent event) {
        if (!isTopClick(event)) {
            return;
        }
        switch (event.getRawSlot()) {
            case 10 -> plugin.menus().open(viewer, new FarmStorageMenu(plugin, farm));
            case 11 -> plugin.menus().open(viewer, new FarmFuelMenu(plugin, farm));
            case 12 -> {
                if (plugin.trust().can(viewer, farm, com.forge.farms.members.FarmFlag.UPGRADE)) {
                    plugin.menus().open(viewer, new FarmUpgradesMenu(plugin, farm));
                } else {
                    viewer.sendMessage(Text.mm("<red>You can't buy upgrades on this farm.</red>"));
                }
            }
            case 13 -> plugin.menus().open(viewer, new FarmMembersMenu(plugin, farm));
            case 14 -> {
                List<OutputMode> priority = farm.outputPriority();
                if (!priority.isEmpty()) {
                    OutputMode first = priority.remove(0);
                    priority.add(first);
                    farm.setOutputPriority(priority);
                    plugin.farms().saveFarm(farm);
                    viewer.sendMessage(Text.mm("<green>Output priority: <white>"
                            + priority.stream().map(Enum::name).collect(Collectors.joining(" > "))
                            + "</white></green>"));
                    plugin.menus().refresh(viewer);
                }
            }
            case 15 -> {
                farm.setHologramEnabled(!farm.hologramEnabled());
                plugin.farms().saveFarm(farm);
                plugin.holograms().refresh(farm);
                plugin.menus().refresh(viewer);
            }
            case 16 -> {
                farm.setAutoSell(!farm.autoSell());
                plugin.farms().saveFarm(farm);
                plugin.menus().refresh(viewer);
            }
            case 17 -> {
                if (plugin.trust().can(viewer, farm, com.forge.farms.members.FarmFlag.CONFIGURE)) {
                    plugin.menus().open(viewer, new FarmTuneMenu(plugin, farm));
                } else {
                    viewer.sendMessage(Text.mm("<red>You can't tune this farm.</red>"));
                }
            }
            default -> {
            }
        }
    }
}
