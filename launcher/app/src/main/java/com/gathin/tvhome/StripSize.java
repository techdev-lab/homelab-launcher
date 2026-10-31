package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.List;

/**
 * Text size of the interests strip, picked in Launcher settings: the bigger the text, the fewer cards in the row.
 * The strip is always one row: a second row pushed the app dock off a 1080p TV (seen on the TV, 2.9.0), so the
 * cards that do not fit take turns in the last place. Small keeps the look of 2.8 (five cards at the original
 * sizes), so an upgrade changes nothing until the viewer picks another size. Pure Java, unit tested on the JVM.
 */
final class StripSize {
    static final String SMALL = "small", MEDIUM = "medium", LARGE = "large";
    static final String[] ALL = {SMALL, MEDIUM, LARGE};

    private StripSize() {
    }

    /** The TV's choice, else config.json "text_size", else small; anything unknown counts as unset. */
    static String pick(String chosen, String theme) {
        if (valid(chosen)) return chosen;
        return valid(theme) ? theme : SMALL;
    }

    static boolean valid(String s) {
        return SMALL.equals(s) || MEDIUM.equals(s) || LARGE.equals(s);
    }

    /** Places in the strip: 5, 4 or 3. */
    static int perRow(String size) {
        if (LARGE.equals(size)) return 3;
        return MEDIUM.equals(size) ? 4 : 5;
    }

    /** Places in the strip next to the right-hand dock column: one fewer than {@link #perRow}, at least two. */
    static int perRowSide(String size) {
        return Math.max(2, perRow(size) - 1);
    }

    /** Factor on every text size of a strip card (the prayer card's too). */
    static float scale(String size) {
        if (LARGE.equals(size)) return 1.35f;
        return MEDIUM.equals(size) ? 1.15f : 1f;
    }

    /**
     * The cards on screen for this turn: all of them when they fit, else the first {@code perRow - 1} and, in the
     * last place, one of the others in turn ({@code turn} counts up). Every card gets its turn; none is dropped.
     */
    static <T> List<T> visible(List<T> cards, int perRow, int turn) {
        int n = Math.max(1, perRow);
        if (cards.size() <= n) return new ArrayList<>(cards);
        List<T> out = new ArrayList<>(cards.subList(0, n - 1));
        List<T> rest = cards.subList(n - 1, cards.size());
        out.add(rest.get(Math.floorMod(turn, rest.size())));
        return out;
    }

    /** True when some cards take turns in the last place. */
    static boolean shared(int cards, int perRow) {
        return cards > Math.max(1, perRow);
    }

    /**
     * The weight sum of the row: never less than a full row of plain cards, so a lone card keeps the width it has
     * among others instead of stretching across the screen.
     */
    static float rowWeightSum(float cardWeights, int perRow) {
        return Math.max(cardWeights, perRow);
    }
}
