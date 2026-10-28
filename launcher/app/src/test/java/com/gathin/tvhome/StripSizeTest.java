package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class StripSizeTest {

    @Test
    public void tvChoiceWinsThenThemeThenSmall() {
        assertEquals("large", StripSize.pick("large", "medium"));
        assertEquals("medium", StripSize.pick("", "medium"));
        assertEquals("small", StripSize.pick("", ""));
        assertEquals("small", StripSize.pick("huge", "tiny"));
        assertEquals("medium", StripSize.pick(null, "medium"));
    }

    @Test
    public void largerTextFewerCardsPerRow() {
        assertEquals(5, StripSize.perRow("small"));
        assertEquals(4, StripSize.perRow("medium"));
        assertEquals(3, StripSize.perRow("large"));
        assertEquals(5, StripSize.perRow("anything else"));
        assertEquals(1f, StripSize.scale("small"), 0f);
        assertTrue(StripSize.scale("medium") > 1f);
        assertTrue(StripSize.scale("large") > StripSize.scale("medium"));
    }

    @Test
    public void rowsKeepOrderAndDropNothing() {
        List<String> five = Arrays.asList("prayer", "clubs", "league", "weather", "news");
        assertEquals(Collections.singletonList(five), StripSize.rows(five, 5));
        List<List<String>> large = StripSize.rows(five, 3);
        assertEquals(2, large.size());
        assertEquals(Arrays.asList("prayer", "clubs", "league"), large.get(0));
        assertEquals(Arrays.asList("weather", "news"), large.get(1));
        assertEquals(2, StripSize.rows(five, 4).size());
        assertEquals(0, StripSize.rows(Collections.<String>emptyList(), 3).size());
        assertEquals(5, StripSize.rows(five, 0).size());
    }

    @Test
    public void shortRowKeepsCardWidth() {
        assertEquals(3f, StripSize.rowWeightSum(2f, 3), 0f);
        assertEquals(5.2f, StripSize.rowWeightSum(5.2f, 5), 0f);
    }
}
