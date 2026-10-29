package com.gathin.tvhome;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

/**
 * Screensaver: the launcher's thin clock over a slow ambient glow, with the Hijri date, the next prayer and a
 * hairline that fills once a minute. The block glides (no jump, no fade) to a new spot every 20 seconds and the
 * glow follows it and slowly changes colour, so nothing burns in. Arabic on an Arabic screen, in IBM Plex Sans
 * Arabic like the home screen's cards. No network, no images: cheap enough for a 1 GB TV.
 */
public class ClockDream extends DreamService {

    private static final long GLIDE_EVERY_MS = 20_000L;
    private static final long GLIDE_MS = 9_000L;
    private static final long COLOUR_EVERY_MS = 60_000L;
    /** Deep, low-brightness glow colours; the glow is drawn at partial alpha on black. */
    private static final int[] PALETTE = {0xFF2B5CFF, 0xFF00A39A, 0xFF8A3FFC, 0xFFE0661B, 0xFFD6336C, 0xFFB8862B};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private View content;
    private View parentView;
    private GradientDrawable glow;
    private int colorIndex;
    private int glowColor;
    private View minuteLine;
    private TextView hijriLine;
    private TextView prayerLine;
    private Interests.Settings settings = Interests.Settings.NONE;
    private boolean arabic;
    private float density;
    /** The layout's TextClocks and their own patterns {12 hour, 24 hour}, see formatClocks. */
    private final List<TextClock> clocks = new ArrayList<>();
    private final List<CharSequence[]> clockPatterns = new ArrayList<>();

    private final Runnable glide = new Runnable() {
        @Override
        public void run() {
            glideClock();
            handler.postDelayed(this, GLIDE_EVERY_MS);
        }
    };

    private final Runnable colour = new Runnable() {
        @Override
        public void run() {
            shiftGlow();
            handler.postDelayed(this, COLOUR_EVERY_MS);
        }
    };

    /** On the minute: new texts, and the hairline starts filling again. */
    private final Runnable minute = new Runnable() {
        @Override
        public void run() {
            updateTexts();
            startMinuteLine();
            handler.postDelayed(this, msToNextMinute());
        }
    };

    /** The home screen's language, not the TV's: Android starts a dream with the system language only. */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Language.wrap(base));
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);   // any key wakes the TV back to where it was
        setFullscreen(true);
        setScreenBright(false);
        setContentView(R.layout.dream_clock);
        density = getResources().getDisplayMetrics().density;
        arabic = "ar".equals(getResources().getConfiguration().getLocales().get(0).getLanguage());
        try {
            settings = Config.load(this).interests;
        } catch (Exception e) {
            settings = Interests.Settings.NONE;
        }

        content = findViewById(R.id.dreamContent);
        collectClocks(content);
        formatClocks();
        parentView = (View) content.getParent();
        View ambient = findViewById(R.id.dreamAmbient);
        glow = new GradientDrawable();
        glow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        ambient.setBackground(glow);
        ambient.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) ->
                glow.setGradientRadius((r - l) * 0.55f));
        colorIndex = random.nextInt(PALETTE.length);
        setGlow(PALETTE[colorIndex]);

        Typeface plex = getResources().getFont(R.font.plex_arabic);
        Typeface plexLight = getResources().getFont(R.font.plex_arabic_light);
        // The date is the block's second child in every dream_clock layout (the clock row is the first).
        View date = ((LinearLayout) content).getChildAt(1);
        if (date instanceof TextView && arabic) {
            ((TextView) date).setTypeface(plex);
            ((TextView) date).setLetterSpacing(0f);
        }
        addExtras((LinearLayout) content, plexLight);
    }

    /** The hairline, the Hijri date and the next prayer, under the clock and date of the layout. */
    private void addExtras(LinearLayout box, Typeface font) {
        int gravity = arabic ? Gravity.RIGHT : Gravity.LEFT;
        minuteLine = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, (int) (2 * density)));
        lp.topMargin = (int) (14 * density);
        lp.bottomMargin = (int) (12 * density);
        minuteLine.setLayoutParams(lp);
        minuteLine.setScaleX(0f);
        box.addView(minuteLine);

        hijriLine = line(font, 24, 0xD9E9D8BE, gravity);
        prayerLine = line(font, 18, 0x99FFFFFF, gravity);
        box.addView(hijriLine);
        box.addView(prayerLine);
    }

    private TextView line(Typeface font, float sp, int color, int gravity) {
        TextView t = new TextView(this);
        t.setTypeface(font);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(gravity);
        t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        t.setSingleLine(true);
        t.setIncludeFontPadding(false);
        t.setPadding(0, (int) (4 * density), 0, (int) (4 * density));
        // Own width, not the clock's: "Thu, 11 Jumada al-Ula 1448 AH" is wider than the clock and was cut off.
        // The block grows to the widest line, and the hairline (MATCH_PARENT) follows it.
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = gravity;
        t.setLayoutParams(lp);
        return t;
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        updateTexts();
        content.setAlpha(0f);
        content.post(() -> {
            placeAt(randomSpot());
            content.animate().alpha(1f).setDuration(1500).start();
            startMinuteLine();
        });
        handler.postDelayed(glide, GLIDE_EVERY_MS);
        handler.postDelayed(colour, COLOUR_EVERY_MS);
        handler.postDelayed(minute, msToNextMinute());
    }

    @Override
    public void onDreamingStopped() {
        stopAll();
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        stopAll();
        super.onDetachedFromWindow();
    }

    private void stopAll() {
        handler.removeCallbacks(glide);
        handler.removeCallbacks(colour);
        handler.removeCallbacks(minute);
        if (content != null) content.animate().cancel();
        if (minuteLine != null) minuteLine.animate().cancel();
    }

    private void collectClocks(View v) {
        if (v instanceof TextClock) {
            TextClock c = (TextClock) v;
            clocks.add(c);
            clockPatterns.add(new CharSequence[]{c.getFormat12Hour(), c.getFormat24Hour()});
        } else if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) collectClocks(g.getChildAt(i));
        }
    }

    /**
     * The clock, the am/pm mark and the date, formatted here in the dream's language. A TextClock formats with the
     * process default locale each time it ticks, and Android puts the TV's language back there, which left the date
     * in English on an Arabic screen. The finished text goes back into the clock as a quoted literal, so it cannot
     * change until the next call (every minute).
     */
    private void formatClocks() {
        Locale.setDefault(getResources().getConfiguration().getLocales().get(0));
        boolean is24 = DateFormat.is24HourFormat(this);
        Calendar now = Calendar.getInstance();
        for (int i = 0; i < clocks.size(); i++) {
            CharSequence pattern = clockPatterns.get(i)[is24 ? 1 : 0];
            String text = pattern == null || pattern.length() == 0 ? "" : DateFormat.format(pattern, now).toString();
            clocks.get(i).setFormat12Hour(Language.quote(text));
            clocks.get(i).setFormat24Hour(Language.quote(text));
        }
    }

    private void updateTexts() {
        formatClocks();
        hijriLine.setText(Hijri.format(this, Hijri.today(), arabic));
        String p = nextPrayer();
        prayerLine.setText(p);
        prayerLine.setVisibility(p.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /** "Asr in 1:20 · City" (R.string.prayer_next) from the home screen's prayer settings; empty when they are off. */
    private String nextPrayer() {
        if (!settings.prayer()) return "";
        Calendar now = Calendar.getInstance();
        boolean ramadan = Hijri.isRamadan(Hijri.today());
        double tz = TimeZone.getDefault().getOffset(now.getTimeInMillis()) / 3_600_000.0;
        double[] t = settings.prayerTimes(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH), tz, ramadan);
        if (t == null) return getString(R.string.prayer_method_unknown, settings.prayerMethodKey);
        double nowH = now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE) / 60.0
                + now.get(Calendar.SECOND) / 3600.0;
        String[] names = getResources().getStringArray(R.array.prayer_names);
        int next = Prayer.next(t, nowH);
        String name, left;
        if (next >= 0) {
            name = names[next];
            left = Prayer.countdown(nowH, t[next]);
        } else {
            Calendar tm = (Calendar) now.clone();
            tm.add(Calendar.DAY_OF_MONTH, 1);
            // Tomorrow's own UTC offset, expressed on today's clock (see MainActivity.prayerCard).
            double tzTomorrow = TimeZone.getDefault().getOffset(tm.getTimeInMillis()) / 3_600_000.0;
            double fajr = Prayer.onTodaysClock(settings.prayerTimes(tm.get(Calendar.YEAR), tm.get(Calendar.MONTH) + 1,
                    tm.get(Calendar.DAY_OF_MONTH), tzTomorrow, ramadan)[Prayer.FAJR], tz, tzTomorrow);
            name = names[Prayer.FAJR];
            left = Prayer.countdown(nowH, fajr + 24);
        }
        String city = settings.city(arabic);
        String where = city.isEmpty() ? "" : " · " + city;
        return getString(R.string.prayer_next, name, left) + where;
    }

    /** The hairline grows across the block over the rest of this minute, in the glow's colour. */
    private void startMinuteLine() {
        minuteLine.animate().cancel();
        minuteLine.setBackgroundColor(lighter(glowColor));
        minuteLine.setPivotX(arabic ? minuteLine.getWidth() : 0f);
        long left = msToNextMinute();
        minuteLine.setScaleX(1f - left / 60_000f);
        minuteLine.animate().scaleX(1f).setDuration(left).setInterpolator(new LinearInterpolator()).start();
    }

    private static long msToNextMinute() {
        long now = System.currentTimeMillis();
        return 60_000L - (now % 60_000L) + 50;
    }

    private void glideClock() {
        float[] to = randomSpot();
        float fromX = content.getX(), fromY = content.getY();
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(GLIDE_MS);
        // Ease in and out, slow at both ends: it reads as floating rather than sliding.
        a.setInterpolator(new PathInterpolator(0.45f, 0f, 0.55f, 1f));
        a.addUpdateListener(v -> {
            float f = (float) v.getAnimatedValue();
            placeAt(new float[]{fromX + (to[0] - fromX) * f, fromY + (to[1] - fromY) * f});
        });
        a.start();
    }

    /** A spot that keeps the whole block on screen with a margin, never right next to the last one. */
    private float[] randomSpot() {
        int maxX = Math.max(0, parentView.getWidth() - content.getWidth());
        int maxY = Math.max(0, parentView.getHeight() - content.getHeight());
        float x = 0, y = 0;
        for (int i = 0; i < 6; i++) {
            x = maxX * (0.08f + 0.84f * random.nextFloat());
            y = maxY * (0.08f + 0.84f * random.nextFloat());
            if (Math.hypot(x - content.getX(), y - content.getY()) > Math.min(maxX, maxY) * 0.3f) break;
        }
        return new float[]{x, y};
    }

    private void placeAt(float[] p) {
        content.setX(p[0]);
        content.setY(p[1]);
        if (parentView.getWidth() > 0 && parentView.getHeight() > 0) {
            glow.setGradientCenter((p[0] + content.getWidth() / 2f) / parentView.getWidth(),
                    (p[1] + content.getHeight() / 2f) / parentView.getHeight());
        }
    }

    private void shiftGlow() {
        int from = glowColor;
        colorIndex = (colorIndex + 1) % PALETTE.length;
        ValueAnimator anim = ValueAnimator.ofObject(new ArgbEvaluator(), from, PALETTE[colorIndex]);
        anim.setDuration(8000);
        anim.addUpdateListener(a -> setGlow((int) a.getAnimatedValue()));
        anim.start();
    }

    private void setGlow(int color) {
        glowColor = color;
        int c = color & 0x00FFFFFF;
        glow.setColors(new int[]{0x40000000 | c, 0x14000000 | c, 0x00000000 | c});
        if (minuteLine != null) minuteLine.setBackgroundColor(lighter(color));
    }

    /** The glow colour lifted toward white for the thin line, which must read on black. */
    private static int lighter(int color) {
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        r = r + (255 - r) / 2;
        g = g + (255 - g) / 2;
        b = b + (255 - b) / 2;
        return 0xB3000000 | (r << 16) | (g << 8) | b;
    }
}
