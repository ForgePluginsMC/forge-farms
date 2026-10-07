package com.forge.farms.gui.admin;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.FarmType;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Admin: edit one effect slot (particle/sound + counts). */
public final class EffectSlotMenu extends TypeMenu {
    private final EffectSlot slot;

    public EffectSlotMenu(ForgeFarms plugin, String typeId, EffectSlot slot) {
        super(plugin, typeId);
        this.slot = slot;
    }

    @Override
    public void build(Player viewer) {
        FarmType type = type();
        create(viewer, 36, "<dark_red><bold>" + slot.title() + "</bold></dark_red>");
        fill(filler());
        if (type == null) {
            navRow();
            return;
        }
        String current = currentMain(type);
        List<String> pickLore = new ArrayList<>();
        pickLore.add("<gray>Current: <white>" + current + "</white></gray>");
        pickLore.add("");
        pickLore.add("<yellow>Click to choose " + (slot.isParticle() ? "a particle" : "a sound") + ".</yellow>");
        inventory.setItem(10, button(slot.icon(), "<gold>Choose " + (slot.isParticle() ? "particle" : "sound") + "</gold>", pickLore));

        List<String> offLore = new ArrayList<>();
        offLore.add("<gray>Disable this effect (sets NONE).</gray>");
        offLore.add("");
        offLore.add("<red>Click to disable.</red>");
        inventory.setItem(11, button(Material.GRAY_DYE, "<gold>Disable</gold>", offLore));

        if (slot.countPath() != null) {
            int count = countValue(type);
            inventory.setItem(13, button(Material.REPEATER, "<gold>Count</gold>",
                    stepperLore("Particles per trigger.", String.valueOf(count))));
        }
        if (slot == EffectSlot.HARVEST_SOUND) {
            inventory.setItem(14, button(Material.JUKEBOX, "<gold>Volume</gold>",
                    stepperLore("Sound volume.", String.format("%.1f", type.harvestSoundVolume()))));
            inventory.setItem(15, button(Material.NOTE_BLOCK, "<gold>Pitch</gold>",
                    stepperLore("Sound pitch.", String.format("%.1f", type.harvestSoundPitch()))));
        }
        if (slot == EffectSlot.RADIUS_RING) {
            inventory.setItem(14, button(Material.CLOCK, "<gold>Interval</gold>",
                    stepperLore("Farm ticks between ring draws.", String.valueOf(type.radiusParticleInterval()))));
        }
        navRow();
    }

    private String currentMain(FarmType type) {
        return switch (slot) {
            case HARVEST_PARTICLE -> type.harvestParticle() == null ? "OFF" : type.harvestParticle().name();
            case HARVEST_SOUND -> type.harvestSound() == null ? "OFF" : type.harvestSoundName();
            case GROWTH_PARTICLE -> type.growthParticle() == null ? "OFF" : type.growthParticle().name();
            case WORKING_AURA -> type.workingParticle() == null ? "OFF" : type.workingParticle().name();
            case RADIUS_RING -> type.radiusParticle() == null ? "OFF" : type.radiusParticle().name();
        };
    }

    private int countValue(FarmType type) {
        return switch (slot) {
            case HARVEST_PARTICLE -> type.harvestParticleCount();
            case GROWTH_PARTICLE -> type.growthParticleCount();
            case WORKING_AURA -> type.workingParticleCount();
            case RADIUS_RING -> type.radiusParticleCount();
            default -> 0;
        };
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
            case 10 -> {
                if (slot.isParticle()) {
                    plugin.menus().open(viewer, new ParticlePickerMenu(plugin, typeId, slot).withParent(this));
                } else {
                    plugin.menus().open(viewer, new SoundPickerMenu(plugin, typeId, slot).withParent(this));
                }
            }
            case 11 -> saveScalar(viewer, slot.mainPath(), "NONE");
            case 13 -> {
                if (slot.countPath() != null) {
                    saveScalar(viewer, slot.countPath(), String.valueOf(clamp(countValue(type) + d, 1, 100)));
                }
            }
            case 14 -> {
                if (slot == EffectSlot.HARVEST_SOUND) {
                    float v = clamp(Math.round((type.harvestSoundVolume() + d * 0.1f) * 10), 1, 20) / 10f;
                    saveScalar(viewer, slot.extraPath(), String.valueOf(v));
                } else if (slot == EffectSlot.RADIUS_RING) {
                    saveScalar(viewer, slot.extraPath(),
                            String.valueOf(clamp(type.radiusParticleInterval() + d, 1, 40)));
                }
            }
            case 15 -> {
                if (slot == EffectSlot.HARVEST_SOUND) {
                    float p = clamp(Math.round((type.harvestSoundPitch() + d * 0.1f) * 10), 5, 20) / 10f;
                    saveScalar(viewer, slot.extra2Path(), String.valueOf(p));
                }
            }
            default -> {
            }
        }
    }
}
