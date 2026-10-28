package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Text size of the interests strip, picked in Launcher settings: the bigger the text, the fewer cards share a row.
 * Small keeps the look of 2.8 (five cards in a row at the original sizes), so an upgrade changes nothing until the
 * viewer picks another size. Pure Java, unit tested on the JVM.
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

    /** Cards in one row of the strip: 5, 4 or 3. */
    static int perRow(String size) {
        if (LARGE.equals(size)) return 3;
        return MEDIUM.equals(size) ? 4 : 5;
    }

    /** Factor on every text size of a strip card (the prayer card's too). */
    static float scale(String size) {
        if (LARGE.equals(size)) return 1.35f;
        return MEDIUM.equals(size) ? 1.15f : 1f;
    }

    /** The cards cut into rows of at most perRow, in order; no card is ever dropped. */
    static <T> List<List<T>> rows(List<T> cards, int perRow) {
        if (cards.isEmpty()) return Collections.emptyList();
        int n = Math.max(1, perRow);
        List<List<T>> out = new ArrayList<>();
        for (int i = 0; i < cards.size(); i += n) {
            out.add(new ArrayList<>(cards.subList(i, Math.min(cards.size(), i + n))));
        }
        return out;
    }

    /**
     * The weight sum of one row: never less than a full row of plain cards, so a short last row keeps its cards at
     * the width they have in a full one instead of stretching them across the screen.
     */
    static float rowWeightSum(float cardWeights, int perRow) {
        return Math.max(cardWeights, perRow);
    }
}
