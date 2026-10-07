package com.forge.farms.gui.admin;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.FarmType;
import com.forge.farms.config.TypeConfigWriter;
import com.forge.farms.core.Text;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Admin: fuel burn time and accepted fuel items. */
public final class FuelMenu extends TypeMenu {
    public FuelMenu(ForgeFarms plugin, String typeId) {
        super(plugin, typeId);
    }

    @Override
    public void build(Player viewer) {
        FarmType type = type();
        create(viewer, 54, "<dark_red><bold>Fuel</bold></dark_red> <gray>· " + typeId + "</gray>");
        fill(filler());
        if (type == null) {
            navRow();
            return;
        }
        inventory.setItem(10, button(Material.FURNACE, "<gold>Burn time</gold>",
                stepperLore("Ticks of farm fuel per item.", String.valueOf(type.fuelTicksPerItem()))));

        List<Material> fuels = new ArrayList<>(new TreeSet<>(type.fuelItems()));
        int slot = 19;
        for (Material m : fuels) {
            if (slot > 34) {
                break;
            }
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Accepted as farm fuel.</gray>");
            lore.add("");
            lore.add("<red>Click to remove.</red>");
            inventory.setItem(slot++, button(itemIcon(m), "<gold>" + pretty(m) + "</gold>", lore));
        }
        if (slot <= 34) {
            inventory.setItem(slot, button(Material.EMERALD, "<green>Add fuel item</green>",
                    "<gray>Hold the item, click, type anything.</gray>",
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
        int raw = event.getRawSlot();
        if (raw == 10) {
            long next = clamp((int) (type.fuelTicksPerItem() + delta(event) * 100L), 100, 20000);
            saveScalar(viewer, "fuel.ticks-per-item", String.valueOf(next));
            return;
        }
        List<Material> fuels = new ArrayList<>(new TreeSet<>(type.fuelItems()));
        int idx = raw - 19;
        if (idx >= 0 && idx < fuels.size()) {
            Material removed = fuels.get(idx);
            fuels.remove(idx);
            if (writeFuels(viewer, fuels)) {
                viewer.sendMessage(Text.mm("<yellow>Removed <white>" + pretty(removed)
                        + "</white> from fuel items.</yellow>"));
            }
            return;
        }
        if (idx == fuels.size() && raw <= 34) {
            plugin.prompts().askHeldItem(viewer, "Hold the fuel item:", mat -> {
                if (fuels.contains(mat)) {
                    viewer.sendMessage(Text.mm("<yellow>Already a fuel item.</yellow>"));
                    plugin.menus().open(viewer, this);
                    return;
                }
                fuels.add(mat);
                if (writeFuels(viewer, fuels)) {
                    viewer.sendMessage(Text.mm("<green>Added <white>" + pretty(mat) + "</white> as fuel.</green>"));
                }
                plugin.menus().open(viewer, new FuelMenu(plugin, typeId).withParent(parent()));
            }, () -> plugin.menus().open(viewer, this));
        }
    }

    private boolean writeFuels(Player viewer, List<Material> fuels) {
        File f = typeFile();
        if (f == null) {
            return false;
        }
        List<String> names = new ArrayList<>();
        for (Material m : new TreeSet<>(fuels)) {
            names.add(m.name());
        }
        try {
            if (!TypeConfigWriter.setFlowList(f, "fuel.items", names)) {
                viewer.sendMessage(Text.mm("<red>fuel.items not found in the type file.</red>"));
                return false;
            }
        } catch (IOException e) {
            viewer.sendMessage(Text.mm("<red>Could not write the type file.</red>"));
            return false;
        }
        return reload(viewer);
    }

    private static Material itemIcon(Material m) {
        return m.isItem() ? m : Material.BUCKET;
    }

    private static String pretty(Material m) {
        String s = m.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
