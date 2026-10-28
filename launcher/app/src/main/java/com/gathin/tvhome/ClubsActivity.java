package com.gathin.tvhome;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Favourite clubs, picked with the remote from assets/clubs.json: up to {@link Interests#MAX_CLUBS}, in the order
 * picked, which is the order the club card shows them. Saved on the TV, so it survives a theme push; "Clear"
 * goes back to the theme's favorite_teams.
 */
public final class ClubsActivity extends Activity {
    private static final String TAG = "TvHome.Clubs";
    private SharedPreferences choices;
    private final List<String> picked = new ArrayList<>();
    private final List<TextView> rows = new ArrayList<>();
    private final List<String> rowIds = new ArrayList<>();
    private final List<String> rowNames = new ArrayList<>();
    private TextView title;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_clubs);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        Config config = Config.load(this);
        picked.addAll(config.interests.favoriteTeams);
        title = findViewById(R.id.clubsTitle);
        ((TextView) findViewById(R.id.clubsHint)).setText(getString(R.string.favorite_clubs_hint,
                Interests.MAX_CLUBS, config.interests.clubRotateSeconds));
        findViewById(R.id.clubsClear).setOnClickListener(v -> {
            choices.edit().remove(Config.FAVORITE_TEAMS_KEY).apply();
            picked.clear();
            picked.addAll(Config.load(this).interests.favoriteTeams);
            setResult(RESULT_OK);
            refresh();
        });
        boolean ar = "ar".equals(getResources().getConfiguration().getLocales().get(0).getLanguage());
        NamesAr names = NamesAr.get(this);
        LinearLayout list = findViewById(R.id.clubsList);
        LayoutInflater inflater = getLayoutInflater();
        try {
            JSONArray groups = new JSONObject(read()).optJSONArray("groups");
            for (int g = 0; groups != null && g < groups.length(); g++) {
                JSONObject group = groups.optJSONObject(g);
                if (group == null) continue;
                TextView header = (TextView) inflater.inflate(R.layout.item_club_group, list, false);
                String code = group.optString("code", "");
                String name = group.optString("name", code);
                String nameAr = group.optString("name_ar", "");
                header.setText(ar && !nameAr.isEmpty() ? nameAr : name);
                list.addView(header);
                JSONArray teams = group.optJSONArray("teams");
                for (int t = 0; teams != null && t < teams.length(); t++) {
                    JSONObject team = teams.optJSONObject(t);
                    String id = team == null ? "" : team.optString("id", "");
                    if (!Interests.validTeamId(id) || rowIds.contains(id)) continue;
                    String en = team.optString("name", id);
                    TextView row = (TextView) inflater.inflate(R.layout.item_club, list, false);
                    row.setOnClickListener(v -> toggle(id));
                    list.addView(row);
                    rows.add(row);
                    rowIds.add(id);
                    rowNames.add(ar ? NamesAr.or(names.teams, id, en) : en);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "clubs.json: " + e);
        }
        refresh();
        if (!rows.isEmpty()) rows.get(0).requestFocus();
    }

    private void toggle(String id) {
        if (picked.contains(id)) {
            picked.remove(id);
        } else if (picked.size() >= Interests.MAX_CLUBS) {
            Toast.makeText(this, getString(R.string.clubs_limit, Interests.MAX_CLUBS), Toast.LENGTH_SHORT).show();
            return;
        } else {
            picked.add(id);
        }
        // An empty string is kept on purpose: "no clubs" chosen on the TV also overrides the theme's list.
        choices.edit().putString(Config.FAVORITE_TEAMS_KEY, String.join(",", picked)).apply();
        setResult(RESULT_OK);
        refresh();
    }

    private void refresh() {
        title.setText(getString(R.string.favorite_clubs_state, picked.size(), Interests.MAX_CLUBS));
        for (int i = 0; i < rows.size(); i++) {
            int at = picked.indexOf(rowIds.get(i));
            rows.get(i).setSelected(at >= 0);
            rows.get(i).setText(at >= 0 ? "✓ " + (at + 1) + "   " + rowNames.get(i) : rowNames.get(i));
        }
    }

    private String read() throws Exception {
        try (InputStream in = getAssets().open("clubs.json")) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            for (int r; (r = in.read(chunk)) > 0; ) buf.write(chunk, 0, r);
            return new String(buf.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
