package com.forge.farms.gui.admin;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.FarmType;
import com.forge.farms.core.Text;
import com.forge.farms.gui.ChatPrompt;
import com.forge.farms.gui.Menu;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Admin: pick a farm type to tune, open server settings, or clone a type.
 * Reached via {@code /farmadmin tune}.
 */
public final class TypeListMenu extends Menu {
    private final List<FarmType> types;

    public TypeListMenu(ForgeFarms plugin) {
        super(plugin);
        types = plugin.config().getTypes().stream()
                .sorted(Comparator.comparing(FarmType::shopCategory)
                        .thenComparing(t -> Text.plain(t.displayName())))
                .toList();
    }

    @Override
    public void build(Player viewer) {
        int size = Math.max(36, ((types.size() + 8) / 9) * 9 + 9);
        size = Math.min(54, size);
        create(viewer, size, "<dark_red><bold>Farm Types</bold></dark_red> <gray>· admin</gray>");
        fill(filler());
        int maxTypeSlots = size - 9;
        for (int i = 0; i < types.size() && i < maxTypeSlots; i++) {
            FarmType type = types.get(i);
            List<String> lore = new ArrayList<>();
            lore.add("<aqua>" + type.shopCategory() + "</aqua>");
            for (String line : type.descriptionLines()) {
                lore.add("<gray>" + line + "</gray>");
            }
            lore.add("");
            lore.add("<yellow>Left-click: edit this type.</yellow>");
            lore.add("<yellow>Right-click: clone this type.</yellow>");
            inventory.setItem(i, button(type.itemMaterial(), type.displayName(), lore));
        }
        inventory.setItem(size - 7, button(Material.COMPARATOR, "<gold>Server settings</gold>",
                "<gray>Limits and sell prices.</gray>",
                "<yellow>Click to open.</yellow>"));
        inventory.setItem(size - 6, button(Material.NAME_TAG, "<gold>Clone a type</gold>",
                "<gray>Right-click a type above to clone it,</gray>",
                "<gray>or click here and pick after.</gray>",
                "<yellow>Click to clone…</yellow>"));
        navRow();
    }

    @Override
    public void click(Player viewer, InventoryClickEvent event) {
        if (!isTopClick(event) || navClick(viewer, event)) {
            return;
        }
        int size = inventory.getSize();
        int slot = event.getRawSlot();
        if (slot == size - 7) {
            plugin.menus().open(viewer, new ServerSettingsMenu(plugin).withParent(this));
            return;
        }
        if (slot == size - 6) {
            viewer.sendMessage(Text.mm("<yellow>Right-click the type you want to clone.</yellow>"));
            return;
        }
        if (slot < 0 || slot >= types.size()) {
            return;
        }
        FarmType type = types.get(slot);
        if (event.isRightClick()) {
            cloneType(viewer, type);
        } else {
            plugin.menus().open(viewer, new TypeEditorMenu(plugin, type.id()).withParent(this));
        }
    }

    private void cloneType(Player viewer, FarmType type) {
        ChatPrompt prompt = plugin.prompts();
        prompt.ask(viewer, "New type id (letters, numbers, underscore):", input -> {
            String id = input.toLowerCase(java.util.Locale.ROOT).trim();
            if (!id.matches("[a-z0-9_]+")) {
                viewer.sendMessage(Text.mm("<red>Invalid id. Use letters, numbers, underscore.</red>"));
                plugin.menus().open(viewer, this);
                return;
            }
            if (plugin.config().getType(id) != null) {
                viewer.sendMessage(Text.mm("<red>A type with that id already exists.</red>"));
                plugin.menus().open(viewer, this);
                return;
            }
            File src = plugin.config().typeFile(type.id());
            File dest = new File(src.getParentFile(), id + ".yml");
            try {
                List<String> lines = new ArrayList<>(Files.readAllLines(src.toPath(), StandardCharsets.UTF_8));
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).strip().startsWith("id:")) {
                        int colon = lines.get(i).indexOf(':');
                        lines.set(i, lines.get(i).substring(0, colon + 1) + " " + id);
                        break;
                    }
                }
                Files.write(dest.toPath(), lines, StandardCharsets.UTF_8);
            } catch (IOException e) {
                viewer.sendMessage(Text.mm("<red>Could not copy the type file.</red>"));
                plugin.menus().open(viewer, this);
                return;
            }
            plugin.config().reload();
            viewer.sendMessage(Text.mm("<green>Cloned <white>" + Text.plain(type.displayName())
                    + "</white> to <white>" + id + "</white>.</green>"));
            plugin.menus().open(viewer, new TypeListMenu(plugin));
        }, () -> plugin.menus().open(viewer, this));
    }
}
