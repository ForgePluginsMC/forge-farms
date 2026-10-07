package com.forge.farms.gui.admin;

import com.forge.farms.ForgeFarms;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Admin: pick a particle for an effect slot. Click selects, writes the type
 * file, and reloads — the farm shows it live.
 */
public final class ParticlePickerMenu extends TypeMenu {
    private static final int PER_PAGE = 45;
    private final EffectSlot slot;
    private final List<Particle> particles = List.of(Particle.values());
    private final int page;

    public ParticlePickerMenu(ForgeFarms plugin, String typeId, EffectSlot slot) {
        this(plugin, typeId, slot, 0);
    }

    private ParticlePickerMenu(ForgeFarms plugin, String typeId, EffectSlot slot, int page) {
        super(plugin, typeId);
        this.slot = slot;
        this.page = page;
    }

    @Override
    public void build(Player viewer) {
        int pages = Math.max(1, (particles.size() + PER_PAGE - 1) / PER_PAGE);
        int p = Math.min(page, pages - 1);
        create(viewer, 54, "<dark_red><bold>Pick particle</bold></dark_red> <gray>(" + (p + 1) + "/" + pages + ")</gray>");
        fill(filler());
        int start = p * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < particles.size(); i++) {
            Particle particle = particles.get(start + i);
            List<String> lore = new ArrayList<>();
            lore.add("<gray>" + particle.name() + "</gray>");
            lore.add("");
            lore.add("<yellow>Click to select.</yellow>");
            inventory.setItem(i, button(Material.FIREWORK_STAR, "<gold>" + pretty(particle.name()) + "</gold>", lore));
        }
        if (p > 0) {
            inventory.setItem(48, button(Material.ARROW, "<yellow>Previous page</yellow>"));
        }
        if (p < pages - 1) {
            inventory.setItem(50, button(Material.ARROW, "<yellow>Next page</yellow>"));
        }
        navRow();
    }

    @Override
    public void click(Player viewer, InventoryClickEvent event) {
        if (!isTopClick(event)) {
            return;
        }
        int raw = event.getRawSlot();
        // Page buttons live on the nav row; handle before navClick so back/close still work.
        int pages = Math.max(1, (particles.size() + PER_PAGE - 1) / PER_PAGE);
        if (raw == 48 && page > 0) {
            plugin.menus().open(viewer, new ParticlePickerMenu(plugin, typeId, slot, page - 1).withParent(parent()));
            return;
        }
        if (raw == 50 && page < pages - 1) {
            plugin.menus().open(viewer, new ParticlePickerMenu(plugin, typeId, slot, page + 1).withParent(parent()));
            return;
        }
        if (navClick(viewer, event)) {
            return;
        }
        int idx = page * PER_PAGE + raw;
        if (raw < 0 || raw >= PER_PAGE || idx >= particles.size()) {
            return;
        }
        Particle particle = particles.get(idx);
        if (saveScalar(viewer, slot.mainPath(), particle.name())) {
            viewer.sendMessage(com.forge.farms.core.Text.mm("<green>Particle set to <white>"
                    + particle.name() + "</white> — watch the farm.</green>"));
            plugin.menus().open(viewer, new EffectSlotMenu(plugin, typeId, slot).withParent(parent()));
        }
    }

    private static String pretty(String name) {
        String s = name.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
