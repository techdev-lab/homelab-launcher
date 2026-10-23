package com.gathin.tvhome;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Arabic names for the Arabic screen (assets/names_ar.json): app labels by package, leagues by ESPN code and
 * teams by ESPN team id. Apps such as YouTube keep their English label in an Arabic system, and ESPN has no
 * Arabic, so without this an Arabic home screen showed English app, league and team names. Missing entries fall
 * back to the original name. Read once; nothing here touches the network.
 */
final class NamesAr {
    private static final String TAG = "TvHome.NamesAr";
    private static NamesAr instance;

    final Map<String, String> apps;
    final Map<String, String> leagues;
    final Map<String, String> teams;

    private NamesAr(Map<String, String> apps, Map<String, String> leagues, Map<String, String> teams) {
        this.apps = Collections.unmodifiableMap(apps);
        this.leagues = Collections.unmodifiableMap(leagues);
        this.teams = Collections.unmodifiableMap(teams);
    }

    static synchronized NamesAr get(Context ctx) {
        if (instance != null) return instance;
        Map<String, String> apps = new HashMap<>(), leagues = new HashMap<>(), teams = new HashMap<>();
        try (InputStream in = ctx.getAssets().open("names_ar.json")) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            for (int r; (r = in.read(chunk)) > 0; ) buf.write(chunk, 0, r);
            JSONObject root = new JSONObject(new String(buf.toByteArray(), StandardCharsets.UTF_8));
            copy(root.optJSONObject("apps"), apps);
            copy(root.optJSONObject("leagues"), leagues);
            copy(root.optJSONObject("teams"), teams);
        } catch (Exception e) {
            Log.w(TAG, "names_ar.json: " + e);
        }
        instance = new NamesAr(apps, leagues, teams);
        return instance;
    }

    private static void copy(JSONObject o, Map<String, String> out) {
        if (o == null) return;
        for (Iterator<String> it = o.keys(); it.hasNext(); ) {
            String k = it.next();
            String v = o.optString(k, "").trim();
            if (!v.isEmpty()) out.put(k, v);
        }
    }

    static String or(Map<String, String> names, String key, String fallback) {
        String v = key == null ? null : names.get(key);
        return v != null ? v : fallback;
    }
}
