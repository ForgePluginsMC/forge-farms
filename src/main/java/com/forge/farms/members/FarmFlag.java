package com.forge.farms.members;

/** Granular permissions checked inside a farm's radius. */
public enum FarmFlag {
    /** Break non-core blocks inside the farm. */
    BLOCK_BREAK,
    /** Place blocks inside the farm. */
    BLOCK_PLACE,
    /** Manually harvest crops / take from farm blocks. */
    HARVEST,
    /** Plant crops or saplings inside the farm. */
    PLANT,
    /** Open the farm menu / interact with the core. */
    INTERACT,
    /** Buy upgrades. */
    UPGRADE,
    /** Add fuel. */
    FUEL,
    /** Change members, roles, settings. */
    CONFIGURE,
    /** Break the farm core (removes the farm). */
    DELETE
}
