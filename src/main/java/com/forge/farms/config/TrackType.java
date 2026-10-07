package com.forge.farms.config;

/**
 * Independent upgrade tracks. Each farm levels these separately —
 * no bundled levels like the premium competition.
 */
public enum TrackType {
    /** Widens the working radius. Effect value = radius in blocks. */
    RADIUS("radius"),
    /** Speeds up growth ticks. Effect value = tick interval. */
    SPEED("speed"),
    /** Expands virtual storage. Effect value = slot count. */
    STORAGE("storage"),
    /** Stretches fuel further. Effect value = fuel multiplier. */
    EFFICIENCY("efficiency"),
    /** Auto-plows soil and hydrates farmland. Effect value = feature level (1 = till, 2 = till + hydrate). */
    TILLING("tilling");

    private final String key;

    TrackType(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static TrackType fromKey(String key) {
        for (TrackType t : values()) {
            if (t.key.equalsIgnoreCase(key)) {
                return t;
            }
        }
        return null;
    }
}
