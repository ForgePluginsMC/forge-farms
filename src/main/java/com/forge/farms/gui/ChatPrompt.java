package com.forge.farms.gui;

import com.forge.farms.ForgeFarms;
import com.forge.farms.core.Text;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Captures a player's next chat message for in-game text input (used by
 * the admin editor for names, materials, prices, hologram lines...).
 * The message never reaches chat. Type "cancel" to abort.
 */
public final class ChatPrompt implements Listener {
    private final ForgeFarms plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    private record Pending(Consumer<String> onInput, Runnable onCancel) {
    }

    public ChatPrompt(ForgeFarms plugin) {
        this.plugin = plugin;
    }

    /** Ask for free text. The input runs on the main thread. */
    public void ask(Player player, String prompt, Consumer<String> onInput, @Nullable Runnable onCancel) {
        pending.put(player.getUniqueId(), new Pending(onInput, onCancel == null ? () -> {
        } : onCancel));
        player.closeInventory();
        player.sendMessage(Text.mm("<yellow>" + prompt + "</yellow>"));
        player.sendMessage(Text.mm("<gray>Type <white>cancel</white> to go back.</gray>"));
    }

    public void ask(Player player, String prompt, Consumer<String> onInput) {
        ask(player, prompt, onInput, null);
    }

    /**
     * Ask for a material name. Re-prompts on unknown input.
     */
    public void askMaterial(Player player, String prompt, Consumer<Material> onMaterial,
            @Nullable Runnable onCancel) {
        ask(player, prompt, input -> {
            Material m = Material.matchMaterial(input);
            if (m == null) {
                player.sendMessage(Text.mm("<red>Unknown material '" + input + "'. Try again.</red>"));
                askMaterial(player, prompt, onMaterial, onCancel);
                return;
            }
            onMaterial.accept(m);
        }, onCancel);
    }

    /**
     * Ask for a decimal number. Re-prompts on invalid input.
     */
    public void askDouble(Player player, String prompt, java.util.function.DoubleConsumer onNumber,
            @Nullable Runnable onCancel) {
        ask(player, prompt, input -> {
            double d;
            try {
                d = Double.parseDouble(input.trim());
            } catch (NumberFormatException e) {
                player.sendMessage(Text.mm("<red>'" + input + "' is not a number. Try again.</red>"));
                askDouble(player, prompt, onNumber, onCancel);
                return;
            }
            if (d < 0) {
                player.sendMessage(Text.mm("<red>Price can't be negative. Try again.</red>"));
                askDouble(player, prompt, onNumber, onCancel);
                return;
            }
            onNumber.accept(d);
        }, onCancel);
    }

    /**
     * Ask the player to hold an item: they type anything while holding it
     * in their main hand. Empty hand re-prompts.
     */
    public void askHeldItem(Player player, String prompt, Consumer<Material> onMaterial,
            @Nullable Runnable onCancel) {
        ask(player, prompt + " <gray>(hold the item now)</gray>", input -> {
            Material m = player.getInventory().getItemInMainHand().getType();
            if (m == Material.AIR) {
                player.sendMessage(Text.mm("<red>Hold an item in your main hand first.</red>"));
                askHeldItem(player, prompt, onMaterial, onCancel);
                return;
            }
            onMaterial.accept(m);
        }, onCancel);
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Pending p = pending.remove(event.getPlayer().getUniqueId());
        if (p == null) {
            return;
        }
        event.setCancelled(true);
        String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (input.equalsIgnoreCase("cancel")) {
                p.onCancel().run();
            } else {
                p.onInput().accept(input);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }
}
