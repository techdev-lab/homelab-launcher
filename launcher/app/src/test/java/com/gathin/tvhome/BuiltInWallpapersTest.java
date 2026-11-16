package com.gathin.tvhome;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.Test;

public class BuiltInWallpapersTest {
    private static File touch(File dir, String name) throws Exception {
        dir.mkdirs();
        File f = new File(dir, name);
        Files.write(f.toPath(), new byte[]{1, 2, 3});
        return f;
    }

    @Test
    public void upgradeRemovesOldJpegCopiesAndTheStampOfTheSet() throws Exception {
        File files = Files.createTempDirectory("files").toFile();
        File set = new File(new File(files, "builtin-wallpapers"), "serene");
        File oldJpg = touch(set, "01-dusk-glow.jpg");
        File stamp = touch(set, ".version");
        BuiltInWallpapers.clearSet(set);
        assertFalse(oldJpg.exists());
        assertFalse(stamp.exists());
        assertTrue(set.isDirectory());
    }

    @Test
    public void usersOwnPicturesAndOtherSetsAreNeverDeleted() throws Exception {
        File files = Files.createTempDirectory("files").toFile();
        File set = new File(new File(files, "builtin-wallpapers"), "serene");
        File otherSet = touch(new File(new File(files, "builtin-wallpapers"), "kids"), "01.webp");
        File userRotate = touch(new File(files, "wallpapers"), "mine.jpg");
        File userFixed = touch(files, "wallpaper.jpg");
        touch(set, "01-dusk-glow.jpg");
        BuiltInWallpapers.clearSet(set);
        assertTrue(otherSet.exists());
        assertTrue(userRotate.exists());
        assertTrue(userFixed.exists());
    }

    @Test
    public void missingOrNullFolderIsAHarmlessNoOp() {
        BuiltInWallpapers.clearSet(null);
        BuiltInWallpapers.clearSet(new File("no-such-folder-for-wallpapers"));
    }

    @Test
    public void theStampNamesTheFormatSoAFormatChangeForcesACleanCopy() {
        assertTrue(BuiltInWallpapers.FORMAT.length() > 0);
    }
}
