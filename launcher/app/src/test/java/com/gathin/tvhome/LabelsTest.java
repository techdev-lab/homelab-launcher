package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

/** The tile label cleaner: formatting characters never reach the screen, and long names are cut safely. */
public class LabelsTest {

    @Test
    public void cleanStripsFormattingAndBounds() {
        assertEquals("Netflix", Labels.clean("Netflix‮", "io.example"));
        assertEquals("a b", Labels.clean("a\n\nb", "io.example"));
        assertEquals("a b c", Labels.clean("  a \t b c  ", "io.example"));
        assertEquals("Zero width", Labels.clean("Zero‍ width​", "io.example"));
        assertEquals("Retro", Labels.clean("Retro", "io.example"));
        assertEquals("io.example", Labels.clean("", "io.example"));
        assertEquals("io.example", Labels.clean("‮\n\u0000", "io.example"));
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
        assertEquals("شاهد", Labels.clean("شاهد", "net.mbc.shahid"));
        assertEquals("يوتيوب", Labels.clean("‫يوتيوب‬", "com.google.android.youtube.tv"));
    }
}
