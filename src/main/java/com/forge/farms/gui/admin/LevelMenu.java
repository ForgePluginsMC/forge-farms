package com.forge.farms.gui.admin;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.Cost;
import com.forge.farms.config.FarmType;
import com.forge.farms.config.TrackType;
import com.forge.farms.config.TypeConfigWriter;
import com.forge.farms.config.UpgradeLevel;
import com.forge.farms.config.UpgradeTrack;
import com.forge.farms.core.Text;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Admin: edit one upgrade level (effect, costs, description, delete). */
public final class LevelMenu extends TypeMenu {
    private final TrackType track;
    private final int level;
    private boolean deleteArmed;

    public LevelMenu(ForgeFarms plugin, String typeId, TrackType track, int level) {
        super(plugin, typeId);
        this.track = track;
        this.level = level;
    }

    @Override
    public void build(Player viewer) {
        FarmType type = type();
        UpgradeLevel lvl = level(type);
        create(viewer, 36, "<dark_red><bold>" + plugin.upgrades().trackTitle(track)
                + " " + level + "</bold></dark_red>");
        fill(filler());
        if (type == null || lvl == null) {
            navRow();
            return;
        }
        Cost cost = lvl.cost();
        effectKnob(10, lvl);
        moneyKnob(11, cost);
        itemKnob(12, cost);
        xpKnob(13, cost);

        List<String> descLore = new ArrayList<>();
        descLore.add("<gray>Current: <white>" + (lvl.description().isEmpty() ? "(auto)" : lvl.description()) + "</white></gray>");
        descLore.add("");
        descLore.add("<yellow>Click to set (chat).</yellow>");
        inventory.setItem(14, button(Material.PAPER, "<gold>Description</gold>", descLore));

        List<String> delLore = new ArrayList<>();
        delLore.add(deleteArmed
                ? "<red><bold>Click again to confirm deletion.</bold></red>"
                : "<gray>Remove this level from the track.</gray>");
        delLore.add("");
        delLore.add(deleteArmed ? "<red>Confirm delete.</red>" : "<red>Click to delete.</red>");
        inventory.setItem(16, button(deleteArmed ? Material.TNT : Material.BARRIER,
                "<red>Delete level</red>", delLore));
        navRow();
    }

    private void effectKnob(int slot, UpgradeLevel lvl) {
        List<String> lore = stepperLore(effectHint(), effectDisplay(lvl));
        inventory.setItem(slot, button(Material.EXPERIENCE_BOTTLE, "<gold>Effect</gold>", lore));
    }

    private String effectHint() {
        return switch (track) {
            case RADIUS -> "Radius in blocks.";
            case SPEED -> "Tick interval in seconds.";
            case STORAGE -> "Storage slots.";
            case EFFICIENCY -> "Fuel multiplier.";
            case TILLING -> "1 = plow, 2 = plow + hydrate.";
        };
    }

    private String effectDisplay(UpgradeLevel lvl) {
        return switch (track) {
            case RADIUS -> String.valueOf((int) lvl.effect());
            case SPEED -> (lvl.effect() / 20.0) + "s";
            case STORAGE -> String.valueOf((int) lvl.effect());
            case EFFICIENCY -> "x" + lvl.effect();
            case TILLING -> String.valueOf((int) lvl.effect());
        };
    }

    private void moneyKnob(int slot, Cost cost) {
        inventory.setItem(slot, button(Material.GOLD_INGOT, "<gold>Money cost</gold>",
                stepperLore("Economy money charged.", plugin.output().formatMoney(cost.money()))));
    }

    private void itemKnob(int slot, Cost cost) {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Current: <white>" + (cost.item() == null ? "none"
                : cost.itemAmount() + "x " + pretty(cost.item())) + "</white></gray>");
        lore.add("");
        lore.add("<yellow>Left-click: hold item, type to set.</yellow>");
        lore.add("<yellow>Right-click: amount ±1 (shift ±10).</yellow>");
        lore.add("<red>Shift-right-click with no item: clear.</red>");
        Material icon = cost.item() == null ? Material.BARRIER
                : cost.item().isItem() ? cost.item() : Material.BARRIER;
        inventory.setItem(slot, button(icon, "<gold>Item cost</gold>", lore));
    }

    private void xpKnob(int slot, Cost cost) {
        inventory.setItem(slot, button(Material.EXPERIENCE_BOTTLE, "<gold>XP cost</gold>",
                stepperLore("XP levels charged.", cost.xpLevels() + " levels")));
    }

    private @org.jetbrains.annotations.Nullable UpgradeLevel level(@org.jetbrains.annotations.Nullable FarmType type) {
        if (type == null) {
            return null;
        }
        UpgradeTrack t = type.upgradeTrack(track);
        return t == null ? null : t.level(level);
    }

    @Override
    public void click(Player viewer, InventoryClickEvent event) {
        if (!isTopClick(event) || navClick(viewer, event)) {
            return;
        }
        FarmType type = type();
        UpgradeLevel lvl = level(type);
        if (lvl == null) {
            return;
        }
        Cost cost = lvl.cost();
        int d = delta(event);
        switch (event.getRawSlot()) {
            case 10 -> writeLevel(viewer, lvl, bumpEffect(lvl.effect(), d), cost, lvl.description());
            case 11 -> {
                int dir = event.isRightClick() ? -1 : 1;
                double step = event.isShiftClick() ? 1000 : 100;
                double money = Math.max(0, cost.money() + dir * step);
                writeLevel(viewer, lvl, lvl.effect(),
                        new Cost(money, cost.item(), cost.itemAmount(), cost.xpLevels()), lvl.description());
            }
            case 12 -> {
                if (event.isRightClick()) {
                    if (event.isShiftClick()) {
                        writeLevel(viewer, lvl, lvl.effect(),
                                new Cost(cost.money(), null, 1, cost.xpLevels()), lvl.description());
                    } else if (cost.item() != null) {
                        int amount = Math.max(1, cost.itemAmount() + d);
                        writeLevel(viewer, lvl, lvl.effect(),
                                new Cost(cost.money(), cost.item(), amount, cost.xpLevels()), lvl.description());
                    }
                } else {
                    plugin.prompts().askHeldItem(viewer, "Hold the cost item:", mat ->
                            writeLevel(viewer, lvl, lvl.effect(),
                                    new Cost(cost.money(), mat, Math.max(1, cost.itemAmount()), cost.xpLevels()),
                                    lvl.description()),
                            () -> plugin.menus().open(viewer, this));
                }
            }
            case 13 -> writeLevel(viewer, lvl, lvl.effect(),
                    new Cost(cost.money(), cost.item(), cost.itemAmount(),
                            Math.max(0, cost.xpLevels() + d)), lvl.description());
            case 14 -> plugin.prompts().ask(viewer, "Level description (empty = auto):", input ->
                    writeLevel(viewer, lvl, lvl.effect(), cost, input),
                    () -> plugin.menus().open(viewer, this));
            case 16 -> {
                if (!deleteArmed) {
                    deleteArmed = true;
                    plugin.menus().refresh(viewer);
                    return;
                }
                File f = typeFile();
                if (f == null) {
                    return;
                }
                try {
                    if (TypeConfigWriter.removeUpgradeLevel(f, track.key(), level - 1) && reload(viewer)) {
                        viewer.sendMessage(Text.mm("<yellow>Level " + level + " deleted.</yellow>"));
                        plugin.menus().open(viewer, new TrackMenu(plugin, typeId, track).withParent(parent()));
                    }
                } catch (IOException e) {
                    viewer.sendMessage(Text.mm("<red>Could not write the type file.</red>"));
                }
            }
            default -> {
            }
        }
    }

    private double bumpEffect(double effect, int d) {
        return switch (track) {
            case RADIUS -> Math.max(1, (int) effect + d);
            case SPEED -> Math.max(5, (int) effect + d * 20);
            case STORAGE -> Math.max(9, (int) effect + (d > 0 ? 9 : -9));
            case EFFICIENCY -> Math.max(1.0, Math.round((effect + d * 0.25) * 100) / 100.0);
            case TILLING -> Math.max(1, Math.min(2, (int) effect + d));
        };
    }

    private void writeLevel(Player viewer, UpgradeLevel lvl, double effect, Cost cost, String description) {
        File f = typeFile();
        if (f == null) {
            return;
        }
        StringBuilder flow = new StringBuilder();
        flow.append("effect: ").append(formatEffect(effect));
        flow.append(", cost-money: ").append(cost.money());
        if (cost.item() != null) {
            flow.append(", cost-item: { material: ").append(cost.item().name())
                .append(", amount: ").append(cost.itemAmount()).append(" }");
        }
        if (cost.xpLevels() > 0) {
            flow.append(", cost-xp-levels: ").append(cost.xpLevels());
        }
        flow.append(", description: \"").append(description.replace("\"", "'")).append("\"");
        try {
            if (TypeConfigWriter.setUpgradeLevel(f, track.key(), level - 1, flow.toString())) {
                reload(viewer);
            }
        } catch (IOException e) {
            viewer.sendMessage(Text.mm("<red>Could not write the type file.</red>"));
        }
    }

    private String formatEffect(double effect) {
        return switch (track) {
            case RADIUS, STORAGE, TILLING -> String.valueOf((int) effect);
            case SPEED -> String.valueOf((int) effect);
            case EFFICIENCY -> String.valueOf(effect);
        };
    }

    private static String pretty(Material m) {
        String s = m.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
