package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Draws the plate behind the first (dock) row and adds a gap below it. {@link DockStyle#BAND} is the flat tvOS style
 * band of 2.9 (the default); {@link DockStyle#FLOATING} is translucent glass in the colour of the focused app.
 */
public class DockDecoration extends RecyclerView.ItemDecoration {

    private final int dockSize;
    /** The right-hand dock column (a vertical list of the dock slots): no gap under every tile, only at the ends. */
    private final boolean column;
    private final float density;
    private final String style;
    private final float radius;
    private final Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    /** Ambient colour of the focused app (0xRRGGBB); negative until there is one. Only the floating plate uses it. */
    private int ambientRgb = -1;
    private int shaderTop, shaderBottom;
    private float shaderFrom = Float.NaN, shaderTo = Float.NaN;

    /** {@code tileCornerFactor} is {@link TileShape#factor}: a softer tile corner makes the plate rounder in step. */
    DockDecoration(int dockSize, float density, String style, float tileCornerFactor) {
        this(dockSize, density, style, tileCornerFactor, false);
    }

    DockDecoration(int dockSize, float density, String style, float tileCornerFactor, boolean column) {
        this.column = column;
        this.dockSize = dockSize;
        this.density = density;
        this.style = style;
        this.radius = DockStyle.radiusDp(style, tileCornerFactor) * density;
        band.setColor(DockStyle.fillTop(style, -1));
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(density);
        edge.setColor(DockStyle.edge(style, -1));
    }

    /** The colour the floating plate takes; the caller repaints the list. A no-op for the band. */
    void setAmbient(int rgb) {
        if (!DockStyle.floating(style)) return;
        ambientRgb = rgb;
        edge.setColor(DockStyle.edge(style, rgb));
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        int pos = parent.getChildAdapterPosition(view);
        if (column) {
            // One plate around the whole column: room inside it above the first tile and below the last.
            if (pos == 0) outRect.top = (int) (6 * density);
            RecyclerView.Adapter<?> a = parent.getAdapter();
            if (a != null && pos == a.getItemCount() - 1) outRect.bottom = (int) (6 * density);
            return;
        }
        if (pos >= 0 && pos < dockSize) {
            outRect.top = (int) (6 * density);
            outRect.bottom = (int) (12 * density);
        }
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        boolean any = false;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            int pos = parent.getChildAdapterPosition(child);
            if (pos < 0 || pos >= dockSize) continue;
            View card = child.findViewById(R.id.card);
            if (card == null) continue;
            float l = child.getLeft() + card.getLeft();
            float t = child.getTop() + card.getTop() + child.getTranslationY();
            float r = l + card.getWidth();
            float b = t + card.getHeight();
            if (!any) {
                rect.set(l, t, r, b);
                any = true;
            } else {
                rect.union(l, t, r, b);
            }
        }
        if (!any) return;
        float pad = DockStyle.PAD_DP * density;
        rect.inset(-pad, -pad);
        if (DockStyle.floating(style)) glassFill();
        c.drawRoundRect(rect, radius, radius, band);
        c.drawRoundRect(rect, radius, radius, edge);
    }

    /** The glass gradient, rebuilt only when the plate moves or the colour changes. */
    private void glassFill() {
        int top = DockStyle.fillTop(style, ambientRgb), bottom = DockStyle.fillBottom(style, ambientRgb);
        if (band.getShader() != null && top == shaderTop && bottom == shaderBottom
                && rect.top == shaderFrom && rect.bottom == shaderTo) return;
        shaderTop = top;
        shaderBottom = bottom;
        shaderFrom = rect.top;
        shaderTo = rect.bottom;
        band.setShader(new LinearGradient(0, rect.top, 0, rect.bottom, top, bottom, Shader.TileMode.CLAMP));
    }
}
