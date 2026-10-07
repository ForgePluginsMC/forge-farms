package com.forge.farms.command;

import com.forge.farms.ForgeFarms;
import com.forge.farms.api.ForgeFarmsAPI;
import com.forge.farms.config.FarmType;
import com.forge.farms.core.Text;
import com.forge.farms.farm.Farm;
import com.forge.farms.gui.FarmMainMenu;
import com.forge.farms.members.FarmFlag;
import com.forge.farms.members.FarmRole;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /farm} — player commands: get, list, menu, trust, untrust, tp, help.
 */
public final class FarmCommand implements CommandExecutor, TabCompleter {
    private final ForgeFarms plugin;

    public FarmCommand(ForgeFarms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "get" -> get(sender, args);
            case "list" -> list(sender);
            case "menu" -> menu(sender, args);
            case "trust" -> trust(sender, args);
            case "untrust" -> untrust(sender, args);
            case "tp" -> tp(sender, args);
            case "help" -> help(sender);
            default -> sender.sendMessage(Text.mm("<red>Unknown subcommand. <gray>/farm help</gray></red>"));
        }
        return true;
    }

    private void get(CommandSender sender, String[] args) {
        if (!sender.hasPermission("forgefarms.command.get")) {
            sender.sendMessage(Text.mm("<red>No permission.</red>"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.mm("<red>Players only.</red>"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Text.mm("<red>Usage: /farm get <type> [amount]</red>"));
            return;
        }
        FarmType type = plugin.config().getType(args[1]);
        if (type == null) {
            player.sendMessage(Text.mm("<red>Unknown farm type. Available: <white>"
                    + String.join(", ", plugin.config().typeIds()) + "</white></red>"));
            return;
        }
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[2])));
            } catch (NumberFormatException e) {
                player.sendMessage(Text.mm("<red>Amount must be a number.</red>"));
                return;
            }
        }
        ItemStack item = ForgeFarmsAPI.createFarmItem(type.id(), amount);
        if (item == null) {
            player.sendMessage(Text.mm("<red>Could not create that farm item.</red>"));
            return;
        }
        player.getInventory().addItem(item);
        player.sendMessage(Text.mm("<green>Gave you <white>" + amount + "x " + type.displayName()
                + "</white><green>.</green>"));
    }

    private void list(CommandSender sender) {
        if (!sender.hasPermission("forgefarms.command.list")) {
            sender.sendMessage(Text.mm("<red>No permission.</red>"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.mm("<red>Players only.</red>"));
            return;
        }
        List<Farm> farms = plugin.farms().getByOwner(player.getUniqueId());
        if (farms.isEmpty()) {
            player.sendMessage(Text.mm("<gray>You don't own any farms yet. <white>/farm get wheat</white></gray>"));
            return;
        }
        player.sendMessage(Text.mm("<dark_green><bold>Your farms (" + farms.size() + ")</bold></dark_green>"));
        for (Farm farm : farms) {
            FarmType type = plugin.config().getType(farm.typeId());
            String name = type == null ? farm.typeId() : Text.plain(type.displayName());
            String id = farm.id().toString().substring(0, 8);
            player.sendMessage(Text.mm("<gray>- <white>" + name + "</white> <dark_gray>[" + id + "]</dark_gray> "
                    + "<gray>at " + farm.x() + ", " + farm.y() + ", " + farm.z()
                    + " | fuel " + plugin.fuel().formatDuration(farm.fuelTicks()) + "</gray>"));
        }
    }

    private void menu(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.mm("<red>Players only.</red>"));
            return;
        }
        Farm farm = null;
        if (args.length >= 2) {
            farm = byIdPrefix(args[1]);
            if (farm == null) {
                player.sendMessage(Text.mm("<red>No farm with that id.</red>"));
                return;
            }
        } else {
            farm = nearest(player, true);
            if (farm == null) {
                player.sendMessage(Text.mm("<red>No farm nearby you can use. <gray>/farm menu <id></gray></red>"));
                return;
            }
        }
        if (!plugin.trust().can(player, farm, FarmFlag.INTERACT)) {
            player.sendMessage(Text.mm("<red>You can't use this farm.</red>"));
            return;
        }
        plugin.menus().open(player, new FarmMainMenu(plugin, farm));
    }

    private void trust(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.mm("<red>Players only.</red>"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Text.mm("<red>Usage: /farm trust <player> [role]</red>"));
            return;
        }
        Farm farm = nearest(player, false);
        if (farm == null) {
            player.sendMessage(Text.mm("<red>Stand near a farm you can configure.</red>"));
            return;
        }
        if (!plugin.trust().can(player, farm, FarmFlag.CONFIGURE)) {
            player.sendMessage(Text.mm("<red>You can't manage members on this farm.</red>"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(Text.mm("<red>Player not found.</red>"));
            return;
        }
        FarmRole role = FarmRole.MEMBER;
        if (args.length >= 3) {
            try {
                role = FarmRole.valueOf(args[2].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                player.sendMessage(Text.mm("<red>Role must be GUEST, MEMBER, or ADMIN.</red>"));
                return;
            }
            if (role == FarmRole.OWNER) {
                player.sendMessage(Text.mm("<red>You can't grant OWNER.</red>"));
                return;
            }
        }
        if (plugin.trust().addMember(farm, target.getUniqueId(), role)) {
            player.sendMessage(Text.mm("<green>Added <white>" + target.getName() + "</white> as <white>"
                    + role.name() + "</white>.</green>"));
        } else {
            player.sendMessage(Text.mm("<red>Couldn't add them (member cap reached?).</red>"));
        }
    }

    private void untrust(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.mm("<red>Players only.</red>"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Text.mm("<red>Usage: /farm untrust <player></red>"));
            return;
        }
        Farm farm = nearest(player, false);
        if (farm == null) {
            player.sendMessage(Text.mm("<red>Stand near a farm you can configure.</red>"));
            return;
        }
        if (!plugin.trust().can(player, farm, FarmFlag.CONFIGURE)) {
            player.sendMessage(Text.mm("<red>You can't manage members on this farm.</red>"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (plugin.trust().removeMember(farm, target.getUniqueId())) {
            player.sendMessage(Text.mm("<green>Removed <white>" + target.getName() + "</white>.</green>"));
        } else {
            player.sendMessage(Text.mm("<red>They weren't a member.</red>"));
        }
    }

    private void tp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("forgefarms.teleport")) {
            sender.sendMessage(Text.mm("<red>No permission.</red>"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.mm("<red>Players only.</red>"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Text.mm("<red>Usage: /farm tp <id></red>"));
            return;
        }
        Farm farm = byIdPrefix(args[1]);
        if (farm == null) {
            player.sendMessage(Text.mm("<red>No farm with that id.</red>"));
            return;
        }
        if (!farm.owner().equals(player.getUniqueId())
                && !plugin.trust().can(player, farm, FarmFlag.INTERACT)
                && !player.hasPermission("forgefarms.bypass.protection")) {
            player.sendMessage(Text.mm("<red>You can't visit this farm.</red>"));
            return;
        }
        Location loc = plugin.farms().coreLocation(farm);
        if (loc == null) {
            player.sendMessage(Text.mm("<red>That farm's world isn't loaded.</red>"));
            return;
        }
        loc.add(0, 1, 0);
        final Location dest = loc;
        plugin.scheduler().region(dest, () -> player.teleport(dest));
    }

    private void help(CommandSender sender) {
        sender.sendMessage(Text.mm("<dark_green><bold>ForgeFarms</bold></dark_green> <gray>— automated farms</gray>"));
        sender.sendMessage(Text.mm("<white>/farm get <type> [amount]</white> <gray>— get a farm item</gray>"));
        sender.sendMessage(Text.mm("<white>/farm list</white> <gray>— your farms</gray>"));
        sender.sendMessage(Text.mm("<white>/farm menu [id]</white> <gray>— manage a farm</gray>"));
        sender.sendMessage(Text.mm("<white>/farm trust <player> [role]</white> <gray>— add a member</gray>"));
        sender.sendMessage(Text.mm("<white>/farm untrust <player></white> <gray>— remove a member</gray>"));
        sender.sendMessage(Text.mm("<white>/farm tp <id></white> <gray>— visit a farm</gray>"));
        sender.sendMessage(Text.mm("<gray>Place a farm item, fuel it with coal, right-click to manage.</gray>"));
    }

    private Farm byIdPrefix(String prefix) {
        for (Farm farm : plugin.farms().all()) {
            if (farm.id().toString().startsWith(prefix.toLowerCase(Locale.ROOT))) {
                return farm;
            }
        }
        return null;
    }

    /** Nearest farm within 64 blocks the player owns or can configure (configureOnly) / interact. */
    private Farm nearest(Player player, boolean interactOnly) {
        Farm best = null;
        double bestDist = 64 * 64;
        for (Farm farm : plugin.farms().all()) {
            Location core = plugin.farms().coreLocation(farm);
            if (core == null || !core.getWorld().equals(player.getWorld())) {
                continue;
            }
            boolean ok = interactOnly ? plugin.trust().can(player, farm, FarmFlag.INTERACT)
                    : plugin.trust().can(player, farm, FarmFlag.CONFIGURE);
            if (!ok) {
                continue;
            }
            double d = core.distanceSquared(player.getLocation());
            if (d < bestDist) {
                bestDist = d;
                best = farm;
            }
        }
        return best;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("get", "list", "menu", "trust", "untrust", "tp", "help")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("get")) {
            for (String id : plugin.config().typeIds()) {
                if (id.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(id);
                }
            }
            return out;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("trust") || args[0].equalsIgnoreCase("untrust"))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(p.getName());
                }
            }
            return out;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("menu") || args[0].equalsIgnoreCase("tp"))
                && sender instanceof Player player) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (Farm farm : plugin.farms().getByOwner(player.getUniqueId())) {
                String id = farm.id().toString().substring(0, 8);
                if (id.startsWith(prefix)) {
                    out.add(id);
                }
            }
            return out;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("trust")) {
            for (FarmRole r : List.of(FarmRole.GUEST, FarmRole.MEMBER, FarmRole.ADMIN)) {
                if (r.name().startsWith(args[2].toUpperCase(Locale.ROOT))) {
                    out.add(r.name());
                }
            }
        }
        return out;
    }
}
