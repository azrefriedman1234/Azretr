package com.pasiflonet.mobile.media;

import android.graphics.RectF;

public class BlurZone {
    public final RectF rect;
    public int radius;
    public BlurZone(RectF r, int radius) { this.rect = r; this.radius = radius; }
}
