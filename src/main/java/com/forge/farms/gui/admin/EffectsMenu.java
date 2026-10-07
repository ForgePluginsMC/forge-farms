package com.forge.farms.gui.admin;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.FarmType;
import com.forge.farms.core.Text;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Admin: the five effect slots of a farm type. */
public final class EffectsMenu extends TypeMenu {
    public EffectsMenu(ForgeFarms plugin, String typeId) {
        super(plugin, typeId);
    }

    @Override
    public void build(Player viewer) {
        FarmType type = type();
        create(viewer, 36, "<dark_red><bold>Effects</bold></dark_red> <gray>· " + typeId + "</gray>");
        fill(filler());
        if (type == null) {
            navRow();
            return;
        }
        int slot = 10;
        for (EffectSlot es : EffectSlot.values()) {
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Current: <white>" + currentValue(type, es) + "</white></gray>");
            lore.add("");
            lore.add("<yellow>Click to edit.</yellow>");
            inventory.setItem(slot++, button(es.icon(), "<gold>" + es.title() + "</gold>", lore));
        }
        navRow();
    }

    private String currentValue(FarmType type, EffectSlot es) {
        return switch (es) {
            case HARVEST_PARTICLE -> type.harvestParticle() == null ? "OFF"
                    : type.harvestParticle().name() + " x" + type.harvestParticleCount();
            case HARVEST_SOUND -> type.harvestSound() == null ? "OFF" : type.harvestSoundName();
            case GROWTH_PARTICLE -> type.growthParticle() == null ? "OFF"
                    : type.growthParticle().name() + " x" + type.growthParticleCount();
            case WORKING_AURA -> type.workingParticle() == null ? "OFF"
                    : type.workingParticle().name() + " x" + type.workingParticleCount();
            case RADIUS_RING -> type.radiusParticle() == null ? "OFF"
                    : type.radiusParticle().name() + " x" + type.radiusParticleCount();
        };
    }

    @Override
    public void click(Player viewer, InventoryClickEvent event) {
        if (!isTopClick(event) || navClick(viewer, event)) {
            return;
        }
        int slot = event.getRawSlot() - 10;
        EffectSlot[] all = EffectSlot.values();
        if (slot < 0 || slot >= all.length) {
            return;
        }
        plugin.menus().open(viewer, new EffectSlotMenu(plugin, typeId, all[slot]).withParent(this));
    }
}
