package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * The bundled wallpapers: every JPEG master in wallpapers/ has a lossy WebP of the same name and the same pixel size, and
 * none is dropped (docs/BATCH_L_LIGHT.md; made by wallpapers/to-webp.py, whose --check does the same). The sizes are read
 * from the JPEG start-of-frame marker and the WebP RIFF header, so the test needs no image library.
 */
public class WallpaperAssetsTest {
    private static final String[] SETS = {"homelab", "kids", "serene"};

    /** {width, height} of a lossy (VP8) or extended (VP8X) WebP, or null when the bytes are neither. */
    static int[] webpSize(byte[] b) {
        if (b.length < 30 || b[0] != 'R' || b[1] != 'I' || b[2] != 'F' || b[3] != 'F'
                || b[8] != 'W' || b[9] != 'E' || b[10] != 'B' || b[11] != 'P') return null;
        String kind = new String(b, 12, 4, StandardCharsets.US_ASCII);
        if ("VP8 ".equals(kind)) {
            if ((b[23] & 0xFF) != 0x9D || (b[24] & 0xFF) != 0x01 || (b[25] & 0xFF) != 0x2A) return null;
            return new int[]{(b[26] & 0xFF) | (b[27] & 0x3F) << 8, (b[28] & 0xFF) | (b[29] & 0x3F) << 8};
        }
        if ("VP8X".equals(kind)) {
            int w = 1 + ((b[24] & 0xFF) | (b[25] & 0xFF) << 8 | (b[26] & 0xFF) << 16);
            int h = 1 + ((b[27] & 0xFF) | (b[28] & 0xFF) << 8 | (b[29] & 0xFF) << 16);
            return new int[]{w, h};
        }
        return null;
    }

    /** {width, height} from the first start-of-frame marker of a JPEG, or null when there is none. */
    static int[] jpegSize(byte[] b) {
        if (b.length < 4 || (b[0] & 0xFF) != 0xFF || (b[1] & 0xFF) != 0xD8) return null;
        int i = 2;
        while (i + 9 < b.length) {
            if ((b[i] & 0xFF) != 0xFF) return null;
            int marker = b[i + 1] & 0xFF;
            if (marker == 0xFF) {
                i++;
                continue;
            }
            boolean frame = marker >= 0xC0 && marker <= 0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC;
            if (frame) {
                return new int[]{(b[i + 7] & 0xFF) << 8 | (b[i + 8] & 0xFF), (b[i + 5] & 0xFF) << 8 | (b[i + 6] & 0xFF)};
            }
            i += 2 + ((b[i + 2] & 0xFF) << 8 | (b[i + 3] & 0xFF));
        }
        return null;
    }

    /** Problems found in one set folder; empty when every master has a same-size WebP and no WebP is orphaned. */
    static List<String> problems(File dir) throws Exception {
        List<String> out = new ArrayList<>();
        File[] all = dir.listFiles();
        if (all == null) return new ArrayList<>(Arrays.asList(dir.getName() + ": folder missing"));
        Arrays.sort(all);
        int masters = 0;
        for (File f : all) {
            String n = f.getName();
            if (n.endsWith(".jpg")) {
                masters++;
                File webp = new File(dir, n.substring(0, n.length() - 4) + ".webp");
                if (!webp.isFile()) {
                    out.add(n + ": no .webp");
                    continue;
                }
                int[] jpg = jpegSize(Files.readAllBytes(f.toPath()));
                int[] size = webpSize(Files.readAllBytes(webp.toPath()));
                if (jpg == null || size == null) out.add(n + ": unreadable picture");
                else if (jpg[0] != size[0] || jpg[1] != size[1]) {
                    out.add(n + ": jpg " + jpg[0] + "x" + jpg[1] + " vs webp " + size[0] + "x" + size[1]);
                }
            } else if (n.endsWith(".webp") && !new File(dir, n.substring(0, n.length() - 5) + ".jpg").isFile()) {
                out.add(n + ": no .jpg master");
            }
        }
        if (masters == 0) out.add(dir.getName() + ": no .jpg masters");
        return out;
    }

    @Test
    public void everyBundledPictureHasAWebpOfTheSameSize() throws Exception {
        List<String> all = new ArrayList<>();
        int pictures = 0;
        for (String set : SETS) {
            File dir = new File("../../wallpapers/" + set);
            all.addAll(problems(dir));
            File[] jpgs = dir.listFiles((d, n) -> n.endsWith(".jpg"));
            pictures += jpgs == null ? 0 : jpgs.length;
        }
        if (!all.isEmpty()) fail("run python wallpapers/to-webp.py: " + all);
        assertEquals("a picture was dropped or added; change this number on purpose", 22, pictures);
    }

    @Test
    public void checkerCatchesAMissingWebpASizeMismatchAndAnOrphan() throws Exception {
        File dir = Files.createTempDirectory("set").toFile();
        // SOI, then SOF0 with height 1 and width 3 (the segment is padded to what the reader needs)
        byte[] jpg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xC0, 0, 17, 8, 0, 1, 0, 3, 3, 1, 0x22, 0, 2, 0x11, 1, 3, 0x11, 1};
        Files.write(new File(dir, "a.jpg").toPath(), jpg);
        assertTrue(problems(dir).get(0).contains("no .webp"));
        byte[] webp = new byte[30];
        System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, webp, 0, 4);
        System.arraycopy("WEBPVP8 ".getBytes(StandardCharsets.US_ASCII), 0, webp, 8, 8);
        webp[23] = (byte) 0x9D;
        webp[24] = 0x01;
        webp[25] = 0x2A;
        webp[26] = 4;
        webp[28] = 1;
        Files.write(new File(dir, "a.webp").toPath(), webp);
        assertTrue(problems(dir).get(0).contains("jpg 3x1 vs webp 4x1"));
        Files.write(new File(dir, "orphan.webp").toPath(), webp);
        assertTrue(problems(dir).toString().contains("orphan.webp: no .jpg master"));
        assertNull(webpSize(new byte[40]));
        assertNull(jpegSize(new byte[40]));
    }
}
