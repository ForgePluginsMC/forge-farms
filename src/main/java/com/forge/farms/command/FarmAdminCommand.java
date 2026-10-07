package com.forge.farms.command;

import com.forge.farms.ForgeFarms;
import com.forge.farms.api.ForgeFarmsAPI;
import com.forge.farms.config.FarmType;
import com.forge.farms.core.Text;
import com.forge.farms.db.Database;
import com.forge.farms.farm.Farm;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /farmadmin} — give, reload, migrate, backup, restore, limits, remove, list.
 */
public final class FarmAdminCommand implements CommandExecutor, TabCompleter {
    private final ForgeFarms plugin;

    public FarmAdminCommand(ForgeFarms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, String[] args) {
        if (!sender.hasPermission("forgefarms.admin")) {
            sender.sendMessage(Text.mm("<red>No permission.</red>"));
            return true;
        }
        if (args.length == 0) {
            help(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "give" -> give(sender, args);
            case "reload" -> reload(sender);
            case "migrate" -> migrate(sender, args);
            case "backup" -> backup(sender);
            case "restore" -> restore(sender, args);
            case "limits" -> limits(sender);
            case "remove" -> remove(sender, args);
            case "list" -> list(sender, args);
            case "help" -> help(sender);
            default -> sender.sendMessage(Text.mm("<red>Unknown subcommand. <gray>/farmadmin help</gray></red>"));
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Text.mm("<red>Usage: /farmadmin give <player> <type> [amount]</red>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Text.mm("<red>Player not online.</red>"));
            return;
        }
        FarmType type = plugin.config().getType(args[2]);
        if (type == null) {
            sender.sendMessage(Text.mm("<red>Unknown farm type.</red>"));
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
            } catch (NumberFormatException e) {
                sender.sendMessage(Text.mm("<red>Amount must be a number.</red>"));
                return;
            }
        }
        ItemStack item = ForgeFarmsAPI.createFarmItem(type.id(), amount);
        if (item == null) {
            sender.sendMessage(Text.mm("<red>Could not create that farm item.</red>"));
            return;
        }
        target.getInventory().addItem(item);
        sender.sendMessage(Text.mm("<green>Gave <white>" + amount + "x " + type.displayName()
                + "</white><green> to " + target.getName() + ".</green>"));
    }

    private void reload(CommandSender sender) {
        plugin.scheduler().async(() -> {
            plugin.config().reload();
            // Re-resolve storage sizes in case configs changed.
            for (Farm farm : plugin.farms().all()) {
                FarmType type = plugin.config().getType(farm.typeId());
                if (type != null) {
                    farm.setStorageSlots(type.slotsAt(farm.storageLevel()));
                    plugin.farms().restartTick(farm);
                }
            }
            sender.sendMessage(Text.mm("<green>ForgeFarms reloaded: <white>"
                    + plugin.config().typeIds().size() + "</white> farm types.</green>"));
        });
    }

    private void migrate(CommandSender sender, String[] args) {
        if (args.length < 2 || (!args[1].equalsIgnoreCase("sqlite") && !args[1].equalsIgnoreCase("mysql"))) {
            sender.sendMessage(Text.mm("<red>Usage: /farmadmin migrate <sqlite|mysql></red>"));
            return;
        }
        String target = args[1].toLowerCase(Locale.ROOT);
        if (target.equals(plugin.database().backendName())) {
            sender.sendMessage(Text.mm("<yellow>Already on " + target + ".</yellow>"));
            return;
        }
        sender.sendMessage(Text.mm("<gray>Migrating data to " + target + "…</gray>"));
        plugin.scheduler().async(() -> {
            Database dest = target.equals("mysql")
                    ? new com.forge.farms.db.MySQLDatabase(plugin, plugin.config())
                    : new com.forge.farms.db.SQLiteDatabase(plugin, plugin.config());
            try {
                dest.migrate();
                List<Farm> farms = plugin.database().loadFarms();
                for (Farm farm : farms) {
                    dest.saveFarm(farm);
                }
                Map<UUID, Map<UUID, String>> members = plugin.database().loadMembers();
                for (Map.Entry<UUID, Map<UUID, String>> e : members.entrySet()) {
                    dest.saveMembers(e.getKey(), e.getValue());
                }
                sender.sendMessage(Text.mm("<green>Migrated <white>" + farms.size()
                        + "</white> farms to " + target + ". Set <white>database.type: " + target
                        + "</white> in config.yml, then <white>/farmadmin reload</white>.</green>"));
            } catch (Exception e) {
                sender.sendMessage(Text.mm("<red>Migration failed: " + e.getMessage() + "</red>"));
            } finally {
                dest.close();
            }
        });
    }

    private void backup(CommandSender sender) {
        plugin.scheduler().async(() -> {
            String ext = plugin.database().backendName().equals("mysql") ? "sql" : "db";
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            File dest = new File(plugin.getDataFolder(), "backups/forgefarms-" + stamp + "." + ext);
            try {
                plugin.database().backup(dest);
                sender.sendMessage(Text.mm("<green>Backup written to <white>backups/"
                        + dest.getName() + "</white>.</green>"));
            } catch (Exception e) {
                sender.sendMessage(Text.mm("<red>Backup failed: " + e.getMessage() + "</red>"));
            }
        });
    }

    private void restore(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Text.mm("<red>Usage: /farmadmin restore <file></red>"));
            File dir = new File(plugin.getDataFolder(), "backups");
            File[] files = dir.listFiles((d, n) -> n.endsWith(".db") || n.endsWith(".sql"));
            if (files != null && files.length > 0) {
                sender.sendMessage(Text.mm("<gray>Available backups:</gray>"));
                for (File f : files) {
                    sender.sendMessage(Text.mm("<gray>- <white>" + f.getName() + "</white></gray>"));
                }
            }
            return;
        }
        File src = new File(plugin.getDataFolder(), "backups/" + args[1]);
        if (!src.isFile()) {
            sender.sendMessage(Text.mm("<red>Backup not found: " + args[1] + "</red>"));
            return;
        }
        sender.sendMessage(Text.mm("<gray>Restoring <white>" + args[1] + "</white>…</gray>"));
        plugin.scheduler().async(() -> {
            try {
                plugin.database().restore(src);
                plugin.farms().reloadFromDatabase();
                sender.sendMessage(Text.mm("<green>Restore complete — farms reloaded, no restart needed.</green>"));
            } catch (Exception e) {
                sender.sendMessage(Text.mm("<red>Restore failed: " + e.getMessage() + "</red>"));
            }
        });
    }

    private void limits(CommandSender sender) {
        sender.sendMessage(Text.mm("<dark_green><bold>ForgeFarms limits</bold></dark_green>"));
        sender.sendMessage(Text.mm("<gray>Database: <white>" + plugin.database().backendName() + "</white></gray>"));
        sender.sendMessage(Text.mm("<gray>Max farms per player: <white>" + plugin.config().maxFarmsPerPlayer()
                + "</white></gray>"));
        sender.sendMessage(Text.mm("<gray>Max members per farm: <white>" + plugin.config().maxMembersPerFarm()
                + "</white></gray>"));
        sender.sendMessage(Text.mm("<gray>Loaded farms: <white>" + plugin.farms().count() + "</white></gray>"));
        sender.sendMessage(Text.mm("<gray>Farm types: <white>" + String.join(", ", plugin.config().typeIds())
                + "</white></gray>"));
        sender.sendMessage(Text.mm("<gray>Economy: <white>"
                + (plugin.output().economy().isAvailable() ? "connected" : "not found") + "</white></gray>"));
    }

    private void remove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Text.mm("<red>Usage: /farmadmin remove <id></red>"));
            return;
        }
        Farm farm = null;
        for (Farm f : plugin.farms().all()) {
            if (f.id().toString().startsWith(args[1].toLowerCase(Locale.ROOT))) {
                farm = f;
                break;
            }
        }
        if (farm == null) {
            sender.sendMessage(Text.mm("<red>No farm with that id.</red>"));
            return;
        }
        plugin.farms().removeFarm(farm.id());
        sender.sendMessage(Text.mm("<green>Farm removed.</green>"));
    }

    private void list(CommandSender sender, String[] args) {
        List<Farm> farms = plugin.farms().all();
        if (args.length >= 2) {
            UUID owner = Bukkit.getOfflinePlayer(args[1]).getUniqueId();
            farms = farms.stream().filter(f -> f.owner().equals(owner)).toList();
        }
        sender.sendMessage(Text.mm("<dark_green><bold>Farms (" + farms.size() + ")</bold></dark_green>"));
        for (Farm farm : farms) {
            String ownerName = Bukkit.getOfflinePlayer(farm.owner()).getName();
            sender.sendMessage(Text.mm("<gray>- <white>" + farm.id().toString().substring(0, 8) + "</white> "
                    + farm.typeId() + " owned by " + (ownerName == null ? "?" : ownerName)
                    + " at " + farm.x() + "," + farm.y() + "," + farm.z() + "</gray>"));
        }
    }

    private void help(CommandSender sender) {
        sender.sendMessage(Text.mm("<dark_green><bold>ForgeFarms admin</bold></dark_green>"));
        sender.sendMessage(Text.mm("<white>/farmadmin give <player> <type> [amount]</white>"));
        sender.sendMessage(Text.mm("<white>/farmadmin reload</white> <gray>— reload configs</gray>"));
        sender.sendMessage(Text.mm("<white>/farmadmin migrate <sqlite|mysql></white> <gray>— copy data</gray>"));
        sender.sendMessage(Text.mm("<white>/farmadmin backup</white> <gray>— snapshot the database</gray>"));
        sender.sendMessage(Text.mm("<white>/farmadmin restore <file></white> <gray>— restore, no restart</gray>"));
        sender.sendMessage(Text.mm("<white>/farmadmin limits</white> <gray>— show config + status</gray>"));
        sender.sendMessage(Text.mm("<white>/farmadmin remove <id></white>"));
        sender.sendMessage(Text.mm("<white>/farmadmin list [player]</white>"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("give", "reload", "migrate", "backup", "restore", "limits", "remove",
                    "list", "help")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("migrate")) {
            for (String s : List.of("sqlite", "mysql")) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(p.getName());
                }
            }
            return out;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            for (String id : plugin.config().typeIds()) {
                if (id.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(id);
                }
            }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("restore")) {
            File dir = new File(plugin.getDataFolder(), "backups");
            File[] files = dir.listFiles((d, n) -> n.endsWith(".db") || n.endsWith(".sql"));
            if (files != null) {
                for (File f : files) {
                    if (f.getName().startsWith(args[1])) {
                        out.add(f.getName());
                    }
                }
            }
        }
        return out;
    }
}
