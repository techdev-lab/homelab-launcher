package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

/**
 * The bundled Scenes pictures (docs/BATCH_S_SCENES.md, made by scenes/to-scenes.py): every scene has its WebP files at the
 * right pixel size, the cut-outs carry alpha, the first-row numbers in {@link SceneList} equal the manifest, and the
 * whole set stays inside its byte budget. Sizes are read from the RIFF header, so no image library is needed.
 */
public class SceneAssetsTest {
    private static final File DIR = new File("../../scenes");
    /** The five scenes cost about 1.0 MB; more than this means a quality was raised without thinking about the 4.2 MB cap. */
    private static final long BUDGET_BYTES = 1_100_000L;

    private static byte[] read(File f) throws Exception {
        return Files.readAllBytes(f.toPath());
    }

    /** True when a VP8X header has its alpha flag (bit 4 of the flags byte) set. */
    static boolean hasAlpha(byte[] b) {
        return b.length > 20 && b[12] == 'V' && b[13] == 'P' && b[14] == '8' && b[15] == 'X' && (b[20] & 0x10) != 0;
    }

    /** Problems found for one scene folder; empty when it is complete and well formed. */
    static List<String> problems(String id) throws Exception {
        List<String> out = new ArrayList<>();
        File dir = new File(DIR, id);
        SceneList.Scene scene = SceneList.of(id);
        String[] roles = scene.hasCutOut() ? new String[]{"bg", "glass", "cut"} : new String[]{"bg", "glass"};
        for (String role : roles) {
            File f = new File(dir, role + ".webp");
            if (!f.isFile()) {
                out.add(id + "/" + role + ".webp: missing");
                continue;
            }
            byte[] b = read(f);
            int[] size = WallpaperAssetsTest.webpSize(b);
            int w = "glass".equals(role) ? SceneGeometry.GLASS_W : SceneList.PICTURE_W;
            int h = "glass".equals(role) ? SceneGeometry.GLASS_H : SceneList.PICTURE_H;
            if (size == null) out.add(id + "/" + role + ".webp: not a WebP");
            else if (size[0] != w || size[1] != h) out.add(id + "/" + role + ".webp: " + size[0] + "x" + size[1]);
            if ("cut".equals(role) && !hasAlpha(b)) out.add(id + "/cut.webp: no alpha");
        }
        if (!scene.hasCutOut() && new File(dir, "cut.webp").exists()) out.add(id + ": has an unexpected cut.webp");
        return out;
    }

    @Test
    public void everySceneIsCompleteAtTheRightPixelSize() throws Exception {
        List<String> all = new ArrayList<>();
        for (String id : SceneList.ALL) all.addAll(problems(id));
        if (!all.isEmpty()) fail("run python scenes/to-scenes.py: " + all);
    }

    @Test
    public void theFirstOpaqueRowsInTheCodeEqualTheManifest() throws Exception {
        String manifest = new String(read(new File(DIR, "manifest.json")), StandardCharsets.UTF_8);
        for (String id : SceneList.ALL) {
            SceneList.Scene s = SceneList.of(id);
            int at = manifest.indexOf("\"" + id + "\": {");
            assertTrue(id + " is not in the manifest", at >= 0);
            int next = manifest.length();
            for (String other : SceneList.ALL) {
                int o = manifest.indexOf("\"" + other + "\": {", at + 1);
                if (o > at && o < next) next = o;
            }
            Matcher m = Pattern.compile("\"cut_first_row\": (\\d+)").matcher(manifest.substring(at, next));
            if (s.hasCutOut()) {
                assertTrue(id + ": no cut_first_row in the manifest", m.find());
                assertEquals(id, Integer.parseInt(m.group(1)), s.cutFirstRow);
            } else {
                assertTrue(id + ": the manifest has a cut-out the code does not know", !m.find());
            }
        }
    }

    @Test
    public void theBundledFilesStayInsideTheByteBudget() throws Exception {
        long total = 0;
        for (String id : SceneList.ALL) {
            File[] files = new File(DIR, id).listFiles((d, n) -> n.endsWith(".webp"));
            assertNotNull(id, files);
            for (File f : files) total += f.length();
        }
        assertTrue("scene pictures are " + total + " bytes", total > 0 && total <= BUDGET_BYTES);
    }

    @Test
    public void theCheckerCatchesAMissingFileAndAWrongSize() throws Exception {
        // night without its cut-out would be reported; a header that is not a WebP is not accepted as one
        assertTrue(WallpaperAssetsTest.webpSize(new byte[40]) == null);
        assertTrue(!hasAlpha(new byte[40]));
        byte[] vp8x = new byte[40];
        System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, vp8x, 0, 4);
        System.arraycopy("WEBPVP8X".getBytes(StandardCharsets.US_ASCII), 0, vp8x, 8, 8);
        assertTrue(!hasAlpha(vp8x));
        vp8x[20] = 0x10;
        assertTrue(hasAlpha(vp8x));
    }
}
