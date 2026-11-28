package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The release-check rules: tag parsing, ordering and picking the newest full release from a GitHub answer. */
public class ReleaseInfoTest {

    @Test
    public void parsesThreePartVersionsFromTagsAndNames() {
        assertEquals(3, ReleaseInfo.parseVersion("v3.2.0").length);
        assertTrue(ReleaseInfo.compare(ReleaseInfo.parseVersion("v3.2.0"), new int[]{3, 2, 0}) == 0);
        assertTrue(ReleaseInfo.compare(ReleaseInfo.parseVersion("3.10.0"), ReleaseInfo.parseVersion("3.9.0")) > 0);
        assertTrue(ReleaseInfo.compare(ReleaseInfo.parseVersion("4.0.0"), ReleaseInfo.parseVersion("3.99.99")) > 0);
    }

    @Test
    public void twoPartOrEmptyTextsCarryNoVersion() {
        assertNull(ReleaseInfo.parseVersion("3.1"));
        assertNull(ReleaseInfo.parseVersion("latest"));
        assertNull(ReleaseInfo.parseVersion(null));
    }

    @Test
    public void newerMeansStrictlyNewer() {
        assertTrue(ReleaseInfo.isNewer("3.2.0", "3.1.1"));
        assertFalse(ReleaseInfo.isNewer("3.1.1", "3.1.1"));
        assertFalse(ReleaseInfo.isNewer("3.1.1", "3.2.0"));
        // an "-online" suffix is not part of the number: both sides read their three parts
        assertTrue(ReleaseInfo.isNewer("3.2.0", "3.1.1-online"));
        assertFalse(ReleaseInfo.isNewer("garbage", "3.1.1"));
        assertFalse(ReleaseInfo.isNewer("3.2.0", null));
    }

    @Test
    public void picksTheHighestFullReleaseAndSkipsDraftsPrereleasesAndBetas() {
        String json = "["
                + "{\"tag_name\":\"3.2.0-beta1\",\"prerelease\":true},"
                + "{\"tag_name\":\"3.1.0\",\"draft\":false,\"prerelease\":false},"
                + "{\"tag_name\":\"3.3.0\",\"draft\":true},"
                + "{\"tag_name\":\"3.2.0\",\"draft\":false,\"prerelease\":false},"
                + "{\"tag_name\":\"wip\",\"draft\":false}"
                + "]";
        assertEquals("3.2.0", ReleaseInfo.latest(json));
        assertEquals("3.2.0", ReleaseInfo.latest("[{\"tag_name\":\"3.1.0\"},{\"tag_name\":\"3.2.0\"}]"));
    }

    @Test
    public void emptyOrBrokenAnswersYieldNothing() {
        assertNull(ReleaseInfo.latest(null));
        assertNull(ReleaseInfo.latest(""));
        assertNull(ReleaseInfo.latest("[]"));
        assertNull(ReleaseInfo.latest("<html>rate limited</html>"));
        assertNull(ReleaseInfo.latest("[{\"tag_name\":\"nightly-2026\"}]"));   // no three-part number at all
    }
}
