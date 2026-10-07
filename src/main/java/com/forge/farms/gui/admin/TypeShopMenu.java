package com.forge.farms.gui.admin;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.Cost;
import com.forge.farms.config.FarmType;
import com.forge.farms.config.TypeConfigWriter;
import com.forge.farms.core.Text;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Admin: the /farm shop listing and price of one farm type. */
public final class TypeShopMenu extends TypeMenu {
    public TypeShopMenu(ForgeFarms plugin, String typeId) {
        super(plugin, typeId);
    }

    @Override
    public void build(Player viewer) {
        FarmType type = type();
        create(viewer, 36, "<dark_red><bold>Shop</bold></dark_red> <gray>· " + typeId + "</gray>");
        fill(filler());
        if (type == null) {
            navRow();
            return;
        }
        Cost cost = type.shopCost();

        List<String> enLore = new ArrayList<>();
        enLore.add("<gray>Show this farm in <white>/farm shop</white>.</gray>");
        enLore.add("");
        enLore.add("<gray>Currently: <white>" + (type.shopEnabled() ? "VISIBLE" : "HIDDEN") + "</white></gray>");
        enLore.add("");
        enLore.add("<yellow>Click to toggle.</yellow>");
        inventory.setItem(10, button(type.shopEnabled() ? Material.GOLD_INGOT : Material.GRAY_DYE,
                "<gold>Shop listing</gold>", enLore));

        List<String> catLore = new ArrayList<>();
        catLore.add("<gray>Groups the shop list. Current: <white>" + type.shopCategory() + "</white></gray>");
        catLore.add("");
        catLore.add("<yellow>Click to set (chat).</yellow>");
        inventory.setItem(11, button(Material.BOOKSHELF, "<gold>Category</gold>", catLore));

        inventory.setItem(13, button(Material.GOLD_INGOT, "<gold>Money price</gold>",
                stepperLore("Economy money charged.", plugin.output().formatMoney(cost.money()))));
        inventory.setItem(14, button(Material.EXPERIENCE_BOTTLE, "<gold>XP price</gold>",
                stepperLore("XP levels charged.", cost.xpLevels() + " levels")));

        List<String> itemLore = new ArrayList<>();
        itemLore.add("<gray>Current: <white>" + (cost.item() == null ? "none"
                : cost.itemAmount() + "x " + pretty(cost.item())) + "</white></gray>");
        itemLore.add("");
        itemLore.add("<yellow>Left-click: hold item, type to set.</yellow>");
        itemLore.add("<yellow>Right-click: amount ±1 (shift ±10).</yellow>");
        itemLore.add("<red>Shift-right-click: clear.</red>");
        Material icon = cost.item() == null ? Material.BARRIER
                : cost.item().isItem() ? cost.item() : Material.BARRIER;
        inventory.setItem(15, button(icon, "<gold>Item price</gold>", itemLore));
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
        Cost cost = type.shopCost();
        int d = delta(event);
        switch (event.getRawSlot()) {
            case 10 -> saveScalar(viewer, "shop.enabled", String.valueOf(!type.shopEnabled()));
            case 11 -> plugin.prompts().ask(viewer, "Shop category (e.g. Crops):",
                    input -> saveScalar(viewer, "shop.category", input.trim()),
                    () -> plugin.menus().open(viewer, this));
            case 13 -> {
                int dir = event.isRightClick() ? -1 : 1;
                double step = event.isShiftClick() ? 1000 : 100;
                saveScalar(viewer, "shop.price-money",
                        String.valueOf(Math.max(0, cost.money() + dir * step)));
            }
            case 14 -> saveScalar(viewer, "shop.price-xp-levels",
                    String.valueOf(Math.max(0, cost.xpLevels() + d)));
            case 15 -> {
                File f = typeFile();
                if (f == null) {
                    return;
                }
                try {
                    if (event.isRightClick()) {
                        if (event.isShiftClick()) {
                            if (TypeConfigWriter.clearShopItem(f) && reload(viewer)) {
                                viewer.sendMessage(Text.mm("<yellow>Item price cleared.</yellow>"));
                            }
                        } else if (cost.item() != null) {
                            int amount = Math.max(1, cost.itemAmount() + d);
                            if (TypeConfigWriter.setShopItem(f, cost.item().name(), amount) && reload(viewer)) {
                                viewer.sendMessage(Text.mm("<green>Item price: <white>" + amount
                                        + "x " + pretty(cost.item()) + "</white>.</green>"));
                            }
                        }
                    } else {
                        plugin.prompts().askHeldItem(viewer, "Hold the price item:", mat -> {
                            try {
                                if (TypeConfigWriter.setShopItem(f, mat.name(),
                                        Math.max(1, cost.itemAmount())) && reload(viewer)) {
                                    viewer.sendMessage(Text.mm("<green>Item price set to <white>"
                                            + pretty(mat) + "</white>.</green>"));
                                }
                            } catch (IOException e) {
                                viewer.sendMessage(Text.mm("<red>Could not write the type file.</red>"));
                            }
                            plugin.menus().open(viewer, new TypeShopMenu(plugin, typeId).withParent(parent()));
                        }, () -> plugin.menus().open(viewer, this));
                    }
                } catch (IOException e) {
                    viewer.sendMessage(Text.mm("<red>Could not write the type file.</red>"));
                }
            }
            default -> {
            }
        }
    }

    private static String pretty(Material m) {
        String s = m.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
