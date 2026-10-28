package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
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
    public void oneRowAndTheRestTakeTurnsInTheLastPlace() {
        List<String> five = Arrays.asList("prayer", "clubs", "ksa", "europe", "weather");
        assertEquals(five, StripSize.visible(five, 5, 7));
        assertTrue(!StripSize.shared(5, 5) && StripSize.shared(5, 3));
        assertEquals(Arrays.asList("prayer", "clubs", "ksa"), StripSize.visible(five, 3, 0));
        assertEquals(Arrays.asList("prayer", "clubs", "europe"), StripSize.visible(five, 3, 1));
        assertEquals(Arrays.asList("prayer", "clubs", "weather"), StripSize.visible(five, 3, 2));
        assertEquals(Arrays.asList("prayer", "clubs", "ksa"), StripSize.visible(five, 3, 3));
        assertEquals(Arrays.asList("prayer", "clubs", "ksa", "weather"), StripSize.visible(five, 4, 1));
        assertEquals(Arrays.asList("prayer", "clubs", "europe"), StripSize.visible(five, 3, -2));
    }

    @Test
    public void shortRowKeepsCardWidth() {
        assertEquals(3f, StripSize.rowWeightSum(2f, 3), 0f);
        assertEquals(5.2f, StripSize.rowWeightSum(5.2f, 5), 0f);
    }
}
