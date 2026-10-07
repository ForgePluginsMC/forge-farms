package com.forge.farms.gui;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.FarmType;
import com.forge.farms.core.Text;
import com.forge.farms.farm.Farm;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Per-farm tuning knobs: every behavior dial from the farm-type config,
 * adjustable live in-game. Overrides are stored per farm and persist;
 * "Reset" drops back to the type defaults.
 */
public final class FarmTuneMenu extends Menu {
    private final Farm farm;

    public FarmTuneMenu(ForgeFarms plugin, Farm farm) {
        super(plugin);
        this.farm = farm;
    }

    @Override
    public void build(Player viewer) {
        FarmType type = plugin.config().getType(farm.typeId());
        String title = type == null ? "Farm" : Text.plain(type.displayName());
        create(viewer, 27, "<dark_green><bold>" + title + " <gray>·</gray> Tuning</bold></dark_green>");
        fill(filler());
        if (type == null) {
            return;
        }

        intKnob(10, Farm.SETTING_GROWTH_ATTEMPTS, 1, 64, type.growthAttempts(),
                Material.WHEAT, "Growth attempts", "Blocks sampled per tick.");
        intKnob(11, Farm.SETTING_AGE_PER_SAMPLE, 1, 7, type.agePerSample(),
                Material.BONE_MEAL, "Growth boost", "Crop ages added per sample.");
        boolKnob(12, Farm.SETTING_HARVEST_SWEEP, type.harvestSweep(),
                Material.IRON_HOE, "Harvest sweep", "Scan the whole radius for ripe crops each tick.");
        intKnob(13, Farm.SETTING_MAX_HARVESTS_PER_TICK, 0, 256, type.maxHarvestsPerTick(),
                Material.HOPPER, "Harvest cap", "Max harvests per tick. 0 = unlimited.");
        boolKnob(14, Farm.SETTING_FX_HARVEST_PARTICLES, true,
                Material.FIREWORK_STAR, "Harvest particles", "Burst of particles on each harvest.");
        boolKnob(15, Farm.SETTING_FX_HARVEST_SOUND, true,
                Material.NOTE_BLOCK, "Harvest sound", "Sound played on each harvest.");
        boolKnob(16, Farm.SETTING_FX_WORKING_AURA, true,
                Material.GLOWSTONE_DUST, "Working aura", "Glow at the core while the farm is fueled.");
        boolKnob(17, Farm.SETTING_FX_RADIUS_RING, true,
                Material.COMPASS, "Radius ring", "Particle ring showing the working radius.");

        List<String> resetLore = new ArrayList<>();
        resetLore.add("<gray>Drop every override and use</gray>");
        resetLore.add("<gray>the farm type's configured values.</gray>");
        resetLore.add("");
        resetLore.add(farm.hasCustomSettings()
                ? "<yellow>Click to reset.</yellow>"
                : "<gray>No overrides set.</gray>");
        inventory.setItem(22, button(Material.BARRIER, "<red>Reset to type defaults</red>", resetLore));
    }

    private void intKnob(int slot, String key, int min, int max, int def,
            Material icon, String name, String hint) {
        int current = clamp(farm.intSetting(key, def), min, max);
        List<String> lore = new ArrayList<>();
        lore.add("<gray>" + hint + "</gray>");
        lore.add("");
        lore.add("<gray>Current: <white>" + label(key, current) + "</white></gray>");
        lore.add("<gray>Type default: <white>" + label(key, def) + "</white></gray>");
        if (current != def) {
            lore.add("<aqua>Overridden</aqua>");
        }
        lore.add("");
        lore.add("<yellow>Left-click: +1 · Right-click: -1</yellow>");
        lore.add("<yellow>Shift-click: ±10</yellow>");
        inventory.setItem(slot, button(icon, "<gold>" + name + "</gold>", lore));
    }

    private void boolKnob(int slot, String key, boolean def, Material icon, String name, String hint) {
        boolean current = farm.boolSetting(key, def);
        List<String> lore = new ArrayList<>();
        lore.add("<gray>" + hint + "</gray>");
        lore.add("");
        lore.add("<gray>Currently: <white>" + (current ? "ON" : "OFF") + "</white></gray>");
        lore.add("");
        lore.add("<yellow>Click to toggle.</yellow>");
        inventory.setItem(slot, button(current ? icon : Material.GRAY_DYE, "<gold>" + name + "</gold>", lore));
    }

    private String label(String key, int value) {
        if (Farm.SETTING_MAX_HARVESTS_PER_TICK.equals(key) && value == 0) {
            return "Unlimited";
        }
        return String.valueOf(value);
    }

    @Override
    public void click(Player viewer, InventoryClickEvent event) {
        if (!isTopClick(event)) {
            return;
        }
        switch (event.getRawSlot()) {
            case 10 -> stepInt(viewer, Farm.SETTING_GROWTH_ATTEMPTS, 1, 64, delta(event));
            case 11 -> stepInt(viewer, Farm.SETTING_AGE_PER_SAMPLE, 1, 7, delta(event));
            case 12 -> toggle(viewer, Farm.SETTING_HARVEST_SWEEP);
            case 13 -> stepInt(viewer, Farm.SETTING_MAX_HARVESTS_PER_TICK, 0, 256, delta(event));
            case 14 -> toggle(viewer, Farm.SETTING_FX_HARVEST_PARTICLES);
            case 15 -> toggle(viewer, Farm.SETTING_FX_HARVEST_SOUND);
            case 16 -> toggle(viewer, Farm.SETTING_FX_WORKING_AURA);
            case 17 -> toggle(viewer, Farm.SETTING_FX_RADIUS_RING);
            case 22 -> {
                farm.clearSettings();
                plugin.farms().saveFarm(farm);
                viewer.sendMessage(Text.mm("<green>Tuning reset to the farm type's defaults.</green>"));
                plugin.menus().refresh(viewer);
            }
            default -> {
            }
        }
    }

    private int delta(InventoryClickEvent event) {
        int step = event.isShiftClick() ? 10 : 1;
        return event.isRightClick() ? -step : step;
    }

    private void stepInt(Player viewer, String key, int min, int max, int delta) {
        FarmType type = plugin.config().getType(farm.typeId());
        int def = type == null ? min : typeDefault(key, type);
        int next = clamp(farm.intSetting(key, def) + delta, min, max);
        farm.setSetting(key, String.valueOf(next));
        plugin.farms().saveFarm(farm);
        plugin.menus().refresh(viewer);
    }

    private void toggle(Player viewer, String key) {
        farm.setSetting(key, String.valueOf(!farm.boolSetting(key, true)));
        plugin.farms().saveFarm(farm);
        plugin.menus().refresh(viewer);
    }

    private int typeDefault(String key, FarmType type) {
        return switch (key) {
            case Farm.SETTING_GROWTH_ATTEMPTS -> type.growthAttempts();
            case Farm.SETTING_AGE_PER_SAMPLE -> type.agePerSample();
            case Farm.SETTING_MAX_HARVESTS_PER_TICK -> type.maxHarvestsPerTick();
            default -> 0;
        };
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
