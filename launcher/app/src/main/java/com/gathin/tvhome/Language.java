package com.gathin.tvhome;

import android.app.LocaleManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

/**
 * The language a component that Android starts on its own (the screensaver) should use. The home screen gets its
 * language from MainActivity.attachBaseContext and from Android's per-app language; a DreamService gets neither,
 * so without this it follows the TV's system language and shows English on an Arabic home screen.
 */
final class Language {

    private Language() {
    }

    /**
     * The language tag to use, or null to keep the TV's language. config.json "language" ("ar" or "en") wins, then
     * the launcher's own Android 13+ language (cmd locale set-app-locales), then the TV's.
     */
    static String pick(String configLanguage, String appLocaleTag) {
        if ("ar".equals(configLanguage) || "en".equals(configLanguage)) return configLanguage;
        if (appLocaleTag != null && !appLocaleTag.isEmpty()) return appLocaleTag;
        return null;
    }

    /**
     * Text as a literal in a TextClock/DateFormat pattern: quoted, with a quote inside doubled. Empty stays empty,
     * because an empty quoted pair would print one apostrophe.
     */
    static String quote(String text) {
        return text.isEmpty() ? "" : "'" + text.replace("'", "''") + "'";
    }

    /** The base context with that language applied (its resources, so layout-ar and values-ar come with it). */
    static Context wrap(Context base) {
        String tag = pick(Config.language(base), appLocaleTag(base));
        if (tag == null) return base;
        Locale locale = Locale.forLanguageTag(tag);
        // Date and number formatting reads the process default, not the context's locale (as in MainActivity).
        Locale.setDefault(locale);
        Configuration c = new Configuration(base.getResources().getConfiguration());
        c.setLocale(locale);
        return base.createConfigurationContext(c);
    }

    static String appLocaleTag(Context base) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null;
        LocaleManager manager = base.getSystemService(LocaleManager.class);
        if (manager == null) return null;
        LocaleList locales = manager.getApplicationLocales();
        return locales.isEmpty() ? null : locales.get(0).toLanguageTag();
    }
}
