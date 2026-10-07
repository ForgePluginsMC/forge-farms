package com.forge.farms.gui.admin;

import org.bukkit.Material;

/** The five configurable effect slots of a farm type. */
public enum EffectSlot {
    HARVEST_PARTICLE("Harvest particles", Material.FIREWORK_STAR,
            "effects.harvest-particle", "effects.harvest-particle-count", null, null, true),
    HARVEST_SOUND("Harvest sound", Material.NOTE_BLOCK,
            "effects.harvest-sound", null, "effects.harvest-sound-volume", "effects.harvest-sound-pitch", false),
    GROWTH_PARTICLE("Growth particles", Material.COMPOSTER,
            "effects.growth-particle", "effects.growth-particle-count", null, null, true),
    WORKING_AURA("Working aura", Material.GLOWSTONE_DUST,
            "effects.working-particle", "effects.working-particle-count", null, null, true),
    RADIUS_RING("Radius ring", Material.COMPASS,
            "effects.radius-particle", "effects.radius-particle-count",
            "effects.radius-particle-interval", null, true);

    private final String title;
    private final Material icon;
    private final String mainPath;
    private final String countPath;
    private final String extraPath;
    private final String extra2Path;
    private final boolean particle;

    EffectSlot(String title, Material icon, String mainPath, String countPath,
            String extraPath, String extra2Path, boolean particle) {
        this.title = title;
        this.icon = icon;
        this.mainPath = mainPath;
        this.countPath = countPath;
        this.extraPath = extraPath;
        this.extra2Path = extra2Path;
        this.particle = particle;
    }

    public String title() {
        return title;
    }

    public Material icon() {
        return icon;
    }

    public String mainPath() {
        return mainPath;
    }

    public String countPath() {
        return countPath;
    }

    public String extraPath() {
        return extraPath;
    }

    public String extra2Path() {
        return extra2Path;
    }

    public boolean isParticle() {
        return particle;
    }
}
