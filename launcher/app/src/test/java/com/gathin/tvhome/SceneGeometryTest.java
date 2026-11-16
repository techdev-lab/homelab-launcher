package com.gathin.tvhome;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SceneGeometryTest {
    private static SceneList.Scene s(String id) {
        return SceneList.of(id);
    }

    @Test
    public void theCoverMatrixFillsTheScreenOnEveryShape() {
        assertArrayEquals(new float[]{1f, 0f, 0f}, SceneGeometry.cover(1920, 1080), 0.0001f);
        assertArrayEquals(new float[]{2f, 0f, 0f}, SceneGeometry.cover(3840, 2160), 0.0001f);
        assertArrayEquals(new float[]{1280f / 1920, 0f, 0f}, SceneGeometry.cover(1280, 720), 0.0001f);
        float[] square = SceneGeometry.cover(1024, 768);   // 4:3: the height limits, the sides are cropped
        assertEquals(768f / 1080, square[0], 0.0001f);
        assertTrue(square[1] < 0);
        assertEquals(0f, square[2], 0.0001f);
    }

    @Test
    public void aZeroOrNegativeScreenGivesTheIdentityInsteadOfDividingByZero() {
        assertArrayEquals(new float[]{1f, 0f, 0f}, SceneGeometry.cover(0, 1080), 0f);
        assertArrayEquals(new float[]{1f, 0f, 0f}, SceneGeometry.cover(1920, -5), 0f);
    }

    @Test
    public void theBandRowsAndBytesMatchTheDesignTable() {
        assertArrayEquals(new int[]{154, 478}, SceneGeometry.band(s("night")));
        assertArrayEquals(new int[]{491, 557}, SceneGeometry.band(s("fog")));
        assertArrayEquals(new int[]{364, 472}, SceneGeometry.band(s("golden")));
        assertArrayEquals(new int[]{479, 558}, SceneGeometry.band(s("dawn")));
        assertEquals(2_488_320L, SceneGeometry.bandBytes(s("night")));
        assertEquals(506_880L, SceneGeometry.bandBytes(s("fog")));
        assertEquals(829_440L, SceneGeometry.bandBytes(s("golden")));
        assertEquals(606_720L, SceneGeometry.bandBytes(s("dawn")));
    }

    @Test
    public void oneSceneInMemoryMatchesTheDesignTable() {
        assertEquals(6_894_720L, SceneGeometry.sceneBytes(s("night")));
        assertEquals(4_913_280L, SceneGeometry.sceneBytes(s("fog")));
        assertEquals(5_235_840L, SceneGeometry.sceneBytes(s("golden")));
        assertEquals(5_013_120L, SceneGeometry.sceneBytes(s("dawn")));
        assertEquals(4_406_400L, SceneGeometry.sceneBytes(s("water")));
        assertEquals(7_111_680L, SceneGeometry.fullCutBytes(s("night")));
    }

    @Test
    public void theBandIsAlwaysMuchSmallerThanTheWholeCutOut() {
        for (String id : new String[]{"night", "fog", "golden", "dawn"}) {
            assertTrue(id, SceneGeometry.bandBytes(s(id)) < SceneGeometry.fullCutBytes(s(id)) / 2);
        }
    }

    @Test
    public void aSceneWithoutACutOutHasNoBandAndNothingIsDecoded() {
        assertNull(SceneGeometry.band(s("water")));
        assertEquals(0L, SceneGeometry.bandBytes(s("water")));
        assertEquals(0L, SceneGeometry.fullCutBytes(s("water")));
    }

    @Test
    public void aClockBoxThatEndsAboveTheFirstOpaqueRowGivesNoBand() {
        SceneList.Scene high = new SceneList.Scene("x", 700, 100, 300, false, 0, false);   // box ends at 346
        assertNull(SceneGeometry.band(high));
        assertEquals(0L, SceneGeometry.bandBytes(high));
    }

    @Test
    public void aBandIsClampedToThePicture() {
        SceneList.Scene low = new SceneList.Scene("x", 900, 900, 600, false, 0, false);    // box would end at 1392
        assertArrayEquals(new int[]{900, 1080}, SceneGeometry.band(low));
        assertEquals(SceneList.PICTURE_H, SceneGeometry.clockBottom(low));
    }

    @Test
    public void theClockPositionIsAPropertyOfThePictureNotOfTheReadingDirection() {
        // centred scenes: the same x for any text; fog starts at the left margin. Nothing here takes a direction.
        assertEquals((1920 - 800) / 2f, SceneGeometry.clockLeft(s("night"), 800f), 0.001f);
        assertEquals((1920 - 800) / 2f, SceneGeometry.clockLeft(s("water"), 800f), 0.001f);
        assertEquals((float) SceneList.CLOCK_LEFT_MARGIN, SceneGeometry.clockLeft(s("fog"), 800f), 0.001f);
        assertEquals(300, SceneGeometry.scrimHeight());
    }
}
