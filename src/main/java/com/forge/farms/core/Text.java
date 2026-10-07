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

    /**
     * Convert A–Z/a–z to Unicode small caps, leaving MiniMessage tags
     * untouched. Applied to GUI titles for the small-caps look.
     */
    public static String smallCaps(String miniMessage) {
        if (miniMessage == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(miniMessage.length());
        boolean inTag = false;
        for (int i = 0; i < miniMessage.length();) {
            int cp = miniMessage.codePointAt(i);
            if (cp == '<') {
                inTag = true;
            }
            sb.appendCodePoint(inTag ? cp : toSmallCap(cp));
            if (cp == '>') {
                inTag = false;
            }
            i += Character.charCount(cp);
        }
        return sb.toString();
    }

    private static int toSmallCap(int cp) {
        return switch (cp) {
            case 'A', 'a' -> 0x1D00; // ᴀ
            case 'B', 'b' -> 0x0299; // ʙ
            case 'C', 'c' -> 0x1D04; // ᴄ
            case 'D', 'd' -> 0x1D05; // ᴅ
            case 'E', 'e' -> 0x1D07; // ᴇ
            case 'F', 'f' -> 0xA730; // ꜰ
            case 'G', 'g' -> 0x0262; // ɢ
            case 'H', 'h' -> 0x029C; // ʜ
            case 'I', 'i' -> 0x026A; // ɪ
            case 'J', 'j' -> 0x1D0A; // ᴊ
            case 'K', 'k' -> 0x1D0B; // ᴋ
            case 'L', 'l' -> 0x029F; // ʟ
            case 'M', 'm' -> 0x1D0D; // ᴍ
            case 'N', 'n' -> 0x0274; // ɴ
            case 'O', 'o' -> 0x1D0F; // ᴏ
            case 'P', 'p' -> 0x1D18; // ᴘ
            case 'R', 'r' -> 0x0280; // ʀ
            case 'S', 's' -> 0xA731; // ꜱ
            case 'T', 't' -> 0x1D1B; // ᴛ
            case 'U', 'u' -> 0x1D1C; // ᴜ
            case 'V', 'v' -> 0x1D20; // ᴠ
            case 'W', 'w' -> 0x1D21; // ᴡ
            case 'Y', 'y' -> 0x028F; // ʏ
            case 'Z', 'z' -> 0x1D22; // ᴢ
            default -> cp; // Q, X have no small-cap forms; digits/punct pass through
        };
    }
}
