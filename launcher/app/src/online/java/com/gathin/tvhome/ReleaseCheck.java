package com.gathin.tvhome;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

/**
 * Online build only: asks GitHub for the releases of this repository and returns the newest full one (no drafts, no
 * betas) as a tag like {@code 3.2.0}, or null on any failure. HTTPS to one fixed host, short timeouts, a capped body,
 * no redirects followed — the same rules as {@link Feeds}. Called off the main thread; a TV without the network simply
 * hears "no update found".
 */
final class ReleaseCheck {

    static boolean available() {
        return true;
    }

    private static final String TAG = "TvHome.Release";
    private static final String HOST = "api.github.com";
    /** The project's repository on GitHub; docs/RELEASING.md publishes the tags this reads. */
    private static final String PATH = "/repos/techdev-lab/tvhome-launcher/releases?per_page=15";
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
            JSONArray releases = new JSONArray(body);
            String best = null;
            int[] bestVersion = null;
            for (int i = 0; i < releases.length(); i++) {
                JSONObject r = releases.optJSONObject(i);
                if (r == null || r.optBoolean("draft") || r.optBoolean("prerelease")) continue;
                String tag = r.optString("tag_name", "");
                int[] version = ReleaseInfo.parseVersion(tag);
                if (version == null) continue;   // a beta tag is never offered as an update
                if (bestVersion == null || ReleaseInfo.compare(version, bestVersion) > 0) {
                    bestVersion = version;
                    best = tag;
                }
            }
            return best;
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
