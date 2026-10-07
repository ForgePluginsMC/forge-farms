package com.forge.farms.gui.admin;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.TypeConfigWriter;
import com.forge.farms.core.Text;
import com.forge.farms.gui.Menu;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Admin: auto-sell prices. Click a price to edit, right-click to remove. */
public final class PricesMenu extends Menu {
    public PricesMenu(ForgeFarms plugin) {
        super(plugin);
    }

    @Override
    public void build(Player viewer) {
        Map<String, Double> prices = new TreeMap<>(plugin.config().sellPrices());
        int size = Math.max(36, Math.min(54, ((prices.size() + 1 + 8) / 9) * 9 + 9));
        create(viewer, size, "<dark_red><bold>Sell Prices</bold></dark_red>");
        fill(filler());
        int max = size - 9;
        int slot = 0;
        for (Map.Entry<String, Double> e : prices.entrySet()) {
            if (slot >= max - 1) {
                break;
            }
            Material m = Material.matchMaterial(e.getKey());
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Price: <white>" + plugin.output().formatMoney(e.getValue()) + "</white></gray>");
            lore.add("");
            lore.add("<yellow>Left-click: edit (chat).</yellow>");
            lore.add("<red>Right-click: remove.</red>");
            inventory.setItem(slot++, button(m == null || !m.isItem() ? Material.PAPER : m,
                    "<gold>" + pretty(e.getKey()) + "</gold>", lore));
        }
        if (slot < max - 1) {
            inventory.setItem(slot, button(Material.EMERALD, "<green>Add price</green>",
                    "<gray>Hold the item, click, type the price.</gray>",
                    "<yellow>Click to add.</yellow>"));
        }
        navRow();
    }

    @Override
    public void click(Player viewer, InventoryClickEvent event) {
        if (!isTopClick(event) || navClick(viewer, event)) {
            return;
        }
        List<String> keys = new ArrayList<>(new TreeMap<>(plugin.config().sellPrices()).keySet());
        int raw = event.getRawSlot();
        int max = inventory.getSize() - 9;
        if (raw >= 0 && raw < keys.size() && raw < max - 1) {
            String key = keys.get(raw);
            Double current = plugin.config().sellPrice(Material.matchMaterial(key));
            if (event.isRightClick()) {
                if (removePrice(viewer, key)) {
                    viewer.sendMessage(Text.mm("<yellow>Removed sell price for <white>" + pretty(key) + "</white>.</yellow>"));
                }
                return;
            }
            plugin.prompts().askDouble(viewer,
                    "New price for " + pretty(key) + " (current " + current + "):",
                    price -> {
                        if (setPrice(viewer, key, price)) {
                            viewer.sendMessage(Text.mm("<green>Price set.</green>"));
                        }
                        plugin.menus().open(viewer, new PricesMenu(plugin).withParent(parent()));
                    }, () -> plugin.menus().open(viewer, this));
            return;
        }
        if (raw == keys.size() && raw < max - 1) {
            plugin.prompts().askHeldItem(viewer, "Hold the item to price:", mat -> {
                String key = mat.name();
                Double current = plugin.config().sellPrice(mat);
                plugin.prompts().askDouble(viewer,
                        "Price for " + pretty(key) + (current == null ? ":" : " (current " + current + "):"),
                        price -> {
                            if (setPrice(viewer, key, price)) {
                                viewer.sendMessage(Text.mm("<green>Price set.</green>"));
                            }
                            plugin.menus().open(viewer, new PricesMenu(plugin).withParent(parent()));
                        }, () -> plugin.menus().open(viewer, new PricesMenu(plugin)));
            }, () -> plugin.menus().open(viewer, this));
        }
    }

    private boolean setPrice(Player viewer, String key, double price) {
        File config = new File(plugin.getDataFolder(), "config.yml");
        try {
            if (!TypeConfigWriter.upsertPrice(config, key, price)) {
                return false;
            }
        } catch (IOException e) {
            viewer.sendMessage(Text.mm("<red>Could not write config.yml.</red>"));
            return false;
        }
        plugin.config().reload();
        plugin.menus().refresh(viewer);
        return true;
    }

    private boolean removePrice(Player viewer, String key) {
        File config = new File(plugin.getDataFolder(), "config.yml");
        try {
            if (!TypeConfigWriter.removePrice(config, key)) {
                return false;
            }
        } catch (IOException e) {
            viewer.sendMessage(Text.mm("<red>Could not write config.yml.</red>"));
            return false;
        }
        plugin.config().reload();
        plugin.menus().refresh(viewer);
        return true;
    }

    private static String pretty(String name) {
        String s = name.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
