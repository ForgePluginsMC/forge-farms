package com.forge.farms.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/** MiniMessage helpers shared across the plugin. */
public final class Text {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Text() {
    }

    /** Parse MiniMessage markup. */
    public static Component mm(String input) {
        return MM.deserialize(input == null ? "" : input);
    }

    /** Strip markup to plain text (for logs, console, item comparisons). */
    public static String plain(String input) {
        return PlainTextComponentSerializer.plainText().serialize(mm(input));
    }
}
