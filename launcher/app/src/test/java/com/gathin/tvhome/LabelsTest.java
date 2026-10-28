package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

/** The tile label cleaner: formatting characters never reach the screen, and long names are cut safely. */
public class LabelsTest {

    @Test
    public void cleanStripsFormattingAndBounds() {
        assertEquals("Netflix", Labels.clean("Netflix\u202E", "io.example"));
        assertEquals("a b", Labels.clean("a\n\nb", "io.example"));
        assertEquals("a b c", Labels.clean("  a \t b\u2028c  ", "io.example"));
        assertEquals("Zero width", Labels.clean("Zero\u200D width\u200B", "io.example"));
        assertEquals("Retro", Labels.clean("Retro", "io.example"));
        assertEquals("io.example", Labels.clean("", "io.example"));
        assertEquals("io.example", Labels.clean("\u202E\n\u0000", "io.example"));
        assertEquals("io.example", Labels.clean(null, "io.example"));
        char[] big = new char[100];
        Arrays.fill(big, 'x');
        assertEquals(40, Labels.clean(new String(big), "io.example").length());
        // 39 letters then an emoji (a surrogate pair): the pair does not fit in 40 and is left out whole.
        String emoji = new String(Character.toChars(0x1F3AE));
        assertEquals(39, Labels.clean(new String(big, 0, 39) + emoji + "y", "io.example").length());
        assertEquals(new String(big, 0, 38) + emoji, Labels.clean(new String(big, 0, 38) + emoji + "y", "io.example"));
    }

    @Test
    public void arabicLabelsPassThrough() {
        assertEquals("\u0634\u0627\u0647\u062F", Labels.clean("\u0634\u0627\u0647\u062F", "net.mbc.shahid"));
        assertEquals("\u064A\u0648\u062A\u064A\u0648\u0628", Labels.clean("\u202B\u064A\u0648\u062A\u064A\u0648\u0628\u202C", "com.google.android.youtube.tv"));
    }
}
