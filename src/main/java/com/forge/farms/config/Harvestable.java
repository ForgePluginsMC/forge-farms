package com.forge.farms.config;

import org.bukkit.Material;

/** One harvestable block entry from a farm type config. */
public final class Harvestable {
    private final Material material;
    private final HarvestBehavior behavior;

    public Harvestable(Material material, HarvestBehavior behavior) {
        this.material = material;
        this.behavior = behavior;
    }

    public Material material() {
        return material;
    }

    public HarvestBehavior behavior() {
        return behavior;
    }
}
