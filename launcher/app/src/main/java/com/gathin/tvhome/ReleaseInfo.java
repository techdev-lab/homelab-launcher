package com.gathin.tvhome;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version rules for the update check: reads a three-part number from a tag or versionName, orders two of them and says
 * whether a released one is newer than the running build. Pure Java so JVM tests can run it without Android.
 *
 * {@link #parseVersion(String)} takes the first three-part number it finds and ignores the text around it, so a suffix
 * such as {@code -beta4} or {@code -online} is neither filtered out nor compared.
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

    private ReleaseInfo() {
    }
}
