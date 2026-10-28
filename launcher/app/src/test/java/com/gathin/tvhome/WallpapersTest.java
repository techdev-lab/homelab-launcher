package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.List;

import org.junit.Test;

public class WallpapersTest {

    @Test
    public void choiceOnTheTvWinsThenConfig() {
        assertEquals("homelab", Wallpapers.mode("homelab", "rotate", true, false));
        assertEquals("rotate", Wallpapers.mode("", "rotate", true, false));
        assertEquals("serene", Wallpapers.mode(null, "serene", true, true));
    }

    @Test
    public void freshInstallShowsSereneButOldThemesKeepTheirLook() {
        assertEquals("serene", Wallpapers.mode("", "", false, false));
        assertEquals("fixed", Wallpapers.mode("", "", false, true));
        assertEquals("fixed", Wallpapers.mode("", "", true, false));
        assertEquals("fixed", Wallpapers.mode("nonsense", "also-bad", true, false));
    }

    @Test
    public void builtInSetsAreKnown() {
        assertTrue(Wallpapers.builtIn("homelab"));
        assertTrue(Wallpapers.builtIn("serene"));
        assertTrue(Wallpapers.builtIn("kids"));
        assertEquals("kids", Wallpapers.mode("kids", "rotate", true, true));
        assertFalse(Wallpapers.builtIn("rotate"));
        assertFalse(Wallpapers.builtIn(null));
    }

    @Test
    public void onlyPicturesWithinSize() {
        assertTrue(Wallpapers.accepted("01-homelab.jpg", 500_000));
        assertTrue(Wallpapers.accepted("x.PNG", 1));
        assertFalse(Wallpapers.accepted("notes.txt", 10));
        assertFalse(Wallpapers.accepted(".hidden.jpg", 10));
        assertFalse(Wallpapers.accepted("empty.jpg", 0));
        assertFalse(Wallpapers.accepted("huge.jpg", Wallpapers.MAX_BYTES + 1));
    }

    @Test
    public void indexStepsEveryIntervalAndWraps() {
        assertEquals(0, Wallpapers.indexAt(0, 5, 3));
        assertEquals(0, Wallpapers.indexAt(4, 5, 3));
        assertEquals(1, Wallpapers.indexAt(5, 5, 3));
        assertEquals(2, Wallpapers.indexAt(14, 5, 3));
        assertEquals(0, Wallpapers.indexAt(15, 5, 3));
        assertEquals(-1, Wallpapers.indexAt(15, 5, 0));
        assertEquals(1, Wallpapers.indexAt(1, 0, 3));   // a bad interval behaves like 1 minute
    }

    @Test
    public void listsInNameOrder() throws Exception {
        File dir = Files.createTempDirectory("walls").toFile();
        for (String n : new String[]{"03-c.jpg", "01-a.jpg", "readme.txt", "02-b.png"}) {
            try (FileOutputStream out = new FileOutputStream(new File(dir, n))) {
                out.write(1);
            }
        }
        List<File> got = Wallpapers.list(dir);
        assertEquals(3, got.size());
        assertEquals("01-a.jpg", got.get(0).getName());
        assertEquals("03-c.jpg", got.get(2).getName());
        assertTrue(Wallpapers.list(new File(dir, "missing")).isEmpty());
    }
}
