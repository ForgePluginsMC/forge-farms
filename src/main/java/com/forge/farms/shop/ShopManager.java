package com.forge.farms.shop;

import com.forge.farms.ForgeFarms;
import com.forge.farms.api.ForgeFarmsAPI;
import com.forge.farms.config.Cost;
import com.forge.farms.config.FarmType;
import com.forge.farms.core.Text;
import java.util.List;
import org.bukkit.entity.Player;

/** Handles farm-type purchases from the shop GUI. */
public final class ShopManager {
    private final ForgeFarms plugin;

    public ShopManager(ForgeFarms plugin) {
        this.plugin = plugin;
    }

    /** Attempt to buy one farm item of the given type. */
    public boolean buy(Player player, FarmType type) {
        if (!type.shopEnabled()) {
            return false;
        }
        Cost cost = type.shopCost();
        if (!cost.isFree()) {
            List<String> unmet = cost.unmet(player, plugin.output().economy());
            if (!unmet.isEmpty()) {
                player.sendMessage(Text.mm("<red>You need " + String.join(", ", unmet)
                        + " to buy this farm.</red>"));
                return false;
            }
            if (!cost.charge(player, plugin.output().economy())) {
                player.sendMessage(Text.mm("<red>Payment failed — purchase cancelled.</red>"));
                return false;
            }
        }
        if (!ForgeFarmsAPI.giveFarmItem(player, type.id(), 1)) {
            player.sendMessage(Text.mm("<red>Could not create that farm item.</red>"));
            return false;
        }
        player.sendMessage(Text.mm("<green>Bought " + Text.plain(type.displayName()) + "<green>!</green>"));
        return true;
    }
}
