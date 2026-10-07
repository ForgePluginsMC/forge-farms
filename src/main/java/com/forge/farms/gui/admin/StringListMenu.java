package com.forge.farms.gui.admin;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.TypeConfigWriter;
import com.forge.farms.core.Text;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Generic line-list editor: shows each line as a button (click to remove),
 * plus an add button. Used for descriptions and hologram lines.
 */
public final class StringListMenu extends TypeMenu {
    private final String path;
    private final String title;
    private final String addPrompt;
    private final List<String> lines;

    public StringListMenu(ForgeFarms plugin, String typeId, String path, String title,
            String addPrompt, List<String> lines) {
        super(plugin, typeId);
        this.path = path;
        this.title = title;
        this.addPrompt = addPrompt;
        this.lines = new ArrayList<>(lines);
    }

    @Override
    public void build(Player viewer) {
        int size = Math.max(36, Math.min(54, ((lines.size() + 1 + 8) / 9) * 9 + 9));
        create(viewer, size, "<dark_red><bold>" + title + "</bold></dark_red>");
        fill(filler());
        int max = size - 9;
        for (int i = 0; i < lines.size() && i < max - 1; i++) {
            List<String> lore = new ArrayList<>();
            lore.add("<white>" + lines.get(i) + "</white>");
            lore.add("");
            lore.add("<red>Click to remove this line.</red>");
            inventory.setItem(i, button(Material.PAPER, "<gold>Line " + (i + 1) + "</gold>", lore));
        }
        if (lines.size() < max - 1) {
            inventory.setItem(lines.size(), button(Material.EMERALD, "<green>Add line</green>",
                    "<yellow>Click to type a new line.</yellow>"));
        }
        navRow();
    }

    @Override
    public void click(Player viewer, InventoryClickEvent event) {
        if (!isTopClick(event) || navClick(viewer, event)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= lines.size() + 1) {
            return;
        }
        if (slot == lines.size()) {
            plugin.prompts().ask(viewer, addPrompt, input -> {
                List<String> next = new ArrayList<>(lines);
                next.add(input);
                if (write(viewer, next)) {
                    plugin.menus().open(viewer,
                            new StringListMenu(plugin, typeId, path, title, addPrompt, next)
                                    .withParent(parentMenu()));
                }
            }, () -> plugin.menus().open(viewer, this));
            return;
        }
        List<String> next = new ArrayList<>(lines);
        next.remove(slot);
        if (write(viewer, next)) {
            lines.clear();
            lines.addAll(next);
            plugin.menus().refresh(viewer);
        }
    }

    private com.forge.farms.gui.Menu parentMenu() {
        return parent();
    }

    private boolean write(Player viewer, List<String> next) {
        File f = typeFile();
        if (f == null) {
            return false;
        }
        try {
            if (!TypeConfigWriter.setStringList(f, path, next)) {
                viewer.sendMessage(Text.mm("<red>Key not found in the type file: " + path + "</red>"));
                return false;
            }
        } catch (IOException e) {
            viewer.sendMessage(Text.mm("<red>Could not write the type file.</red>"));
            return false;
        }
        return reload(viewer);
    }
}
