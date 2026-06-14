package com.pasiflonet.mobile.media;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class EditorCanvasView extends View {
    private Bitmap bitmap;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blurPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<BlurZone> zones = new ArrayList<>();
    private String watermark = "Azretr Ultra";
    private float wx = 0.92f, wy = 0.90f;
    private boolean blurMode;
    private float startX, startY;
    private RectF temp;
    private int textSize = 48;
    private int blurRadius = 18;

    public EditorCanvasView(Context c) { super(c); init(); }
    private void init() {
        setBackgroundColor(Color.rgb(4, 10, 18));
        text.setColor(Color.WHITE); text.setTypeface(Typeface.DEFAULT_BOLD); text.setTextAlign(Paint.Align.RIGHT);
        blurPaint.setStyle(Paint.Style.STROKE); blurPaint.setStrokeWidth(4); blurPaint.setColor(Color.argb(210, 53, 208, 255));
    }

    public void setBitmap(Bitmap b) { bitmap = b; invalidate(); }
    public Bitmap getBitmap() { return bitmap; }
    public List<BlurZone> getZones() { return zones; }
    public void setWatermark(String s) { watermark = s == null ? "" : s; invalidate(); }
    public String getWatermark() { return watermark; }
    public void setBlurMode(boolean b) { blurMode = b; }
    public void setTextSizeValue(int size) { textSize = size; invalidate(); }
    public int getTextSizeValue() { return textSize; }
    public void setBlurRadius(int r) { blurRadius = r; }
    public int getBlurRadius() { return blurRadius; }
    public void clearBlur() { zones.clear(); invalidate(); }
    public float getWatermarkX() { return wx; }
    public float getWatermarkY() { return wy; }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (bitmap == null) {
            p.setColor(Color.rgb(156,180,208)); p.setTextSize(42); p.setTextAlign(Paint.Align.CENTER);
            c.drawText("בחר מדיה לעריכה", getWidth()/2f, getHeight()/2f, p); return;
        }
        RectF dst = imageRect();
        c.drawBitmap(bitmap, null, dst, null);
        for (BlurZone z : zones) c.drawRect(toScreen(z.rect), blurPaint);
        if (temp != null) c.drawRect(temp, blurPaint);
        drawWatermark(c, dst);
    }

    private void drawWatermark(Canvas c, RectF dst) {
        text.setTextSize(textSize);
        Paint shadow = new Paint(text); shadow.setColor(Color.argb(170,0,0,0));
        float x = dst.left + wx * dst.width(); float y = dst.top + wy * dst.height();
        c.drawText(watermark, x + 3, y + 3, shadow);
        c.drawText(watermark, x, y, text);
    }

    private RectF imageRect() {
        if (bitmap == null) return new RectF(0,0,getWidth(),getHeight());
        float vw = getWidth(), vh = getHeight(); float bw = bitmap.getWidth(), bh = bitmap.getHeight();
        float scale = Math.min(vw/bw, vh/bh); float dw = bw*scale, dh = bh*scale;
        return new RectF((vw-dw)/2f, (vh-dh)/2f, (vw+dw)/2f, (vh+dh)/2f);
    }

    private RectF toScreen(RectF rel) {
        RectF d = imageRect(); return new RectF(d.left + rel.left*d.width(), d.top + rel.top*d.height(), d.left + rel.right*d.width(), d.top + rel.bottom*d.height());
    }
    private RectF toRelative(RectF screen) {
        RectF d = imageRect();
        float l = clamp((screen.left-d.left)/d.width()), t = clamp((screen.top-d.top)/d.height()), r = clamp((screen.right-d.left)/d.width()), b = clamp((screen.bottom-d.top)/d.height());
        return new RectF(Math.min(l,r), Math.min(t,b), Math.max(l,r), Math.max(t,b));
    }
    private float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (bitmap == null) return true;
        RectF d = imageRect();
        if (blurMode) {
            if (e.getAction() == MotionEvent.ACTION_DOWN) { startX=e.getX(); startY=e.getY(); temp = new RectF(startX,startY,startX,startY); invalidate(); return true; }
            if (e.getAction() == MotionEvent.ACTION_MOVE) { temp = new RectF(Math.min(startX,e.getX()),Math.min(startY,e.getY()),Math.max(startX,e.getX()),Math.max(startY,e.getY())); invalidate(); return true; }
            if (e.getAction() == MotionEvent.ACTION_UP && temp != null) {
                RectF rel = toRelative(temp);
                if (Math.abs(rel.width()) > 0.03f && Math.abs(rel.height()) > 0.03f) zones.add(new BlurZone(rel, blurRadius));
                temp = null; invalidate(); return true;
            }
        } else {
            if (e.getAction() == MotionEvent.ACTION_DOWN || e.getAction() == MotionEvent.ACTION_MOVE) {
                wx = clamp((e.getX() - d.left) / d.width()); wy = clamp((e.getY() - d.top) / d.height()); invalidate(); return true;
            }
        }
        return true;
    }
}
