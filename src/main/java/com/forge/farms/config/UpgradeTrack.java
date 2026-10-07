package com.forge.farms.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/** One independent upgrade track: an ordered list of purchasable levels. */
public final class UpgradeTrack {
    private final TrackType type;
    private final List<UpgradeLevel> levels;

    public UpgradeTrack(TrackType type, List<UpgradeLevel> levels) {
        this.type = type;
        this.levels = Collections.unmodifiableList(new ArrayList<>(levels));
    }

    public TrackType type() {
        return type;
    }

    public int maxLevel() {
        return levels.size();
    }

    /** Level numbers start at 1; returns null past the end. */
    public @Nullable UpgradeLevel level(int level) {
        if (level < 1 || level > levels.size()) {
            return null;
        }
        return levels.get(level - 1);
    }
}
