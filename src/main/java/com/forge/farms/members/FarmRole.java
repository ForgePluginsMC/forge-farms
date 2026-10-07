package com.forge.farms.members;

import java.util.EnumSet;
import java.util.Set;

/** Member roles with their default flag sets. */
public enum FarmRole {
    OWNER(EnumSet.allOf(FarmFlag.class)),
    ADMIN(EnumSet.of(
            FarmFlag.BLOCK_BREAK, FarmFlag.BLOCK_PLACE, FarmFlag.HARVEST,
            FarmFlag.PLANT, FarmFlag.INTERACT, FarmFlag.UPGRADE,
            FarmFlag.FUEL, FarmFlag.CONFIGURE)),
    MEMBER(EnumSet.of(
            FarmFlag.BLOCK_BREAK, FarmFlag.BLOCK_PLACE, FarmFlag.HARVEST,
            FarmFlag.PLANT, FarmFlag.INTERACT, FarmFlag.FUEL)),
    GUEST(EnumSet.of(FarmFlag.INTERACT));

    private final Set<FarmFlag> flags;

    FarmRole(Set<FarmFlag> flags) {
        this.flags = flags;
    }

    public boolean has(FarmFlag flag) {
        return flags.contains(flag);
    }

    public Set<FarmFlag> flags() {
        return EnumSet.copyOf(flags);
    }

    public static FarmRole parse(String name) {
        for (FarmRole r : values()) {
            if (r.name().equalsIgnoreCase(name)) {
                return r;
            }
        }
        return MEMBER;
    }

    /** Next role when cycling in the members GUI (owner excluded). */
    public FarmRole next() {
        return switch (this) {
            case GUEST -> MEMBER;
            case MEMBER -> ADMIN;
            case ADMIN -> GUEST;
            case OWNER -> OWNER;
        };
    }
}
