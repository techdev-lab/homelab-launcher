package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Guards for the fixes after the first TV check of 3.1.0 (docs/BATCH_S_SCENES.md, section 13). The drawing needs the
 * Android framework, so these read the sources, as HomeLifecycleSourceTest does; each failure message says what comes back.
 */
public class ScenesFixesSourceTest {

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    @Test
    public void aFailedSceneDecodeLetsTheSameSceneBeTriedAgain() throws IOException {
        String view = read("src/main/java/com/gathin/tvhome/SceneView.java");
        assertTrue("SceneView must clear shownId when a decode fails, or the screen stays without a scene for hours",
                view.contains("shownId = null"));
    }

    @Test
    public void theHeaderDividerIsHiddenInScenesAndExistsInEveryLayout() throws IOException {
        assertTrue(read("src/main/java/com/gathin/tvhome/MainActivity.java").contains("R.id.headerDivider"));
        for (String dir : new String[]{"layout", "layout-ar", "layout-iw"}) {
            String xml = read("src/main/res/" + dir + "/activity_main.xml");
            assertEquals(dir + " must name the divider once", 1, count(xml, "@+id/headerDivider"));
        }
    }

    @Test
    public void theDateLinesGetAnEdgeInScenes() throws IOException {
        String home = read("src/main/java/com/gathin/tvhome/MainActivity.java");
        assertTrue("the date lines over the big digits need setShadowLayer in the Scenes look",
                home.contains("setShadowLayer(edge"));
        assertFalse(home.isEmpty());
    }

    @Test
    public void everyChangelogForTheCurrentVersionCodeExistsAndFitsTheStoreLimit() throws IOException {
        String gradle = read("build.gradle");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("versionCode\\s+(\\d+)").matcher(gradle);
        assertTrue(m.find());
        String code = m.group(1);
        for (String locale : new String[]{"en-US", "ar"}) {
            java.nio.file.Path p = Paths.get("../../fastlane/metadata/android/" + locale + "/changelogs/" + code + ".txt");
            assertTrue(p + " must exist for versionCode " + code, Files.exists(p));
            int bytes = Files.readAllBytes(p).length;
            assertTrue(p + " is " + bytes + " bytes; the store limit is 500", bytes <= 500);
        }
    }

    private static int count(String s, String what) {
        int n = 0;
        for (int i = s.indexOf(what); i >= 0; i = s.indexOf(what, i + 1)) n++;
        return n;
    }
}
