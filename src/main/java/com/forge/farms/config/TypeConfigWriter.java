package com.forge.farms.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Surgical edits to {@code farm-types/*.yml} (and {@code config.yml}) that
 * preserve comments, ordering, and formatting. The admin in-game editor
 * writes through here; values are re-read via
 * {@link ConfigManager#reloadType(String)}.
 *
 * <p>All methods return false when the target was not found; IOException on
 * IO failure.
 */
public final class TypeConfigWriter {
    private TypeConfigWriter() {
    }

    // ------------------------------------------------------------------
    // Scalars
    // ------------------------------------------------------------------

    /** Replace the value of an existing {@code a.b.c: value} key. */
    public static boolean setScalar(File file, String path, String value) throws IOException {
        List<String> lines = read(file);
        int idx = findHeader(lines, path.split("\\."));
        if (idx < 0 || !hasValue(lines.get(idx))) {
            return false;
        }
        lines.set(idx, replaceValue(lines.get(idx), value));
        write(file, lines);
        return true;
    }

    /** Replace an existing {@code key: [A, B]} flow list. */
    public static boolean setFlowList(File file, String path, List<String> values) throws IOException {
        return setScalar(file, path, "[" + String.join(", ", values) + "]");
    }

    /**
     * Replace a block string list ({@code key:} followed by {@code - "..."}
     * lines), e.g. {@code description} or {@code hologram.lines}. A scalar
     * value is converted to list form.
     */
    public static boolean setStringList(File file, String path, List<String> values) throws IOException {
        List<String> lines = read(file);
        String[] parts = path.split("\\.");
        int idx = findHeader(lines, parts);
        if (idx < 0) {
            return false;
        }
        String header = lines.get(idx);
        int hIndent = indentOf(header);
        String key = parts[parts.length - 1];
        lines.set(idx, " ".repeat(hIndent) + key + ":");
        int j = idx + 1;
        while (j < lines.size()) {
            String l = lines.get(j);
            String t = l.stripLeading();
            if (t.isEmpty()) {
                j++;
                continue;
            }
            if (indentOf(l) > hIndent && (t.startsWith("- ") || t.equals("-"))) {
                lines.remove(j);
            } else {
                break;
            }
        }
        for (int k = 0; k < values.size(); k++) {
            lines.add(j + k, " ".repeat(hIndent + 2) + "- \"" + escape(values.get(k)) + "\"");
        }
        write(file, lines);
        return true;
    }

    // ------------------------------------------------------------------
    // Harvestables
    // ------------------------------------------------------------------

    public static boolean addHarvestable(File file, String entryKey, String material, String behavior)
            throws IOException {
        List<String> lines = read(file);
        int harvest = findTopHeader(lines, "harvest:");
        if (harvest < 0) {
            return false;
        }
        int insertAt = nextTopHeader(lines, harvest + 1);
        int at = insertAt;
        while (at > harvest + 1 && lines.get(at - 1).isBlank()) {
            at--;
        }
        lines.add(at, "  " + entryKey + ":");
        lines.add(at + 1, "    material: " + material);
        lines.add(at + 2, "    behavior: " + behavior);
        write(file, lines);
        return true;
    }

    public static boolean removeHarvestable(File file, String material) throws IOException {
        List<String> lines = read(file);
        int harvest = findTopHeader(lines, "harvest:");
        if (harvest < 0) {
            return false;
        }
        int end = nextTopHeader(lines, harvest + 1);
        for (int i = harvest + 1; i < end; i++) {
            if (lines.get(i).strip().equals("material: " + material)) {
                String keyLine = lines.get(i - 1).strip();
                String behLine = i + 1 < end ? lines.get(i + 1).strip() : "";
                if (keyLine.endsWith(":") && behLine.startsWith("behavior:")) {
                    lines.remove(i + 1);
                    lines.remove(i);
                    lines.remove(i - 1);
                    write(file, lines);
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Upgrade levels (flow-map lines under upgrades.<track>.levels)
    // ------------------------------------------------------------------

    public static boolean setUpgradeLevel(File file, String track, int index, String flowBody)
            throws IOException {
        List<String> lines = read(file);
        List<Integer> idxs = upgradeLevelLines(lines, track);
        if (index < 0 || index >= idxs.size()) {
            return false;
        }
        int li = idxs.get(index);
        lines.set(li, " ".repeat(indentOf(lines.get(li))) + "- { " + flowBody + " }");
        write(file, lines);
        return true;
    }

    public static boolean addUpgradeLevel(File file, String track, String flowBody) throws IOException {
        List<String> lines = read(file);
        List<Integer> idxs = upgradeLevelLines(lines, track);
        int insertAt;
        int indent;
        if (idxs.isEmpty()) {
            int header = levelsHeader(lines, track);
            if (header < 0) {
                return false;
            }
            insertAt = header + 1;
            indent = indentOf(lines.get(header)) + 2;
        } else {
            int last = idxs.get(idxs.size() - 1);
            insertAt = last + 1;
            indent = indentOf(lines.get(last));
        }
        lines.add(insertAt, " ".repeat(indent) + "- { " + flowBody + " }");
        write(file, lines);
        return true;
    }

    public static boolean removeUpgradeLevel(File file, String track, int index) throws IOException {
        List<String> lines = read(file);
        List<Integer> idxs = upgradeLevelLines(lines, track);
        if (index < 0 || index >= idxs.size()) {
            return false;
        }
        lines.remove((int) idxs.get(index));
        write(file, lines);
        return true;
    }

    // ------------------------------------------------------------------
    // Shop price-item block
    // ------------------------------------------------------------------

    public static boolean setShopItem(File file, String material, int amount) throws IOException {
        List<String> lines = read(file);
        int mi = findHeader(lines, new String[] { "shop", "price-item", "material" });
        int ai = findHeader(lines, new String[] { "shop", "price-item", "amount" });
        if (mi >= 0 && ai >= 0) {
            lines.set(mi, replaceValue(lines.get(mi), material));
            lines.set(ai, replaceValue(lines.get(ai), String.valueOf(amount)));
            write(file, lines);
            return true;
        }
        int shop = findTopHeader(lines, "shop:");
        if (shop < 0) {
            return false;
        }
        int at = nextTopHeader(lines, shop + 1);
        while (at > shop + 1 && lines.get(at - 1).isBlank()) {
            at--;
        }
        lines.add(at, "  price-item:");
        lines.add(at + 1, "    material: " + material);
        lines.add(at + 2, "    amount: " + amount);
        write(file, lines);
        return true;
    }

    public static boolean clearShopItem(File file) throws IOException {
        List<String> lines = read(file);
        int idx = findHeader(lines, new String[] { "shop", "price-item" });
        if (idx < 0 || hasValue(lines.get(idx))) {
            return false;
        }
        int hIndent = indentOf(lines.get(idx));
        lines.remove(idx);
        while (idx < lines.size()) {
            String l = lines.get(idx);
            String t = l.stripLeading();
            if (t.isEmpty()) {
                idx++;
                continue;
            }
            if (indentOf(l) > hIndent) {
                lines.remove(idx);
            } else {
                break;
            }
        }
        write(file, lines);
        return true;
    }

    // ------------------------------------------------------------------
    // config.yml sell prices
    // ------------------------------------------------------------------

    /** Set or append {@code prices.<MATERIAL>} in config.yml. */
    public static boolean upsertPrice(File file, String material, double price) throws IOException {
        List<String> lines = read(file);
        if (setScalarIn(lines, new String[] { "prices", material }, formatDouble(price))) {
            write(file, lines);
            return true;
        }
        int prices = findTopHeader(lines, "prices:");
        if (prices < 0) {
            lines.add("prices:");
            lines.add("  " + material + ": " + formatDouble(price));
        } else {
            int at = nextTopHeader(lines, prices + 1);
            while (at > prices + 1 && lines.get(at - 1).isBlank()) {
                at--;
            }
            lines.add(at, "  " + material + ": " + formatDouble(price));
        }
        write(file, lines);
        return true;
    }

    public static boolean removePrice(File file, String material) throws IOException {
        List<String> lines = read(file);
        int idx = findHeader(lines, new String[] { "prices", material });
        if (idx < 0) {
            return false;
        }
        lines.remove(idx);
        write(file, lines);
        return true;
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private static List<String> read(File file) throws IOException {
        return new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
    }

    private static void write(File file, List<String> lines) throws IOException {
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
    }

    private static int indentOf(String line) {
        return line.length() - line.stripLeading().length();
    }

    private static boolean hasValue(String line) {
        int colon = line.indexOf(':');
        if (colon < 0) {
            return false;
        }
        String after = line.substring(colon + 1).strip();
        return !after.isEmpty() && !after.startsWith("#");
    }

    private static final Pattern VALUE_TOKEN =
            Pattern.compile("^(\\s*)(?:\"[^\"]*\"|'[^']*'|\\[.*?\\]|\\S+)(.*)$");

    private static String replaceValue(String line, String value) {
        int colon = line.indexOf(':');
        String head = line.substring(0, colon + 1);
        String rest = line.substring(colon + 1);
        Matcher m = VALUE_TOKEN.matcher(rest);
        if (m.matches()) {
            return head + m.group(1) + value + m.group(2);
        }
        return head + " " + value;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String formatDouble(double d) {
        return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    /**
     * Line index of the key at a dotted path, using indentation to track
     * sections. List items ({@code - ...}) are skipped.
     */
    private static int findHeader(List<String> lines, String[] path) {
        Deque<String> stack = new ArrayDeque<>();
        Deque<Integer> indents = new ArrayDeque<>();
        List<String> want = Arrays.asList(path);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String t = line.stripLeading();
            if (t.isEmpty() || t.startsWith("#") || t.startsWith("-")) {
                continue;
            }
            int colon = t.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String key = t.substring(0, colon).strip();
            int indent = indentOf(line);
            while (!indents.isEmpty() && indents.peekLast() >= indent) {
                indents.pollLast();
                stack.pollLast();
            }
            List<String> cur = new ArrayList<>(stack);
            cur.add(key);
            if (cur.equals(want)) {
                return i;
            }
            if (!hasValue(line)) {
                stack.addLast(key);
                indents.addLast(indent);
            }
        }
        return -1;
    }

    private static boolean setScalarIn(List<String> lines, String[] path, String value) {
        int idx = findHeader(lines, path);
        if (idx < 0 || !hasValue(lines.get(idx))) {
            return false;
        }
        lines.set(idx, replaceValue(lines.get(idx), value));
        return true;
    }

    /** Index of an indent-0 {@code name:} header, or -1. */
    private static int findTopHeader(List<String> lines, String name) {
        for (int i = 0; i < lines.size(); i++) {
            String t = lines.get(i).stripLeading();
            if (t.equals(name) && indentOf(lines.get(i)) == 0) {
                return i;
            }
        }
        return -1;
    }

    /** Index of the next indent-0 header at/after {@code from}, or lines.size(). */
    private static int nextTopHeader(List<String> lines, int from) {
        for (int i = from; i < lines.size(); i++) {
            String line = lines.get(i);
            String t = line.stripLeading();
            if (t.isEmpty() || t.startsWith("#")) {
                continue;
            }
            if (indentOf(line) == 0 && t.endsWith(":")) {
                return i;
            }
        }
        return lines.size();
    }

    /** Line indices of {@code - { ... }} level entries under upgrades.track.levels. */
    private static List<Integer> upgradeLevelLines(List<String> lines, String track) {
        List<Integer> out = new ArrayList<>();
        boolean inUpgrades = false;
        boolean inTrack = false;
        boolean inLevels = false;
        int tIndent = -1;
        int lIndent = -1;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String t = line.stripLeading();
            if (t.isEmpty() || t.startsWith("#")) {
                continue;
            }
            int indent = indentOf(line);
            if (indent == 0 && t.endsWith(":")) {
                inUpgrades = t.equals("upgrades:");
                inTrack = false;
                inLevels = false;
                continue;
            }
            if (inUpgrades && !inTrack) {
                if (t.equals(track + ":")) {
                    inTrack = true;
                    tIndent = indent;
                } else if (indent == 0) {
                    inUpgrades = false;
                }
                continue;
            }
            if (inTrack && !inLevels) {
                if (indent <= tIndent) {
                    inTrack = false;
                    continue;
                }
                if (t.equals("levels:")) {
                    inLevels = true;
                    lIndent = indent;
                }
                continue;
            }
            if (inLevels) {
                if (t.startsWith("- {")) {
                    out.add(i);
                } else if (indent <= lIndent) {
                    inLevels = false;
                    inTrack = false;
                }
            }
        }
        return out;
    }

    /** Line index of {@code levels:} under upgrades.track, or -1. */
    private static int levelsHeader(List<String> lines, String track) {
        boolean inUpgrades = false;
        boolean inTrack = false;
        int tIndent = -1;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String t = line.stripLeading();
            if (t.isEmpty() || t.startsWith("#")) {
                continue;
            }
            int indent = indentOf(line);
            if (indent == 0 && t.endsWith(":")) {
                inUpgrades = t.equals("upgrades:");
                inTrack = false;
                continue;
            }
            if (inUpgrades && !inTrack) {
                if (t.equals(track + ":")) {
                    inTrack = true;
                    tIndent = indent;
                } else if (indent == 0) {
                    inUpgrades = false;
                }
                continue;
            }
            if (inTrack) {
                if (indent <= tIndent) {
                    return -1;
                }
                if (t.equals("levels:")) {
                    return i;
                }
            }
        }
        return -1;
    }
}
