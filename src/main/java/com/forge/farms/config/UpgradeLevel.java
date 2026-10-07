package com.forge.farms.config;

/** One purchasable level within an upgrade track. */
public final class UpgradeLevel {
    private final int level;
    /** Track-dependent effect: radius blocks, tick interval, slots, or fuel multiplier. */
    private final double effect;
    private final Cost cost;
    private final String description;

    public UpgradeLevel(int level, double effect, Cost cost, String description) {
        this.level = level;
        this.effect = effect;
        this.cost = cost;
        this.description = description;
    }

    public int level() {
        return level;
    }

    public double effect() {
        return effect;
    }

    public Cost cost() {
        return cost;
    }

    public String description() {
        return description;
    }
}
