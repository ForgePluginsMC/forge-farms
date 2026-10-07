package com.forge.farms;

import com.forge.farms.api.ForgeFarmsAPI;
import com.forge.farms.command.FarmAdminCommand;
import com.forge.farms.command.FarmCommand;
import com.forge.farms.config.ConfigManager;
import com.forge.farms.core.Keys;
import com.forge.farms.core.Scheduler;
import com.forge.farms.db.Database;
import com.forge.farms.farm.FarmManager;
import com.forge.farms.fuel.FuelManager;
import com.forge.farms.growth.GrowthEngine;
import com.forge.farms.gui.ChatPrompt;
import com.forge.farms.gui.MenuManager;
import com.forge.farms.hologram.HologramManager;
import com.forge.farms.listener.FarmListener;
import com.forge.farms.members.TrustManager;
import com.forge.farms.output.OutputPipeline;
import com.forge.farms.shop.ShopManager;
import com.forge.farms.upgrade.UpgradeManager;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ForgeFarms — automated player-owned farms with trust, hoppers, and
 * holograms built in. Original implementation, no paid dependencies.
 */
public final class ForgeFarms extends JavaPlugin {
    private static ForgeFarms instance;

    private Scheduler scheduler;
    private ConfigManager configManager;
    private Database database;
    private FarmManager farmManager;
    private GrowthEngine growthEngine;
    private FuelManager fuelManager;
    private UpgradeManager upgradeManager;
    private TrustManager trustManager;
    private HologramManager hologramManager;
    private OutputPipeline outputPipeline;
    private MenuManager menuManager;
    private ShopManager shopManager;
    private ChatPrompt chatPrompt;

    /** Plugin instance for static access (keys, API). */
    public static ForgeFarms getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        try {
            Keys.init(this);
            scheduler = new Scheduler(this);
            configManager = new ConfigManager(this);
            configManager.load();

            database = Database.open(this, configManager);
            database.migrate();

            farmManager = new FarmManager(this);
            growthEngine = new GrowthEngine(this);
            fuelManager = new FuelManager(this);
            upgradeManager = new UpgradeManager(this);
            trustManager = new TrustManager(this);
            hologramManager = new HologramManager(this);
            outputPipeline = new OutputPipeline(this);
            menuManager = new MenuManager(this);
            shopManager = new ShopManager(this);
            chatPrompt = new ChatPrompt(this);

            // Load persisted farms, then start their ticks and holograms.
            scheduler.async(() -> {
                try {
                    farmManager.loadAll();
                    scheduler.globalAtFixedRate(t -> farmManager.tickLoaded(), 20L, 20L);
                    getLogger().info("Loaded " + farmManager.count() + " farms.");
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Failed to load farms", e);
                }
            });

            getServer().getPluginManager().registerEvents(new FarmListener(this), this);
            getServer().getPluginManager().registerEvents(menuManager, this);
            getServer().getPluginManager().registerEvents(chatPrompt, this);

            var farmCmd = new FarmCommand(this);
            var farmPluginCmd = getCommand("farm");
            if (farmPluginCmd != null) {
                farmPluginCmd.setExecutor(farmCmd);
                farmPluginCmd.setTabCompleter(farmCmd);
            }
            var adminCmd = new FarmAdminCommand(this);
            var adminPluginCmd = getCommand("farmadmin");
            if (adminPluginCmd != null) {
                adminPluginCmd.setExecutor(adminCmd);
                adminPluginCmd.setTabCompleter(adminCmd);
            }

            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new com.forge.farms.hook.PlaceholderHook(this).register();
                getLogger().info("PlaceholderAPI hooked.");
            }

            ForgeFarmsAPI.init(this);
            getLogger().info("ForgeFarms enabled.");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable ForgeFarms", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (scheduler != null) {
                scheduler.cancelAll();
            }
            if (hologramManager != null) {
                hologramManager.removeAll();
            }
            if (farmManager != null) {
                farmManager.saveAll();
            }
            if (database != null) {
                database.close();
            }
            ForgeFarmsAPI.shutdown();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during disable", e);
        }
        instance = null;
    }

    public Scheduler scheduler() {
        return scheduler;
    }

    public ConfigManager config() {
        return configManager;
    }

    public Database database() {
        return database;
    }

    public FarmManager farms() {
        return farmManager;
    }

    public GrowthEngine growth() {
        return growthEngine;
    }

    public FuelManager fuel() {
        return fuelManager;
    }

    public UpgradeManager upgrades() {
        return upgradeManager;
    }

    public TrustManager trust() {
        return trustManager;
    }

    public HologramManager holograms() {
        return hologramManager;
    }

    public OutputPipeline output() {
        return outputPipeline;
    }

    public MenuManager menus() {
        return menuManager;
    }

    public ShopManager shop() {
        return shopManager;
    }

    public ChatPrompt prompts() {
        return chatPrompt;
    }
}
