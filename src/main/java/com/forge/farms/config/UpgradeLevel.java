package com.forge.farms.config;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

/** One purchasable level within an upgrade track. */
public final class UpgradeLevel {
    private final int level;
    /** Track-dependent effect: radius blocks, tick interval, slots, or fuel multiplier. */
    private final double effect;
    private final double costMoney;
    private final @Nullable Material costItem;
    private final int costItemAmount;
    private final int costXpLevels;
    private final String description;

    public UpgradeLevel(int level, double effect, double costMoney,
            @Nullable Material costItem, int costItemAmount, int costXpLevels, String description) {
        this.level = level;
        this.effect = effect;
        this.costMoney = costMoney;
        this.costItem = costItem;
        this.costItemAmount = costItemAmount;
        this.costXpLevels = costXpLevels;
        this.description = description;
    }

    public int level() {
        return level;
    }

    public double effect() {
        return effect;
    }

    public double costMoney() {
        return costMoney;
    }

    public @Nullable Material costItem() {
        return costItem;
    }

    public int costItemAmount() {
        return costItemAmount;
    }

    /** XP levels charged for this upgrade (0 = none). */
    public int costXpLevels() {
        return costXpLevels;
    }

    public String description() {
        return description;
    }

    public boolean hasCost() {
        return costMoney > 0 || (costItem != null && costItemAmount > 0) || costXpLevels > 0;
    }
}
