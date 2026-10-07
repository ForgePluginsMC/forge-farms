package com.forge.farms.gui.admin;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.TypeConfigWriter;
import com.forge.farms.core.Text;
import com.forge.farms.gui.Menu;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Admin: server-wide settings — limits, hologram refresh, prices, database. */
public final class ServerSettingsMenu extends Menu {
    public ServerSettingsMenu(ForgeFarms plugin) {
        super(plugin);
    }

    @Override
    public void build(Player viewer) {
        create(viewer, 36, "<dark_red><bold>Server Settings</bold></dark_red>");
        fill(filler());

        inventory.setItem(10, button(Material.PLAYER_HEAD, "<gold>Farm limit</gold>",
                settingLore("Farms each player may own.", String.valueOf(plugin.config().maxFarmsPerPlayer()))));
        inventory.setItem(11, button(Material.PLAYER_HEAD, "<gold>Member limit</gold>",
                settingLore("Members per farm.", String.valueOf(plugin.config().maxMembersPerFarm()))));
        inventory.setItem(12, button(Material.CLOCK, "<gold>Hologram refresh</gold>",
                settingLore("Ticks between hologram updates.",
                        plugin.config().hologramRefreshTicks() + " ticks")));

        List<String> priceLore = new ArrayList<>();
        priceLore.add("<gray>Auto-sell prices per item.</gray>");
        priceLore.add("");
        priceLore.add("<yellow>Click to edit.</yellow>");
        inventory.setItem(14, button(Material.GOLD_INGOT, "<gold>Sell prices</gold>", priceLore));

        List<String> dbLore = new ArrayList<>();
        dbLore.add("<gray>Backend: <white>" + plugin.database().backendName() + "</white></gray>");
        dbLore.add("<gray>Farms: <white>" + plugin.farms().all().size() + "</white></gray>");
        dbLore.add("");
        dbLore.add("<yellow>Click to back up now.</yellow>");
        inventory.setItem(16, button(Material.CHEST, "<gold>Database backup</gold>", dbLore));
        navRow();
    }

    private List<String> settingLore(String hint, String current) {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>" + hint + "</gray>");
        lore.add("");
        lore.add("<gray>Current: <white>" + current + "</white></gray>");
        lore.add("");
        lore.add("<yellow>Left-click: +1 · Right-click: -1</yellow>");
        lore.add("<yellow>Shift-click: ±10</yellow>");
        return lore;
    }

    @Override
    public void click(Player viewer, InventoryClickEvent event) {
        if (!isTopClick(event) || navClick(viewer, event)) {
            return;
        }
        int d = event.isShiftClick() ? 10 : 1;
        if (event.isRightClick()) {
            d = -d;
        }
        switch (event.getRawSlot()) {
            case 10 -> save(viewer, "limits.max-farms-per-player",
                    String.valueOf(Math.max(1, plugin.config().maxFarmsPerPlayer() + d)));
            case 11 -> save(viewer, "limits.max-members-per-farm",
                    String.valueOf(Math.max(1, plugin.config().maxMembersPerFarm() + d)));
            case 12 -> save(viewer, "hologram.refresh-ticks",
                    String.valueOf(Math.max(20, plugin.config().hologramRefreshTicks() + d * 20)));
            case 14 -> plugin.menus().open(viewer, new PricesMenu(plugin).withParent(this));
            case 16 -> backup(viewer);
            default -> {
            }
        }
    }

    private void save(Player viewer, String path, String value) {
        File config = new File(plugin.getDataFolder(), "config.yml");
        try {
            if (!TypeConfigWriter.setScalar(config, path, value)) {
                viewer.sendMessage(Text.mm("<red>Key not found in config.yml: " + path + "</red>"));
                return;
            }
        } catch (IOException e) {
            viewer.sendMessage(Text.mm("<red>Could not write config.yml.</red>"));
            return;
        }
        plugin.config().reload();
        plugin.menus().refresh(viewer);
    }

    private void backup(Player viewer) {
        viewer.sendMessage(Text.mm("<gray>Backing up the database…</gray>"));
        plugin.scheduler().async(() -> {
            String ext = plugin.database().backendName().equals("mysql") ? "sql" : "db";
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            File dest = new File(plugin.getDataFolder(), "backups/forgefarms-" + stamp + "." + ext);
            try {
                plugin.database().backup(dest);
                org.bukkit.Location loc = viewer.getLocation();
                plugin.scheduler().region(loc, () -> {
                    viewer.sendMessage(Text.mm("<green>Backup written to <white>backups/"
                            + dest.getName() + "</white>.</green>"));
                    plugin.menus().refresh(viewer);
                });
            } catch (Exception e) {
                org.bukkit.Location loc = viewer.getLocation();
                plugin.scheduler().region(loc, () -> viewer.sendMessage(
                        Text.mm("<red>Backup failed: " + e.getMessage() + "</red>")));
            }
        });
    }
}
