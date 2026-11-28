package com.gathin.tvhome;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the latest release of this launcher from a GitHub releases API answer: its tag, its versionName and whether it
 * is newer than the running build. Pure Java so JVM tests can feed it real answers without Android.
 *
 * A tag looks like {@code v3.2.0} or {@code 3.2.0}; anything that does not carry three numbers (a beta tag such as
 * {@code 3.1.0-beta4}) is skipped by {@link #latest(String)}, because an update card should point at a full release.
 */
final class ReleaseInfo {

    /** Tag / versionName → [major, minor, patch], or null when the text carries no three-part number. */
    static int[] parseVersion(String text) {
        if (text == null) return null;
        Matcher m = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)").matcher(text);
        if (!m.find()) return null;
        try {
            return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))};
        } catch (NumberFormatException e) {
            return null;   // absurd digits in a crafted tag: treated as no version
        }
    }

    /** Negative when a is older, 0 when equal, positive when a is newer. Missing parts read as 0. */
    static int compare(int[] a, int[] b) {
        for (int i = 0; i < 3; i++) {
            int x = a != null && i < a.length ? a[i] : 0;
            int y = b != null && i < b.length ? b[i] : 0;
            if (x != y) return Integer.compare(x, y);
        }
        return 0;
    }

    /** True when the running versionName is older than the released one; either side unreadable means "not newer". */
    static boolean isNewer(String latestVersionName, String currentVersionName) {
        int[] latest = parseVersion(latestVersionName);
        int[] current = parseVersion(currentVersionName);
        if (latest == null || current == null) return false;
        return compare(latest, current) > 0;
    }

    /**
     * The newest full release from a GitHub {@code /releases} JSON array given as text: scans the objects in order and
     * keeps the highest three-part tag among those that are not drafts or prereleases. Returns null when nothing fits
     * (no network answer, a changed shape, only betas), so the caller simply says there is nothing to report.
     */
    static String latest(String releasesJson) {
        if (releasesJson == null) return null;
        String best = null;
        int[] bestVersion = null;
        Matcher objs = Pattern.compile("\\{[^{}]*\\}", Pattern.DOTALL).matcher(releasesJson);
        while (objs.find()) {
            String obj = objs.group();
            if (obj.contains("\"draft\":true") || obj.contains("\"prerelease\":true")) continue;
            Matcher tag = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").matcher(obj);
            if (!tag.find()) continue;
            int[] version = parseVersion(tag.group(1));
            if (version == null) continue;   // a beta or odd tag: never offered as an update
            if (bestVersion == null || compare(version, bestVersion) > 0) {
                bestVersion = version;
                best = tag.group(1);
            }
        }
        return best;
    }

    private ReleaseInfo() {
    }
}
