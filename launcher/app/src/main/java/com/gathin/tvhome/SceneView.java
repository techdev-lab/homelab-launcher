package com.gathin.tvhome;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The Scenes look (docs/BATCH_S_SCENES.md): one view that draws the scene picture, the big clock whose digits show the
 * blurred copy of that picture, and the scenery over the bottom of the digits. Everything is drawn in picture space
 * (1920 x 1080) and scaled to the screen with {@link SceneGeometry#cover}. One scene is decoded at a time: the picture and
 * its glass copy as RGB_565, and of the cut-out only the rows the clock can touch. On a small-memory TV the old scene is
 * released before the next one is decoded (no cross-fade); elsewhere the old one stays for the length of the fade.
 */
final class SceneView extends View {
    /** One decoded scene. */
    private static final class Layer {
        final SceneList.Scene scene;
        Bitmap background, glass, cutBand;
        BitmapShader glassShader;

        Layer(SceneList.Scene scene) {
            this.scene = scene;
        }

        void release() {
            if (background != null) background.recycle();
            if (glass != null) glass.recycle();
            if (cutBand != null) cutBand.recycle();
            background = glass = cutBand = null;
            glassShader = null;
        }

        boolean ready() {
            return background != null && glass != null;
        }
    }

    private final AssetManager assets;
    private final Typeface typeface;
    private final boolean smallMemory, animations, reducedMotion;
    private final String digits, scrimSetting;
    private final ExecutorService decoder = Executors.newSingleThreadExecutor();
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint suffixPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scrimPaint = new Paint();
    private final Rect bounds = new Rect();

    private Layer current, previous;
    private float fade = 1f;
    private int wanted = -1;          // sequence number of the newest request: older decodes are dropped
    private String shownId = "";
    private boolean is24;
    private long clockMillis = System.currentTimeMillis();
    private boolean released;

    SceneView(Context context, Typeface typeface, boolean smallMemory, boolean animations, boolean reducedMotion,
              String digits, String scrimSetting) {
        super(context);
        this.assets = context.getAssets();
        this.typeface = typeface;
        this.smallMemory = smallMemory;
        this.animations = animations;
        this.reducedMotion = reducedMotion;
        this.digits = digits;
        this.scrimSetting = scrimSetting;
        fillPaint.setTypeface(typeface);
        rimPaint.setTypeface(typeface);
        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setColor(0x66FFFFFF);
        suffixPaint.setTypeface(typeface);
        suffixPaint.setColor(0xB3FFFFFF);
        setWillNotDraw(false);
    }

    /** The time to draw; the caller calls this on the minute. */
    void setClock(long millis, boolean is24Hour) {
        clockMillis = millis;
        is24 = is24Hour;
        invalidate();
    }

    /** Shows the scene {@code id}: a no-op when it is already on screen. The decode runs on a worker thread. */
    void show(String id) {
        if (released || id == null || id.equals(shownId)) return;
        shownId = id;
        final int ticket = ++wanted;
        final boolean fadeNow = SceneFade.useFade(smallMemory, animations, reducedMotion) && current != null;
        if (!fadeNow) {
            // One scene in memory: the old one goes first, the next one is drawn when it is ready.
            releaseLayer(previous);
            previous = null;
            releaseLayer(current);
            current = null;
            invalidate();
        }
        final SceneList.Scene scene = SceneList.of(id);
        decoder.execute(() -> {
            Layer layer = decode(scene);
            post(() -> {
                if (released || ticket != wanted || layer == null || !layer.ready()) {
                    releaseLayer(layer);
                    return;
                }
                if (fadeNow) {
                    releaseLayer(previous);
                    previous = current;
                    current = layer;
                    fade = 0f;
                    animate(layer);
                } else {
                    current = layer;
                    fade = 1f;
                }
                invalidate();
            });
        });
    }

    private void animate(Layer layer) {
        final long start = System.currentTimeMillis();
        post(new Runnable() {
            @Override
            public void run() {
                if (released || current != layer) return;
                fade = Math.min(1f, (System.currentTimeMillis() - start) / (float) SceneFade.FADE_MS);
                if (fade >= 1f) {
                    releaseLayer(previous);
                    previous = null;
                } else {
                    postOnAnimation(this);
                }
                invalidate();
            }
        });
    }

    /** Frees every bitmap (Home is stopped or gone); {@link #show} decodes again when Home comes back. */
    void releaseBitmaps() {
        wanted++;
        shownId = "";
        releaseLayer(previous);
        releaseLayer(current);
        previous = current = null;
    }

    void destroy() {
        released = true;
        releaseBitmaps();
        decoder.shutdownNow();
    }

    private static void releaseLayer(Layer l) {
        if (l != null) l.release();
    }

    private Layer decode(SceneList.Scene scene) {
        Layer layer = new Layer(scene);
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inPreferredConfig = Bitmap.Config.RGB_565;   // no alpha needed, half the memory
            layer.background = decodeAsset("scenes/" + scene.id + "/bg.webp", o);
            layer.glass = decodeAsset("scenes/" + scene.id + "/glass.webp", o);
            if (layer.glass != null) {
                layer.glassShader = new BitmapShader(layer.glass, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                Matrix m = new Matrix();
                m.setScale(SceneList.PICTURE_W / (float) SceneGeometry.GLASS_W,
                        SceneList.PICTURE_H / (float) SceneGeometry.GLASS_H);
                layer.glassShader.setLocalMatrix(m);
            }
            layer.cutBand = decodeBand(scene);
            return layer;
        } catch (Exception | OutOfMemoryError e) {
            // A bad file or no memory must never take the home screen down: the colour behind stays.
            layer.release();
            return null;
        }
    }

    private Bitmap decodeAsset(String path, BitmapFactory.Options o) throws IOException {
        try (InputStream in = assets.open(path)) {
            return BitmapFactory.decodeStream(in, null, o);
        }
    }

    /** Only the rows of the cut-out the clock can touch; null when the scene has none or the decode fails. */
    private Bitmap decodeBand(SceneList.Scene scene) {
        int[] band = SceneGeometry.band(scene);
        if (band == null) return null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try (InputStream in = assets.open("scenes/" + scene.id + "/cut.webp")) {
            BitmapRegionDecoder region = BitmapRegionDecoder.newInstance(in, false);
            try {
                return region.decodeRegion(new Rect(0, band[0], SceneList.PICTURE_W, band[1]), o);
            } finally {
                region.recycle();
            }
        } catch (Exception | OutOfMemoryError e) {
            return null;   // without the cut-out the digits simply sit in front of the scenery
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        float[] c = SceneGeometry.cover(w, h);
        canvas.save();
        canvas.clipRect(0, 0, w, h);
        canvas.translate(c[1], c[2]);
        canvas.scale(c[0], c[0]);
        if (previous != null && previous.ready()) drawLayer(canvas, previous, 255);
        if (current != null && current.ready()) {
            drawLayer(canvas, current, Math.round(255 * fade));
            drawScrim(canvas, current);
        }
        canvas.restore();
    }

    private void drawLayer(Canvas canvas, Layer l, int alpha) {
        bitmapPaint.setAlpha(alpha);
        canvas.drawBitmap(l.background, 0, 0, bitmapPaint);
        drawClock(canvas, l, alpha);
        int[] band = SceneGeometry.band(l.scene);
        if (l.cutBand != null && band != null) canvas.drawBitmap(l.cutBand, 0, band[0], bitmapPaint);
    }

    private void drawClock(Canvas canvas, Layer l, int alpha) {
        SceneList.Scene s = l.scene;
        String text = SceneClockText.time(clockMillis, TimeZone.getDefault(), is24, digits);
        fillPaint.setTextSize(s.clockSize);
        rimPaint.setTextSize(s.clockSize);
        rimPaint.setStrokeWidth(Math.max(1.5f, s.clockSize * 0.006f));
        fillPaint.setShader(l.glassShader);
        fillPaint.setAlpha(alpha);
        rimPaint.setAlpha(Math.round(alpha * 0.4f));
        fillPaint.getTextBounds(text, 0, text.length(), bounds);
        float width = fillPaint.measureText(text);
        float x = SceneGeometry.clockLeft(s, width);
        float baseline = s.clockTop - bounds.top;   // the top of the digits lands on the top of the clock box
        if (s.waterLine > 0) {
            // Above the waterline the clock as it is, below it the same clock mirrored and fainter.
            canvas.save();
            canvas.clipRect(0, 0, SceneList.PICTURE_W, s.waterLine);
            canvas.drawTextRun(text, 0, text.length(), 0, text.length(), x, baseline, false, fillPaint);
            canvas.drawTextRun(text, 0, text.length(), 0, text.length(), x, baseline, false, rimPaint);
            canvas.restore();
            canvas.save();
            canvas.clipRect(0, s.waterLine, SceneList.PICTURE_W, SceneList.PICTURE_H);
            canvas.translate(0, 2f * s.waterLine);
            canvas.scale(1f, -1f);
            fillPaint.setAlpha(Math.round(alpha * 0.55f));
            canvas.drawTextRun(text, 0, text.length(), 0, text.length(), x, baseline, false, fillPaint);
            canvas.restore();
        } else {
            canvas.drawTextRun(text, 0, text.length(), 0, text.length(), x, baseline, false, fillPaint);
            canvas.drawTextRun(text, 0, text.length(), 0, text.length(), x, baseline, false, rimPaint);
        }
        String suffix = SceneClockText.suffix(clockMillis, TimeZone.getDefault(), is24);
        if (!suffix.isEmpty()) {
            suffixPaint.setTextSize(s.clockSize * 0.16f);
            suffixPaint.setAlpha(Math.round(alpha * 0.7f));
            canvas.drawTextRun(suffix, 0, suffix.length(), 0, suffix.length(), x + width + s.clockSize * 0.04f,
                    baseline, false, suffixPaint);
        }
    }

    private void drawScrim(Canvas canvas, Layer l) {
        if (!SceneScrim.drawn(scrimSetting, l.scene.bright, HomeLook.SCENES)) return;
        if (scrimPaint.getShader() == null) {
            int top = SceneScrim.TOP_ALPHA_PERCENT * 255 / 100;
            scrimPaint.setShader(new LinearGradient(0, 0, 0, SceneGeometry.scrimHeight(), top << 24, 0,
                    Shader.TileMode.CLAMP));
        }
        scrimPaint.setAlpha(Math.round(255 * fade));
        canvas.drawRect(0, 0, SceneList.PICTURE_W, SceneGeometry.scrimHeight(), scrimPaint);
    }
}
