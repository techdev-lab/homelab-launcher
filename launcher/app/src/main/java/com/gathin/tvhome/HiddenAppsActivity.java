package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hide apps from the home screen, picked with the remote: OK on an app hides it or shows it again. A hidden app stays
 * installed and opens from anywhere else. Saved on the TV, so it survives a theme push; "Use the theme's list" goes
 * back to the theme's hidden_apps. The rules are in {@link HiddenApps}.
 */
public final class HiddenAppsActivity extends Activity {
    private SharedPreferences choices;
    private List<String> hidden = new ArrayList<>();
    private final List<TextView> rows = new ArrayList<>();
    private final List<String> rowPackages = new ArrayList<>();
    private final List<String> rowLabels = new ArrayList<>();
    private TextView title;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Language.wrap(base));
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_hidden_apps);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        hidden = new ArrayList<>(Config.load(this).hiddenApps);
        title = findViewById(R.id.hiddenTitle);
        findViewById(R.id.hiddenReset).setOnClickListener(v -> {
            choices.edit().remove(Config.HIDDEN_APPS_KEY).apply();
            hidden = new ArrayList<>(Config.load(this).hiddenApps);
            setResult(RESULT_OK);
            refresh();
        });

        boolean ar = "ar".equals(getResources().getConfiguration().getLocales().get(0).getLanguage());
        Map<String, String> arabic = ar ? NamesAr.get(this).apps : new HashMap<>();
        PackageManager pm = getPackageManager();
        List<ResolveInfo> found = pm.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0);
        Map<String, String> byPackage = new HashMap<>();
        for (ResolveInfo ri : found) {
            if (ri.activityInfo == null) continue;
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || byPackage.containsKey(pkg)) continue;
            String name = arabic.get(pkg);
            if (name == null) name = String.valueOf(ri.loadLabel(pm));
            byPackage.put(pkg, Labels.clean(name, pkg));
        }
        List<String> packages = new ArrayList<>(byPackage.keySet());
        Collections.sort(packages, (a, b) -> byPackage.get(a).compareToIgnoreCase(byPackage.get(b)));
        Map<String, Integer> sameLabel = new HashMap<>();
        for (String label : byPackage.values()) {
            String key = label.toLowerCase();
            Integer n = sameLabel.get(key);
            sameLabel.put(key, n == null ? 1 : n + 1);
        }
        LinearLayout list = findViewById(R.id.hiddenList);
        LayoutInflater inflater = getLayoutInflater();
        for (String pkg : packages) {
            String label = byPackage.get(pkg);
            // Two apps with the same name: show the package too, or the rows cannot be told apart.
            if (sameLabel.get(label.toLowerCase()) > 1) label = label + " (" + pkg + ")";
            TextView row = (TextView) inflater.inflate(R.layout.item_club, list, false);
            row.setOnClickListener(v -> toggle(pkg));
            list.addView(row);
            rows.add(row);
            rowPackages.add(pkg);
            rowLabels.add(label);
        }
        refresh();
        if (!rows.isEmpty()) rows.get(0).requestFocus();
    }

    private void toggle(String pkg) {
        hidden = HiddenApps.toggle(hidden, pkg);
        // An empty string is kept on purpose: "nothing hidden" chosen on the TV also overrides the theme's list.
        choices.edit().putString(Config.HIDDEN_APPS_KEY, HiddenApps.toCsv(hidden)).apply();
        setResult(RESULT_OK);
        refresh();
    }

    private void refresh() {
        title.setText(getString(R.string.hidden_apps_state, hidden.size()));
        for (int i = 0; i < rows.size(); i++) {
            boolean isHidden = hidden.contains(rowPackages.get(i));
            rows.get(i).setSelected(isHidden);
            rows.get(i).setText(isHidden ? getString(R.string.hidden_apps_row_hidden, rowLabels.get(i)) : rowLabels.get(i));
        }
    }
}
