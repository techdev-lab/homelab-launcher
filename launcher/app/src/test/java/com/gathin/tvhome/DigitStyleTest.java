package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DigitStyleTest {

    @Test
    public void theTvChoiceWinsThenTheThemeThenLatin() {
        assertEquals(DigitStyle.ARABIC, DigitStyle.pick("arabic", "latin"));
        assertEquals(DigitStyle.LATIN, DigitStyle.pick("latin", "arabic"));
        assertEquals(DigitStyle.ARABIC, DigitStyle.pick("", "arabic"));
        assertEquals(DigitStyle.ARABIC, DigitStyle.pick(null, "arabic"));
        assertEquals(DigitStyle.LATIN, DigitStyle.pick(null, null));
        assertEquals(DigitStyle.LATIN, DigitStyle.pick("", ""));
    }

    @Test
    public void anUnknownValueCountsAsUnset() {
        assertEquals("unknown TV choice falls to the theme", DigitStyle.ARABIC, DigitStyle.pick("persian", "arabic"));
        assertEquals("unknown theme falls to latin", DigitStyle.LATIN, DigitStyle.pick(null, "persian"));
        assertEquals("case matters, like every other look option", DigitStyle.LATIN, DigitStyle.pick("Arabic", null));
        assertFalse(DigitStyle.valid(null));
        assertFalse(DigitStyle.valid(""));
        assertTrue(DigitStyle.valid("arabic"));
        assertTrue(DigitStyle.valid("latin"));
    }

    @Test
    public void arabicDrawsEveryDigitAsAnArabicIndicDigit() {
        assertEquals("\u0661\u0662:\u0660\u0663", DigitStyle.apply("12:03", DigitStyle.ARABIC));
        assertEquals("\u0660\u0661\u0662\u0663\u0664\u0665\u0666\u0667\u0668\u0669",
                DigitStyle.apply("0123456789", DigitStyle.ARABIC));
    }

    @Test
    public void everythingThatIsNotADigitStaysAsItIs() {
        assertEquals("Asr \u0661\u0664:\u0663\u0668 PM", DigitStyle.apply("Asr 14:38 PM", DigitStyle.ARABIC));
        assertEquals("\u0663\u0665°", DigitStyle.apply("35°", DigitStyle.ARABIC));
        assertEquals("\u0662 – \u0661", DigitStyle.apply("2 – 1", DigitStyle.ARABIC));
        assertEquals("", DigitStyle.apply("", DigitStyle.ARABIC));
    }

    @Test
    public void latinChangesNothing() {
        String s = "12:03 \u0661\u0662";
        assertSame(s, DigitStyle.apply(s, DigitStyle.LATIN));
        assertSame("an unknown style is latin", s, DigitStyle.apply(s, "persian"));
    }

    @Test
    public void nullIsHandled() {
        assertNull(DigitStyle.apply(null, DigitStyle.ARABIC));
        assertNull(DigitStyle.apply(null, DigitStyle.LATIN));
    }

    @Test
    public void textThatIsAlreadyArabicIndicIsNotChangedTwice() {
        assertEquals("\u0661\u0662", DigitStyle.apply("\u0661\u0662", DigitStyle.ARABIC));
    }
}
