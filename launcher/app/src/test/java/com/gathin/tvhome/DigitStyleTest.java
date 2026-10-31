package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
    public void latinOnAnArabicScreenDrawsLatinDigits() {
        // The clock and the dates come out of the system locale as Arabic-Indic digits; latin must bring them back.
        assertEquals("12:03", DigitStyle.apply("\u0661\u0662:\u0660\u0663", DigitStyle.LATIN));
        assertEquals("0123456789", DigitStyle.apply("\u0660\u0661\u0662\u0663\u0664\u0665\u0666\u0667\u0668\u0669",
                DigitStyle.LATIN));
        assertEquals("28 \u0623\u0643\u062a\u0648\u0628\u0631 2026",
                DigitStyle.apply("\u0662\u0668 \u0623\u0643\u062a\u0648\u0628\u0631 \u0662\u0660\u0662\u0666", DigitStyle.LATIN));
    }

    @Test
    public void latinKeepsWhatIsAlreadyLatinAndTheDefaultIsLatin() {
        assertEquals("12:03", DigitStyle.apply("12:03", DigitStyle.LATIN));
        assertEquals("an unknown style is latin", "12", DigitStyle.apply("\u0661\u0662", "persian"));
        assertEquals("no style is latin", "12", DigitStyle.apply("\u0661\u0662", null));
    }

    @Test
    public void aNumberInsideArabicTextChangesAndTheLettersDoNot() {
        String arabicIndic = "\u0627\u0644\u0639\u0635\u0631 \u0628\u0639\u062f \u0663 \u0633\u0627\u0639\u0627\u062a";
        String latin = "\u0627\u0644\u0639\u0635\u0631 \u0628\u0639\u062f 3 \u0633\u0627\u0639\u0627\u062a";
        assertEquals(latin, DigitStyle.apply(arabicIndic, DigitStyle.LATIN));
        assertEquals(arabicIndic, DigitStyle.apply(latin, DigitStyle.ARABIC));
    }

    @Test
    public void theArabicDecimalAndPercentSignsGoWithTheirDigits() {
        assertEquals("35.5°", DigitStyle.apply("\u0663\u0665\u066b\u0665°", DigitStyle.LATIN));
        assertEquals("1,200", DigitStyle.apply("\u0661\u066c\u0662\u0660\u0660", DigitStyle.LATIN));
        assertEquals("50%", DigitStyle.apply("\u0665\u0660\u066a", DigitStyle.LATIN));
    }

    @Test
    public void bothStylesAgreeOnAMixedString() {
        String mixed = "1 \u0662 3";
        assertEquals("1 2 3", DigitStyle.apply(mixed, DigitStyle.LATIN));
        assertEquals("\u0661 \u0662 \u0663", DigitStyle.apply(mixed, DigitStyle.ARABIC));
    }

    @Test
    public void applyingTwiceChangesNothingMore() {
        for (String style : DigitStyle.ALL) {
            String once = DigitStyle.apply("\u0661\u0662:30 14", style);
            assertEquals(once, DigitStyle.apply(once, style));
        }
    }

    @Test
    public void emptyTextStaysEmpty() {
        assertEquals("", DigitStyle.apply("", DigitStyle.LATIN));
        assertEquals("", DigitStyle.apply("", DigitStyle.ARABIC));
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
