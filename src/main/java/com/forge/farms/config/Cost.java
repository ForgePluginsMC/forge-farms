package com.forge.farms.config;

import com.forge.farms.output.EconomyBridge;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A purchasable cost: money (Vault/ForgeCore economy) + items + XP levels.
 * All specified parts are charged together (AND, not OR).
 */
public final class Cost {
    private final double money;
    private final @Nullable Material item;
    private final int itemAmount;
    private final int xpLevels;

    public Cost(double money, @Nullable Material item, int itemAmount, int xpLevels) {
        this.money = Math.max(0, money);
        this.item = item;
        this.itemAmount = Math.max(0, itemAmount);
        this.xpLevels = Math.max(0, xpLevels);
    }

    public static Cost free() {
        return new Cost(0, null, 0, 0);
    }

    /**
     * Parse from a flat map (upgrade level or shop section). Keys are
     * {@code <prefix>money}, {@code <prefix>item: {material, amount}},
     * {@code <prefix>xp-levels} — e.g. prefix "cost-" or "price-".
     */
    public static Cost parse(Map<?, ?> map, String prefix, File file, String path) {
        double money = toDouble(map.get(prefix + "money"));
        Material item = null;
        int amount = 0;
        Object ci = map.get(prefix + "item");
        if (ci instanceof Map<?, ?> cm) {
            Object mat = cm.get("material");
            if (mat != null) {
                item = Material.matchMaterial(mat.toString());
                if (item == null) {
                    throw new IllegalArgumentException(
                            file.getName() + ": unknown material '" + mat + "' at " + path);
                }
            }
            amount = toInt(cm.get("amount"));
        }
        int xp = toInt(map.get(prefix + "xp-levels"));
        return new Cost(money, item, amount, xp);
    }

    public double money() {
        return money;
    }

    public @Nullable Material item() {
        return item;
    }

    public int itemAmount() {
        return itemAmount;
    }

    public int xpLevels() {
        return xpLevels;
    }

    public boolean isFree() {
        return money <= 0 && (item == null || itemAmount <= 0) && xpLevels <= 0;
    }

    /** Human-readable lines for every unmet requirement. Empty = affordable. */
    public List<String> unmet(Player player, EconomyBridge economy) {
        List<String> out = new ArrayList<>();
        if (money > 0) {
            if (!economy.isAvailable()) {
                out.add("needs an economy (Vault or ForgeCore)");
            } else if (economy.balance(player) < money) {
                out.add("needs " + String.format("$%,.2f", money));
            }
        }
        if (item != null && itemAmount > 0 && countItems(player, item) < itemAmount) {
            out.add("needs " + itemAmount + "x " + pretty(item));
        }
        if (xpLevels > 0 && player.getLevel() < xpLevels) {
            out.add("needs " + xpLevels + " XP levels");
        }
        return out;
    }

    /**
     * Charge everything. Call {@link #unmet} first; if money withdrawal
     * fails midway, items/XP are untouched (money is charged first).
     */
    public boolean charge(Player player, EconomyBridge economy) {
        if (money > 0 && !economy.withdraw(player, money)) {
            return false;
        }
        if (item != null && itemAmount > 0) {
            player.getInventory().removeItem(new ItemStack(item, itemAmount));
        }
        if (xpLevels > 0) {
            player.setLevel(Math.max(0, player.getLevel() - xpLevels));
        }
        return true;
    }

    /** MiniMessage line for GUI lore, e.g. "$500 + 3x diamond + 10 XP levels" or "Free". */
    public String describe(Function<Double, String> moneyFormat) {
        List<String> parts = new ArrayList<>();
        if (money > 0) {
            parts.add("<white>" + moneyFormat.apply(money) + "</white>");
        }
        if (item != null && itemAmount > 0) {
            parts.add("<white>" + itemAmount + "x " + pretty(item).toLowerCase(Locale.ROOT) + "</white>");
        }
        if (xpLevels > 0) {
            parts.add("<white>" + xpLevels + " XP levels</white>");
        }
        if (parts.isEmpty()) {
            return "<white>Free</white>";
        }
        return String.join(" <gray>+</gray> ", parts);
    }

    private static int countItems(Player player, Material material) {
        int n = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                n += item.getAmount();
            }
        }
        return n;
    }

    private static String pretty(Material material) {
        String[] parts = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    private static double toDouble(@Nullable Object o) {
        return o instanceof Number n ? n.doubleValue() : 0;
    }

    private static int toInt(@Nullable Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }
}
