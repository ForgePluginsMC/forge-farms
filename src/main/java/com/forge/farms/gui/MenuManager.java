package com.forge.farms.gui;

import com.forge.farms.ForgeFarms;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Tracks open ForgeFarms menus and routes clicks to them.
 * Clicks are always cancelled — menus are displays, not inventories.
 */
public final class MenuManager implements Listener {
    private final ForgeFarms plugin;
    private final Map<UUID, Menu> open = new ConcurrentHashMap<>();

    public MenuManager(ForgeFarms plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Menu menu) {
        menu.build(player);
        open.put(player.getUniqueId(), menu);
        player.openInventory(menu.inventory());
    }

    /** Rebuild the player's current menu in place. */
    public void refresh(Player player) {
        Menu menu = open.get(player.getUniqueId());
        if (menu != null) {
            menu.build(player);
            player.openInventory(menu.inventory());
        }
    }

    public void close(Player player) {
        open.remove(player.getUniqueId());
        player.closeInventory();
    }

    public boolean hasOpen(Player player) {
        return open.containsKey(player.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Menu menu = open.get(player.getUniqueId());
        if (menu == null) {
            return;
        }
        event.setCancelled(true);
        menu.click(player, event);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (open.containsKey(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        // Only unregister when the closed inventory is the tracked menu's.
        // Switching menus (open() while one is open) fires a close for the
        // OLD inventory — removing the player there would untrack the new menu
        // and leave its clicks uncancelled (GUI items become pickable).
        Menu menu = open.get(event.getPlayer().getUniqueId());
        if (menu != null && event.getInventory() == menu.inventory()) {
            open.remove(event.getPlayer().getUniqueId());
        }
    }
}
