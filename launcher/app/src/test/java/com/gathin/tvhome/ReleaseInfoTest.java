package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The release-check rules: tag parsing and ordering. */
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
}
