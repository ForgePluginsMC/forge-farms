package com.forge.farms.gui.admin;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.HarvestBehavior;
import com.forge.farms.config.TypeConfigWriter;
import com.forge.farms.core.Text;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Admin: pick the harvest behavior for a newly added crop block. */
public final class HarvestBehaviorMenu extends TypeMenu {
    private final Material material;

    public HarvestBehaviorMenu(ForgeFarms plugin, String typeId, Material material) {
        super(plugin, typeId);
        this.material = material;
    }

    @Override
    public void build(Player viewer) {
        create(viewer, 27, "<dark_red><bold>Behavior</bold></dark_red> <gray>· " + material.name() + "</gray>");
        fill(filler());
        behaviorButton(10, HarvestBehavior.AGEABLE_CROP, Material.WHEAT,
                "Replanted at age 0 after harvest. Wheat, carrots, nether wart…");
        behaviorButton(12, HarvestBehavior.STEM_FRUIT, Material.MELON,
                "Fruit block breaks; the stem stays. Melon, pumpkin.");
        behaviorButton(14, HarvestBehavior.STALK, Material.SUGAR_CANE,
                "Everything above the base breaks; base regrows. Cane, cactus…");
        navRow();
    }

    private void behaviorButton(int slot, HarvestBehavior behavior, Material icon, String hint) {
        inventory.setItem(slot, button(icon, "<gold>" + pretty(behavior.name()) + "</gold>",
                "<gray>" + hint + "</gray>", "", "<yellow>Click to add.</yellow>"));
    }

    @Override
    public void click(Player viewer, InventoryClickEvent event) {
        if (!isTopClick(event) || navClick(viewer, event)) {
            return;
        }
        HarvestBehavior behavior = switch (event.getRawSlot()) {
            case 10 -> HarvestBehavior.AGEABLE_CROP;
            case 12 -> HarvestBehavior.STEM_FRUIT;
            case 14 -> HarvestBehavior.STALK;
            default -> null;
        };
        if (behavior == null) {
            return;
        }
        File f = typeFile();
        if (f == null) {
            return;
        }
        try {
            String key = material.name().toLowerCase(java.util.Locale.ROOT);
            if (TypeConfigWriter.addHarvestable(f, key, material.name(), behavior.name()) && reload(viewer)) {
                viewer.sendMessage(Text.mm("<green>Added <white>" + HarvestMenu.pretty(material)
                        + "</white> (" + pretty(behavior.name()) + ").</green>"));
                plugin.menus().open(viewer, new HarvestMenu(plugin, typeId).withParent(parent()));
            }
        } catch (IOException e) {
            viewer.sendMessage(Text.mm("<red>Could not write the type file.</red>"));
        }
    }

    private static String pretty(String name) {
        String s = name.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
