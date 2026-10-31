package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** The drawing of a dock layout on a settings chip: a small screen with the dock in the accent colour (DockSketch). */
final class DockPreview extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float[][] cells;
    private final float density;

    DockPreview(Context context, String position, boolean rtl) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        cells = DockSketch.cells(position, rtl);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x66000000);
        rect.set(0, 0, w, h);
        canvas.drawRoundRect(rect, 6 * density, 6 * density, paint);
        for (float[] c : cells) {
            paint.setColor(DockSketch.isDock(c) ? 0xE6F2EEE6 : 0x59FFFFFF);
            rect.set(c[0] * w, c[1] * h, (c[0] + c[2]) * w, (c[1] + c[3]) * h);
            float r = 2 * density;
            canvas.drawRoundRect(rect, r, r, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(density);
        paint.setColor(0x40FFFFFF);
        rect.set(density / 2, density / 2, w - density / 2, h - density / 2);
        canvas.drawRoundRect(rect, 6 * density, 6 * density, paint);
    }
}
