package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The scenes of the Scenes look (docs/BATCH_S_SCENES.md): their fixed order, which ones are on, and the numbers that
 * place the clock on each picture. All positions are in "picture space", 1920 x 1080, and are scaled with the cover matrix
 * of {@link SceneGeometry}, so they hold on every screen. Pure Java, unit tested on the JVM.
 */
final class SceneList {
    static final String NIGHT = "night", FOG = "fog", GOLDEN = "golden", DAWN = "dawn", WATER = "water";
    /** The fixed order, which is also the priority: the last one is the first to be cut. */
    static final String[] ALL = {NIGHT, FOG, GOLDEN, DAWN, WATER};

    /** The picture every scene is drawn on. */
    static final int PICTURE_W = 1920, PICTURE_H = 1080;

    /** The clock of a left-aligned scene starts this far from the left edge of the picture. */
    static final int CLOCK_LEFT_MARGIN = 96;

    /** One scene's data. Start values for the clock, tuned on a TV; they are data, not logic. */
    static final class Scene {
        final String id;
        /** Top row of the first non-transparent pixel of the cut-out, or -1 when the scene has no cut-out. */
        final int cutFirstRow;
        /** Top of the clock box and the font size in picture pixels; the box is 0.82 of the size tall. */
        final int clockTop, clockSize;
        /** True: the clock starts at {@link #CLOCK_LEFT_MARGIN} instead of being centred (its scenery leaves room there). */
        final boolean clockAtLeft;
        /** Row of the waterline in picture pixels, or 0 when the scene has none. */
        final int waterLine;
        /** True for a bright picture: the optional top scrim ({@link SceneScrim}) is drawn for it in "auto". */
        final boolean bright;

        Scene(String id, int cutFirstRow, int clockTop, int clockSize, boolean clockAtLeft, int waterLine,
              boolean bright) {
            this.id = id;
            this.cutFirstRow = cutFirstRow;
            this.clockTop = clockTop;
            this.clockSize = clockSize;
            this.clockAtLeft = clockAtLeft;
            this.waterLine = waterLine;
            this.bright = bright;
        }

        boolean hasCutOut() {
            return cutFirstRow >= 0;
        }
    }

    private static final Scene[] SCENES = {
            new Scene(NIGHT, 154, 150, 400, false, 0, false),
            new Scene(FOG, 491, 172, 470, true, 0, false),
            new Scene(GOLDEN, 364, 152, 390, false, 0, true),
            new Scene(DAWN, 479, 230, 400, false, 0, true),
            new Scene(WATER, -1, 268, 420, false, 594, false)};

    private SceneList() {
    }

    static boolean known(String id) {
        return indexOf(id) >= 0;
    }

    /** Position in {@link #ALL}, or -1 for an id that is not a scene (ids are lower case). */
    static int indexOf(String id) {
        for (int i = 0; i < ALL.length; i++) if (ALL[i].equals(id)) return i;
        return -1;
    }

    /** The scene's data; an unknown id gives Night, so a caller always has something to draw. */
    static Scene of(String id) {
        int i = indexOf(id == null ? "" : id.trim().toLowerCase(Locale.ROOT));
        return SCENES[Math.max(0, i)];
    }

    /**
     * The scenes that are on, in the fixed order. Anything that is not a known id is dropped, a repeat counts once, and
     * an empty result gives Night alone, so there is always one scene to show. Items that are not strings are dropped.
     */
    static List<String> enabled(Iterable<?> ids) {
        boolean[] on = new boolean[ALL.length];
        if (ids != null) {
            for (Object o : ids) {
                if (!(o instanceof String)) continue;
                int i = indexOf(((String) o).trim().toLowerCase(Locale.ROOT));
                if (i >= 0) on[i] = true;
            }
        }
        List<String> out = new ArrayList<>();
        for (int i = 0; i < ALL.length; i++) if (on[i]) out.add(ALL[i]);
        if (out.isEmpty()) out.add(NIGHT);
        return out;
    }

    /** The TV's choice is stored as "night,fog"; blanks and empty parts are ignored. Null gives Night alone. */
    static List<String> fromCsv(String csv) {
        List<String> parts = new ArrayList<>();
        if (csv != null) for (String p : csv.split(",")) parts.add(p);
        return enabled(parts);
    }

    /** Which scenes are on: the TV's comma list (null when none was chosen), else config.json's list (null when absent), else all. */
    static List<String> pick(String tvCsv, List<String> theme) {
        if (tvCsv != null) return fromCsv(tvCsv);
        if (theme != null) return enabled(theme);
        return enabled(java.util.Arrays.asList(ALL));
    }

    static String toCsv(List<String> ids) {
        StringBuilder b = new StringBuilder();
        for (String id : enabled(ids)) {
            if (b.length() > 0) b.append(',');
            b.append(id);
        }
        return b.toString();
    }

    /** True when {@code id} is in {@code enabled}. */
    static boolean on(List<String> enabled, String id) {
        return enabled != null && enabled.contains(id);
    }

    /**
     * The scene to show when {@code wanted} is off: the nearest enabled one in the fixed order, the earlier one on a tie.
     * {@code enabled} is normalised first (see {@link #enabled}); an empty or null list gives Night.
     */
    static String nearestEnabled(List<String> enabled, String wanted) {
        List<String> list = enabled(enabled);
        if (list.contains(wanted)) return wanted;
        int want = Math.max(0, indexOf(wanted));
        String best = list.get(0);
        int bestDistance = Integer.MAX_VALUE;
        for (String id : list) {
            int d = Math.abs(indexOf(id) - want);
            if (d < bestDistance) {
                bestDistance = d;
                best = id;
            }
        }
        return best;
    }
}
