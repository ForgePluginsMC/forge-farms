package com.forge.farms.config;

/**
 * How a harvestable block behaves when the growth engine samples it.
 */
public enum HarvestBehavior {
    /**
     * Ageable crops (wheat, carrots, potatoes, beetroots, nether wart,
     * sweet berries, cocoa). Accelerated aging while immature; harvested
     * at max age and replanted at age 0.
     */
    AGEABLE_CROP,
    /**
     * Stem fruit (melon, pumpkin). The fruit block is harvested, the stem
     * is left to grow another.
     */
    STEM_FRUIT,
    /**
     * Vertical stalks (sugar cane, cactus, bamboo, kelp). Everything above
     * the base block is harvested, the base is left to regrow.
     */
    STALK
}
