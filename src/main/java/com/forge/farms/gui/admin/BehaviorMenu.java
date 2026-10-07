package com.forge.farms.gui.admin;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.FarmType;
import com.forge.farms.core.Text;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Admin: behavior knobs of one farm type. */
public final class BehaviorMenu extends TypeMenu {
    public BehaviorMenu(ForgeFarms plugin, String typeId) {
        super(plugin, typeId);
    }

    @Override
    public void build(Player viewer) {
        FarmType type = type();
        create(viewer, 54, "<dark_red><bold>Behavior</bold></dark_red> <gray>· " + typeId + "</gray>");
        fill(filler());
        if (type == null) {
            navRow();
            return;
        }
        intKnob(10, "farm.base-radius", 1, 30, type.baseRadius(), Material.GRASS_BLOCK,
                "Base radius", "Working radius in blocks (before upgrades).");
        secondsKnob(11, "farm.tick-interval", 5L, 600L, type.baseTickInterval(), Material.CLOCK,
                "Tick interval", "Seconds between growth ticks.");
        intKnob(12, "farm.growth-attempts", 1, 64, type.growthAttempts(), Material.WHEAT,
                "Growth attempts", "Blocks sampled per tick.");
        intKnob(13, "farm.age-per-sample", 1, 7, type.agePerSample(), Material.BONE_MEAL,
                "Growth boost", "Crop ages added per sample.");
        boolKnob(14, "farm.harvest-sweep", type.harvestSweep(), Material.IRON_HOE,
                "Harvest sweep", "Scan the whole radius for ripe crops each tick.");
        intKnob(15, "farm.max-harvests-per-tick", 0, 256, type.maxHarvestsPerTick(), Material.HOPPER,
                "Harvest cap", "Max harvests per tick. 0 = unlimited.");
        intKnob(16, "storage.base-slots", 9, 54, type.baseStorageSlots(), Material.CHEST,
                "Base storage slots", "Virtual storage before upgrades. Step 9.");
        intKnob(17, "farm.till-attempts", 1, 64, type.tillAttempts(), Material.IRON_HOE,
                "Tilling attempts", "Soil blocks sampled per tick for plow/hydrate.");
        intKnob(19, "offline.max-hours", 0, 72, type.maxOfflineHours(), Material.BOOK,
                "Offline catch-up", "Max simulated hours while owner is offline.");
        boolKnob(20, "hologram.enabled", type.hologramDefault(), Material.SEA_LANTERN,
                "Hologram", "Floating info hologram above the core.");

        List<String> nameLore = new ArrayList<>();
        nameLore.add("<gray>Current: <white>" + Text.plain(type.displayName()) + "</white></gray>");
        nameLore.add("");
        nameLore.add("<yellow>Click to rename (chat).</yellow>");
        inventory.setItem(21, button(Material.NAME_TAG, "<gold>Display name</gold>", nameLore));

        List<String> descLore = new ArrayList<>();
        descLore.add("<gray>" + type.descriptionLines().size() + " line(s).</gray>");
        descLore.add("");
        descLore.add("<yellow>Click to edit lines.</yellow>");
        inventory.setItem(22, button(Material.PAPER, "<gold>Description</gold>", descLore));

        List<String> coreLore = new ArrayList<>();
        coreLore.add("<gray>Current: <white>" + pretty(type.coreBlock()) + "</white></gray>");
        coreLore.add("");
        coreLore.add("<yellow>Click, then hold the block.</yellow>");
        inventory.setItem(23, button(type.coreBlock().isBlock() ? type.coreBlock() : Material.STONE,
                "<gold>Core block</gold>", coreLore));
        navRow();
    }

    private void intKnob(int slot, String path, int min, int max, int current,
            Material icon, String name, String hint) {
        List<String> lore = stepperLore(hint, String.valueOf(current));
        inventory.setItem(slot, button(icon, "<gold>" + name + "</gold>", lore));
    }

    private void secondsKnob(int slot, String path, long min, long max, long ticks,
            Material icon, String name, String hint) {
        List<String> lore = stepperLore(hint, String.format("%.2fs", ticks / 20.0));
        inventory.setItem(slot, button(icon, "<gold>" + name + "</gold>", lore));
    }

    private void boolKnob(int slot, String path, boolean current, Material icon, String name, String hint) {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>" + hint + "</gray>");
        lore.add("");
        lore.add("<gray>Currently: <white>" + (current ? "ON" : "OFF") + "</white></gray>");
        lore.add("");
        lore.add("<yellow>Click to toggle.</yellow>");
        inventory.setItem(slot, button(current ? icon : Material.GRAY_DYE, "<gold>" + name + "</gold>", lore));
    }

    @Override
    public void click(Player viewer, InventoryClickEvent event) {
        if (!isTopClick(event) || navClick(viewer, event)) {
            return;
        }
        FarmType type = type();
        if (type == null) {
            return;
        }
        int d = delta(event);
        switch (event.getRawSlot()) {
            case 10 -> saveScalar(viewer, "farm.base-radius",
                    String.valueOf(clamp(type.baseRadius() + d, 1, 30)));
            case 11 -> {
                long ticks = clamp((int) (type.baseTickInterval() + d * 20L), 5, 600);
                saveScalar(viewer, "farm.tick-interval", String.valueOf(ticks));
            }
            case 12 -> saveScalar(viewer, "farm.growth-attempts",
                    String.valueOf(clamp(type.growthAttempts() + d, 1, 64)));
            case 13 -> saveScalar(viewer, "farm.age-per-sample",
                    String.valueOf(clamp(type.agePerSample() + d, 1, 7)));
            case 14 -> saveScalar(viewer, "farm.harvest-sweep", String.valueOf(!type.harvestSweep()));
            case 15 -> saveScalar(viewer, "farm.max-harvests-per-tick",
                    String.valueOf(clamp(type.maxHarvestsPerTick() + d, 0, 256)));
            case 16 -> {
                int slots = clamp(type.baseStorageSlots() + (d > 0 ? 9 : -9), 9, 54);
                saveScalar(viewer, "storage.base-slots", String.valueOf(slots));
            }
            case 17 -> saveScalar(viewer, "farm.till-attempts",
                    String.valueOf(clamp(type.tillAttempts() + d, 1, 64)));
            case 19 -> saveScalar(viewer, "offline.max-hours",
                    String.valueOf(clamp(type.maxOfflineHours() + d, 0, 72)));
            case 20 -> saveScalar(viewer, "hologram.enabled", String.valueOf(!type.hologramDefault()));
            case 21 -> plugin.prompts().ask(viewer, "New display name (MiniMessage allowed):", input ->
                    saveScalar(viewer, "display-name", "\"" + input.replace("\"", "'") + "\""),
                    () -> plugin.menus().open(viewer, this));
            case 22 -> plugin.menus().open(viewer, new StringListMenu(plugin, typeId,
                    "description", "Description", "Type a description line:",
                    type.descriptionLines()).withParent(this));
            case 23 -> plugin.prompts().askHeldItem(viewer, "Hold the new core block and type anything:",
                    mat -> {
                        if (!mat.isBlock()) {
                            viewer.sendMessage(Text.mm("<red>That's not a block.</red>"));
                            plugin.menus().open(viewer, this);
                            return;
                        }
                        saveScalar(viewer, "core-block", mat.name());
                    }, () -> plugin.menus().open(viewer, this));
            default -> {
            }
        }
    }

    private static String pretty(Material m) {
        String s = m.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
