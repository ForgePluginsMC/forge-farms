package com.forge.farms.gui;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.FarmType;
import com.forge.farms.core.Text;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** /farm shop: buy farm items for money, items, or XP levels. */
public final class FarmShopMenu extends Menu {
    private final List<FarmType> types;

    public FarmShopMenu(ForgeFarms plugin) {
        super(plugin);
        types = plugin.config().getTypes().stream()
                .filter(FarmType::shopEnabled)
                .sorted(Comparator.comparing(FarmType::shopCategory)
                        .thenComparing(t -> Text.plain(t.displayName())))
                .toList();
    }

    @Override
    public void build(Player viewer) {
        int size = Math.max(27, Math.min(54, ((types.size() + 8) / 9) * 9));
        create(viewer, size, "<dark_green><bold>Farm Shop</bold></dark_green>");
        fill(filler());
        String lastCategory = "";
        for (int i = 0; i < types.size() && i < size - 9; i++) {
            FarmType type = types.get(i);
            List<String> lore = new ArrayList<>();
            if (!type.shopCategory().equals(lastCategory)) {
                lastCategory = type.shopCategory();
                lore.add("<aqua><bold>" + lastCategory + "</bold></aqua>");
            }
            for (String line : type.descriptionLines()) {
                lore.add("<gray>" + line + "</gray>");
            }
            lore.add("");
            lore.add("<gray>Price: " + type.shopCost().describe(plugin.output()::formatMoney) + "</gray>");
            lore.add("");
            lore.add("<yellow>Click to buy.</yellow>");
            inventory.setItem(i, button(type.itemMaterial(), type.displayName(), lore));
        }
        navRow();
    }

    @Override
    public void click(Player viewer, InventoryClickEvent event) {
        if (!isTopClick(event) || navClick(viewer, event)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= types.size()) {
            return;
        }
        FarmType type = types.get(slot);
        if (plugin.shop().buy(viewer, type)) {
            plugin.menus().refresh(viewer);
        }
    }
}
