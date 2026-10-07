package com.forge.farms.gui.admin;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.FarmType;
import com.forge.farms.core.Text;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Admin: pick a sound for the harvest-sound slot. Curated list; left-click
 * previews, right-click selects and writes the type file. Sound names are
 * Bukkit-style (BLOCK_CROP_BREAK); resolution goes through the registry —
 * {@code Sound.valueOf} is deprecated for removal in 26.3.
 */
public final class SoundPickerMenu extends TypeMenu {
    private static final String[] CURATED = {
        "BLOCK_CROP_BREAK", "BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES", "BLOCK_CAVE_VINES_PICK_BERRIES",
        "BLOCK_WOOD_BREAK", "BLOCK_STONE_BREAK", "BLOCK_GRASS_BREAK", "BLOCK_GRAVEL_BREAK",
        "BLOCK_COMPOSTER_READY", "BLOCK_COMPOSTER_FILL_SUCCESS",
        "ENTITY_PLAYER_LEVELUP", "ENTITY_EXPERIENCE_ORB_PICKUP", "ENTITY_ITEM_PICKUP",
        "ENTITY_VILLAGER_YES", "ENTITY_VILLAGER_NO", "ENTITY_VILLAGER_TRADE",
        "BLOCK_CHEST_OPEN", "BLOCK_CHEST_CLOSE", "BLOCK_BARREL_OPEN",
        "BLOCK_NOTE_BLOCK_PLING", "BLOCK_NOTE_BLOCK_BASS", "BLOCK_NOTE_BLOCK_BELL",
        "BLOCK_NOTE_BLOCK_CHIME", "BLOCK_NOTE_BLOCK_FLUTE", "BLOCK_NOTE_BLOCK_GUITAR",
        "BLOCK_NOTE_BLOCK_XYLOPHONE", "BLOCK_NOTE_BLOCK_BIT",
        "UI_BUTTON_CLICK", "UI_TOAST_CHALLENGE_COMPLETE",
        "BLOCK_LAVA_POP", "BLOCK_BUBBLE_COLUMN_WHIRLPOOL_AMBIENT",
        "ENTITY_CHICKEN_EGG", "ENTITY_ARROW_HIT_PLAYER",
        "BLOCK_ANVIL_USE", "BLOCK_ANVIL_LAND",
        "ENTITY_FIREWORK_ROCKET_LAUNCH", "ENTITY_FIREWORK_ROCKET_TWINKLE",
        "BLOCK_BEACON_ACTIVATE", "BLOCK_BEACON_POWER_SELECT",
        "ENTITY_ENDER_DRAGON_GROWL", "ENTITY_WITHER_SPAWN",
        "BLOCK_PORTAL_TRAVEL", "BLOCK_ENCHANTMENT_TABLE_USE",
        "ITEM_BOOK_PAGE_TURN", "ITEM_BOTTLE_FILL",
    };

    private record Entry(String bukkitName, Sound sound) {
    }

    private final EffectSlot slot;
    private final List<Entry> sounds = new ArrayList<>();

    public SoundPickerMenu(ForgeFarms plugin, String typeId, EffectSlot slot) {
        super(plugin, typeId);
        this.slot = slot;
        for (String name : CURATED) {
            Sound sound = FarmType.soundByBukkitName(name);
            if (sound != null) {
                sounds.add(new Entry(name, sound));
            }
            // Unknown in this version — skip it.
        }
    }

    @Override
    public void build(Player viewer) {
        create(viewer, 54, "<dark_red><bold>Pick sound</bold></dark_red> <gray>· left previews</gray>");
        fill(filler());
        for (int i = 0; i < sounds.size() && i < 45; i++) {
            Entry entry = sounds.get(i);
            List<String> lore = new ArrayList<>();
            lore.add("<gray>" + entry.bukkitName() + "</gray>");
            lore.add("");
            lore.add("<yellow>Left-click: preview.</yellow>");
            lore.add("<yellow>Right-click: select.</yellow>");
            inventory.setItem(i, button(Material.JUKEBOX, "<gold>" + pretty(entry.bukkitName()) + "</gold>", lore));
        }
        navRow();
    }

    @Override
    public void click(Player viewer, InventoryClickEvent event) {
        if (!isTopClick(event) || navClick(viewer, event)) {
            return;
        }
        int raw = event.getRawSlot();
        if (raw < 0 || raw >= sounds.size()) {
            return;
        }
        Entry entry = sounds.get(raw);
        if (event.isRightClick()) {
            if (saveScalar(viewer, slot.mainPath(), entry.bukkitName())) {
                viewer.sendMessage(Text.mm("<green>Sound set to <white>" + entry.bukkitName() + "</white>.</green>"));
                plugin.menus().open(viewer, new EffectSlotMenu(plugin, typeId, slot).withParent(parent()));
            }
        } else {
            viewer.playSound(viewer.getLocation(), entry.sound(), 1.0f, 1.0f);
        }
    }

    private static String pretty(String name) {
        String s = name.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
