package com.forge.farms.output;

import com.forge.farms.ForgeFarms;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Vault economy access via reflection. Vault ships no compile-time
 * dependency this way: when Vault (and an economy plugin) is present the
 * bridge lights up, otherwise auto-sell and money upgrades cleanly
 * disable themselves.
 */
public final class EconomyBridge {
    private final ForgeFarms plugin;
    private @Nullable Object economy;
    private @Nullable Method depositPlayer;
    private @Nullable Method withdrawPlayer;
    private @Nullable Method getBalance;
    private boolean available;

    public EconomyBridge(ForgeFarms plugin) {
        this.plugin = plugin;
        try {
            Class<?> clazz = Class.forName("net.milkbowl.vault.economy.Economy");
            var registration = Bukkit.getServicesManager().getRegistration(clazz);
            if (registration != null) {
                economy = registration.getProvider();
                depositPlayer = clazz.getMethod("depositPlayer", OfflinePlayer.class, double.class);
                withdrawPlayer = clazz.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
                getBalance = clazz.getMethod("getBalance", OfflinePlayer.class);
                available = economy != null;
            }
        } catch (ReflectiveOperationException | LinkageError e) {
            available = false;
        }
        plugin.getLogger().info(available ? "Vault economy hooked." : "No Vault economy found; money features disabled.");
    }

    public boolean isAvailable() {
        return available && economy != null;
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (!isAvailable() || depositPlayer == null) {
            return false;
        }
        try {
            Object response = depositPlayer.invoke(economy, player, amount);
            return transactionOk(response);
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().warning("Economy deposit failed: " + e.getMessage());
            return false;
        }
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!isAvailable() || withdrawPlayer == null) {
            return false;
        }
        try {
            Object response = withdrawPlayer.invoke(economy, player, amount);
            return transactionOk(response);
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().warning("Economy withdrawal failed: " + e.getMessage());
            return false;
        }
    }

    public double balance(OfflinePlayer player) {
        if (!isAvailable() || getBalance == null) {
            return 0;
        }
        try {
            Object result = getBalance.invoke(economy, player);
            return result instanceof Number n ? n.doubleValue() : 0;
        } catch (ReflectiveOperationException e) {
            return 0;
        }
    }

    private boolean transactionOk(@Nullable Object response) {
        if (response == null) {
            return false;
        }
        try {
            Method ok = response.getClass().getMethod("transactionSuccess");
            Object result = ok.invoke(response);
            return result instanceof Boolean b && b;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
