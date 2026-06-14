package com.pasiflonet.mobile.media;

import android.graphics.*;

import java.util.List;

public final class BitmapProcessor {
    private BitmapProcessor() {}

    public static Bitmap render(Bitmap src, String watermark, float wx, float wy, int textSize, List<BlurZone> zones) {
        Bitmap out = src.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(out);
        if (zones != null) {
            for (BlurZone z : zones) applyBlur(out, z.rect, Math.max(4, z.radius));
        }
        Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadow.setColor(Color.argb(160, 0, 0, 0));
        shadow.setTextSize(textSize);
        shadow.setTypeface(Typeface.DEFAULT_BOLD);
        shadow.setTextAlign(Paint.Align.RIGHT);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.WHITE);
        p.setTextSize(textSize);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextAlign(Paint.Align.RIGHT);
        String w = watermark == null || watermark.trim().isEmpty() ? "Azretr Ultra" : watermark;
        float x = wx <= 1 ? wx * out.getWidth() : wx;
        float y = wy <= 1 ? wy * out.getHeight() : wy;
        canvas.drawText(w, x + 3, y + 3, shadow);
        canvas.drawText(w, x, y, p);
        return out;
    }

    private static void applyBlur(Bitmap bitmap, RectF rel, int radius) {
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        int left = clamp((int)(rel.left * w), 0, w-1);
        int top = clamp((int)(rel.top * h), 0, h-1);
        int right = clamp((int)(rel.right * w), left+1, w);
        int bottom = clamp((int)(rel.bottom * h), top+1, h);
        Bitmap piece = Bitmap.createBitmap(bitmap, left, top, right-left, bottom-top);
        Bitmap blurred = stackBlur(piece, radius);
        Canvas c = new Canvas(bitmap);
        c.drawBitmap(blurred, left, top, null);
        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setStyle(Paint.Style.STROKE); border.setStrokeWidth(3); border.setColor(Color.argb(150,255,255,255));
        c.drawRect(left, top, right, bottom, border);
    }

    private static int clamp(int v, int a, int b) { return Math.max(a, Math.min(b, v)); }

    public static Bitmap stackBlur(Bitmap sentBitmap, int radius) {
        if (radius < 1) return sentBitmap;
        Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig() == null ? Bitmap.Config.ARGB_8888 : sentBitmap.getConfig(), true);
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        int[] pix = new int[w * h]; bitmap.getPixels(pix, 0, w, 0, 0, w, h);
        int wm = w - 1, hm = h - 1, wh = w * h, div = radius + radius + 1;
        int[] r = new int[wh], g = new int[wh], b = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int[] vmin = new int[Math.max(w, h)];
        int divsum = (div + 1) >> 1; divsum *= divsum;
        int[] dv = new int[256 * divsum]; for (i = 0; i < 256 * divsum; i++) dv[i] = (i / divsum);
        yw = yi = 0;
        int[][] stack = new int[div][3];
        int stackpointer, stackstart; int[] sir; int rbs; int r1 = radius + 1; int routsum, goutsum, boutsum; int rinsum, ginsum, binsum;
        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))]; sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16; sir[1] = (p & 0x00ff00) >> 8; sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i); rsum += sir[0] * rbs; gsum += sir[1] * rbs; bsum += sir[2] * rbs;
                if (i > 0) { rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]; } else { routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]; }
            }
            stackpointer = radius;
            for (x = 0; x < w; x++) {
                r[yi] = dv[rsum]; g[yi] = dv[gsum]; b[yi] = dv[bsum];
                rsum -= routsum; gsum -= goutsum; bsum -= boutsum;
                stackstart = stackpointer - radius + div; sir = stack[stackstart % div];
                routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2];
                if (y == 0) vmin[x] = Math.min(x + radius + 1, wm);
                p = pix[yw + vmin[x]]; sir[0] = (p & 0xff0000) >> 16; sir[1] = (p & 0x00ff00) >> 8; sir[2] = (p & 0x0000ff);
                rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2];
                rsum += rinsum; gsum += ginsum; bsum += binsum;
                stackpointer = (stackpointer + 1) % div; sir = stack[(stackpointer) % div];
                routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2];
                rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2]; yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0; yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x; sir = stack[i + radius]; sir[0] = r[yi]; sir[1] = g[yi]; sir[2] = b[yi];
                rbs = r1 - Math.abs(i); rsum += r[yi] * rbs; gsum += g[yi] * rbs; bsum += b[yi] * rbs;
                if (i > 0) { rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]; } else { routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]; }
                if (i < hm) yp += w;
            }
            yi = x; stackpointer = radius;
            for (y = 0; y < h; y++) {
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];
                rsum -= routsum; gsum -= goutsum; bsum -= boutsum;
                stackstart = stackpointer - radius + div; sir = stack[stackstart % div];
                routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2];
                if (x == 0) vmin[y] = Math.min(y + r1, hm) * w;
                p = x + vmin[y]; sir[0] = r[p]; sir[1] = g[p]; sir[2] = b[p];
                rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2];
                rsum += rinsum; gsum += ginsum; bsum += binsum;
                stackpointer = (stackpointer + 1) % div; sir = stack[stackpointer];
                routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2];
                rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2]; yi += w;
            }
        }
        bitmap.setPixels(pix, 0, w, 0, 0, w, h); return bitmap;
    }
}
