package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** Which language the screensaver uses: config.json first, then the launcher's own language, then the TV's. */
public class LanguageTest {

    @Test
    public void configLanguageWinsOverTheAppLanguage() {
        assertEquals("ar", Language.pick("ar", "en"));
        assertEquals("en", Language.pick("en", "ar"));
    }

    @Test
    public void appLanguageIsUsedWhenConfigFollowsTheTv() {
        assertEquals("ar", Language.pick("system", "ar"));
        assertEquals("en-US", Language.pick("system", "en-US"));
        assertEquals("ar", Language.pick(null, "ar"));
    }

    @Test
    public void tvLanguageIsKeptWhenNothingIsSet() {
        assertNull(Language.pick("system", null));
        assertNull(Language.pick("system", ""));
        assertNull(Language.pick(null, null));
    }

    @Test
    public void quoteWrapsTextAsALiteralPattern() {
        assertEquals("'16:04'", Language.quote("16:04"));
        assertEquals("'it''s'", Language.quote("it's"));
    }

    @Test
    public void quoteKeepsEmptyTextEmpty() {
        assertEquals("", Language.quote(""));
    }

    @Test
    public void unknownConfigValueIsIgnored() {
        assertNull(Language.pick("fr", null));
        assertEquals("ar", Language.pick("fr", "ar"));
        assertNull(Language.pick("AR ", null));
    }
}
