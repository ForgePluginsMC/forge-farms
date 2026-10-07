package com.forge.farms.gui;

import com.forge.farms.ForgeFarms;
import com.forge.farms.core.Text;
import com.forge.farms.farm.Farm;
import com.forge.farms.members.FarmFlag;
import com.forge.farms.members.FarmRole;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * Member list. Left-click a member to cycle their role,
 * right-click to remove. Adding is done via /farm trust.
 */
public final class FarmMembersMenu extends Menu {
    private final Farm farm;
    private final List<UUID> order = new ArrayList<>();

    public FarmMembersMenu(ForgeFarms plugin, Farm farm) {
        super(plugin);
        this.farm = farm;
    }

    @Override
    public void build(Player viewer) {
        create(viewer, 54, "<dark_green><bold>Farm Members</bold></dark_green>");
        fill(filler());
        order.clear();
        order.add(farm.owner());
        for (UUID id : plugin.trust().getMembers(farm).keySet()) {
            if (!order.contains(id)) {
                order.add(id);
            }
        }
        int slot = 0;
        for (UUID id : order) {
            if (slot >= 45) {
                break;
            }
            FarmRole role = plugin.trust().getRole(farm, id);
            OfflinePlayer op = Bukkit.getOfflinePlayer(id);
            String name = op.getName() == null ? id.toString().substring(0, 8) : op.getName();
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Role: <white>" + (role == null ? "?" : role.name()) + "</white></gray>");
            if (role != null && role != FarmRole.OWNER) {
                lore.add("");
                lore.add("<yellow>Left-click: cycle role</yellow>");
                lore.add("<red>Right-click: remove</red>");
            } else {
                lore.add("<gray>Farm owner</gray>");
            }
            inventory.setItem(slot++, head(id, "<gold>" + name + "</gold>", lore));
        }
        inventory.setItem(49, button(Material.PAPER, "<yellow>Add members</yellow>",
                "<gray>Use <white>/farm trust <player> [role]</white></gray>",
                "<gray>Roles: GUEST, MEMBER, ADMIN</gray>"));
        navRow();
    }

    private ItemStack head(UUID id, String name, List<String> lore) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(id));
        meta.displayName(Text.mm(name));
        List<net.kyori.adventure.text.Component> lines = new ArrayList<>();
        for (String line : lore) {
            lines.add(Text.mm(line));
        }
        meta.lore(lines);
        skull.setItemMeta(meta);
        return skull;
    }

    @Override
    public void click(Player viewer, InventoryClickEvent event) {
        if (!isTopClick(event) || navClick(viewer, event)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= order.size()) {
            return;
        }
        if (!plugin.trust().can(viewer, farm, FarmFlag.CONFIGURE)) {
            viewer.sendMessage(Text.mm("<red>You can't manage members on this farm.</red>"));
            return;
        }
        UUID target = order.get(slot);
        if (target.equals(farm.owner())) {
            viewer.sendMessage(Text.mm("<red>You can't change the owner's role.</red>"));
            return;
        }
        if (event.getClick() == ClickType.RIGHT) {
            if (plugin.trust().removeMember(farm, target)) {
                viewer.sendMessage(Text.mm("<green>Member removed.</green>"));
            }
        } else {
            FarmRole current = plugin.trust().getRole(farm, target);
            if (current != null) {
                FarmRole next = current.next();
                plugin.trust().setRole(farm, target, next);
                viewer.sendMessage(Text.mm("<green>Role set to <white>" + next.name() + "</white>.</green>"));
            }
        }
        plugin.menus().refresh(viewer);
    }
}
