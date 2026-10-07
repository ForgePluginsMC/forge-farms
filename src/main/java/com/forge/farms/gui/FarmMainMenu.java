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
    private boolean deleteArmed;

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
            for (String line : type.descriptionLines()) {
                info.add("<gray>" + line + "</gray>");
            }
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
                        + farm.efficiencyLevel() + "</white> | Tilling <white>"
                        + farm.tillingLevel() + "</white></gray>",
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

        boolean owner = viewer.getUniqueId().equals(farm.owner());
        if (owner) {
            List<String> delLore = new ArrayList<>();
            delLore.add(deleteArmed
                    ? "<red><bold>Click again to confirm deletion.</bold></red>"
                    : "<gray>Permanently remove this farm.</gray>");
            delLore.add("<gray>Stored items drop at your feet.</gray>");
            delLore.add("");
            delLore.add(deleteArmed ? "<red>Confirm delete.</red>" : "<red>Click to delete.</red>");
            inventory.setItem(17, button(deleteArmed ? Material.TNT : Material.BARRIER,
                    "<red>Delete farm</red>", delLore));
        }
        navRow();
    }

    @Override
    public void click(Player viewer, InventoryClickEvent event) {
        if (!isTopClick(event) || navClick(viewer, event)) {
            return;
        }
        switch (event.getRawSlot()) {
            case 10 -> plugin.menus().open(viewer, new FarmStorageMenu(plugin, farm).withParent(this));
            case 11 -> plugin.menus().open(viewer, new FarmFuelMenu(plugin, farm).withParent(this));
            case 12 -> {
                if (plugin.trust().can(viewer, farm, com.forge.farms.members.FarmFlag.UPGRADE)) {
                    plugin.menus().open(viewer, new FarmUpgradesMenu(plugin, farm).withParent(this));
                } else {
                    viewer.sendMessage(Text.mm("<red>You can't buy upgrades on this farm.</red>"));
                }
            }
            case 13 -> plugin.menus().open(viewer, new FarmMembersMenu(plugin, farm).withParent(this));
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
                if (!viewer.getUniqueId().equals(farm.owner())) {
                    return;
                }
                if (!deleteArmed) {
                    deleteArmed = true;
                    plugin.menus().refresh(viewer);
                    return;
                }
                dropStorage(viewer);
                if (plugin.farms().removeFarm(farm.id())) {
                    viewer.sendMessage(Text.mm("<yellow>Farm deleted.</yellow>"));
                }
                viewer.closeInventory();
            }
            default -> {
            }
        }
    }

    /** Drop stored items at the owner's feet before deletion. */
    private void dropStorage(Player viewer) {
        org.bukkit.Location loc = viewer.getLocation();
        for (int i = 0; i < farm.storageSlots(); i++) {
            org.bukkit.inventory.ItemStack item = farm.getSlot(i);
            if (item != null) {
                loc.getWorld().dropItemNaturally(loc, item);
            }
        }
    }
}
