package com.forge.farms.gui.admin;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.FarmType;
import com.forge.farms.config.TypeConfigWriter;
import com.forge.farms.core.Text;
import com.forge.farms.gui.Menu;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/** Base for the admin farm-type editor menus. */
public abstract class TypeMenu extends Menu {
    protected final String typeId;

    protected TypeMenu(ForgeFarms plugin, String typeId) {
        super(plugin);
        this.typeId = typeId;
    }

    protected @Nullable FarmType type() {
        return plugin.config().getType(typeId);
    }

    protected @Nullable File typeFile() {
        return plugin.config().typeFile(typeId);
    }

    /**
     * Write one scalar to the type file, reload the type, refresh existing
     * farms' derived stats, and rebuild the menu. Returns false on failure.
     */
    protected boolean saveScalar(Player viewer, String path, String value) {
        File f = typeFile();
        if (f == null) {
            viewer.sendMessage(Text.mm("<red>No file for this farm type.</red>"));
            return false;
        }
        try {
            if (!TypeConfigWriter.setScalar(f, path, value)) {
                viewer.sendMessage(Text.mm("<red>Key not found in the type file: <white>" + path + "</white></red>"));
                return false;
            }
        } catch (IOException e) {
            viewer.sendMessage(Text.mm("<red>Could not write the type file.</red>"));
            return false;
        }
        return reload(viewer);
    }

    /** Reload the type after a (non-scalar) write and refresh the menu. */
    protected boolean reload(Player viewer) {
        if (!plugin.config().reloadType(typeId)) {
            viewer.sendMessage(Text.mm("<red>Type failed to reload — check the file syntax.</red>"));
            return false;
        }
        plugin.farms().refreshTypeStats(typeId);
        plugin.menus().refresh(viewer);
        return true;
    }

    /** Standard stepper lore. */
    protected List<String> stepperLore(String hint, String current) {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>" + hint + "</gray>");
        lore.add("");
        lore.add("<gray>Current: <white>" + current + "</white></gray>");
        lore.add("");
        lore.add("<yellow>Left-click: +1 · Right-click: -1</yellow>");
        lore.add("<yellow>Shift-click: ±10</yellow>");
        return lore;
    }

    protected int delta(org.bukkit.event.inventory.InventoryClickEvent event) {
        int step = event.isShiftClick() ? 10 : 1;
        return event.isRightClick() ? -step : step;
    }

    protected int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    protected void sectionButton(int slot, Material icon, String name, String... lore) {
        inventory.setItem(slot, button(icon, name, lore));
    }
}
