package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * The rules for hiding apps from the tiles, picked on the TV in Launcher settings (docs/REMOTE_RELIABILITY.md). A hidden app
 * stays installed and opens from anywhere else; it is only not shown here. The TV's choice is stored as a comma list and, like
 * the favourite clubs, wins over the theme's "hidden_apps" (an empty choice means nothing is hidden); Reset removes it so the
 * theme's list applies again. The launcher itself can never be hidden. Pure Java, unit tested on the JVM.
 */
final class HiddenApps {
    /** More than any TV has apps: a longer stored list is cut. */
    static final int MAX = 200;
    /** A stored value larger than this is only read up to here, so a corrupted value cannot cost time. */
    private static final int MAX_CHARS = 20_000;
    private static final String SELF = "com.gathin.tvhome";

    private HiddenApps() {
    }

    static boolean valid(String pkg) {
        return pkg != null && !SELF.equals(pkg) && pkg.matches("[A-Za-z0-9_.]{3,150}");
    }

    /** The packages in a stored comma list: in order, valid ones only, without duplicates, at most {@link #MAX}. */
    static List<String> parse(String csv) {
        if (csv == null || csv.isEmpty()) return new ArrayList<>();
        String text = csv;
        boolean cut = false;
        if (text.length() > MAX_CHARS) {
            text = text.substring(0, MAX_CHARS);
            cut = true;
        }
        String[] parts = text.split(",");
        // A cut value ends in the middle of a name: that last piece is not trusted.
        int end = cut ? parts.length - 1 : parts.length;
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (int i = 0; i < end && out.size() < MAX; i++) {
            String p = parts[i].trim();
            if (valid(p)) out.add(p);
        }
        return new ArrayList<>(out);
    }

    static String toCsv(Collection<String> packages) {
        return String.join(",", packages);
    }

    /** A new list with the package hidden, or shown again if it was hidden. Invalid names and a full list change nothing. */
    static List<String> toggle(List<String> current, String pkg) {
        List<String> out = new ArrayList<>(current);
        if (out.remove(pkg)) return out;
        if (valid(pkg) && out.size() < MAX) out.add(pkg);
        return out;
    }

    /**
     * The list in force. {@code tvChoice} is the stored comma list, or null when nothing was picked on the TV: then the theme's
     * list applies. An empty (but present) choice hides nothing.
     */
    static List<String> effective(String tvChoice, List<String> theme) {
        if (tvChoice != null) return parse(tvChoice);
        List<String> clean = new ArrayList<>();
        if (theme == null) return clean;
        for (String p : theme) {
            if (valid(p) && !clean.contains(p) && clean.size() < MAX) clean.add(p);
        }
        return clean;
    }
}
