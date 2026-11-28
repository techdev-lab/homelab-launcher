package com.gathin.tvhome;

import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

/**
 * Online build only: asks GitHub for the latest release of this repository and returns its tag, like {@code 3.2.0}, or
 * null on any failure. Drafts and prereleases are excluded by GitHub's {@code /releases/latest} endpoint, not by this
 * class. HTTPS to one fixed host, short timeouts, a capped body, no redirects followed — the same rules as
 * {@link Feeds}. Called off the main thread; a TV without the network simply hears that GitHub could not be reached.
 */
final class ReleaseCheck {

    static boolean available() {
        return true;
    }

    private static final String TAG = "TvHome.Release";
    private static final String HOST = "api.github.com";
    /** The project's repository on GitHub; docs/RELEASING.md publishes the tags this reads. */
    private static final String PATH = "/repos/techdev-lab/homelab-launcher/releases/latest";
    private static final int TIMEOUT_MS = 6_000;
    private static final int MAX_BYTES = 64 * 1024;

    static String latestTag() {
        try {
            HttpsURLConnection c = (HttpsURLConnection) new URL("https://" + HOST + PATH).openConnection();
            c.setConnectTimeout(TIMEOUT_MS);
            c.setReadTimeout(TIMEOUT_MS);
            c.setInstanceFollowRedirects(false);
            c.setRequestProperty("Accept", "application/vnd.github+json");
            c.setRequestProperty("User-Agent", "tvhome-launcher");
            if (c.getResponseCode() != 200) return null;
            String body = read(c.getInputStream());
            // Drafts and prereleases are excluded by GitHub's /releases/latest endpoint; one release fits under the cap.
            String tag = new JSONObject(body).optString("tag_name", "");
            return ReleaseInfo.parseVersion(tag) == null ? null : tag;
        } catch (Exception e) {
            Log.i(TAG, "release check failed: " + e);   // offline TV, rate limit, changed shape: stay quiet
            return null;
        }
    }

    private static String read(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n, total = 0;
        while ((n = in.read(buf)) > 0 && (total += n) <= MAX_BYTES) out.write(buf, 0, n);
        in.close();
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private ReleaseCheck() {
    }
}
