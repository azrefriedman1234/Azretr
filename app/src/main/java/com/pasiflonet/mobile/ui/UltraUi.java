package com.pasiflonet.mobile.ui;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.*;
import android.content.Context;

public final class UltraUi {
    public static final int BG = Color.rgb(7, 17, 31);
    public static final int CARD = Color.rgb(16, 34, 56);
    public static final int CARD2 = Color.rgb(21, 43, 70);
    public static final int TEXT = Color.rgb(244, 248, 255);
    public static final int MUTED = Color.rgb(156, 180, 208);
    public static final int ACCENT = Color.rgb(53, 208, 255);
    public static final int GOOD = Color.rgb(141, 255, 184);
    public static final int WARN = Color.rgb(255, 195, 87);

    private UltraUi() {}

    public static void setup(Activity a) {
        a.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        Window w = a.getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= 23) w.getDecorView().setSystemUiVisibility(0);
    }

    public static int dp(Context c, int v) { return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f); }

    public static ScrollView page(Activity a, LinearLayout inner) {
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.RIGHT);
        inner.setPadding(dp(a, 18), dp(a, 16), dp(a, 18), dp(a, 24));
        inner.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        ScrollView sv = new ScrollView(a);
        sv.setFillViewport(true);
        sv.setBackgroundColor(BG);
        sv.addView(inner, new ScrollView.LayoutParams(-1, -2));
        return sv;
    }

    public static TextView title(Context c, String s) {
        TextView t = text(c, s, 30, TEXT, true);
        t.setPadding(0, dp(c, 4), 0, dp(c, 4));
        return t;
    }

    public static TextView sub(Context c, String s) { return text(c, s, 15, MUTED, false); }

    public static TextView text(Context c, String s, int sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.RIGHT);
        t.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setIncludeFontPadding(true);
        return t;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.RIGHT);
        l.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 14));
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{CARD2, CARD});
        g.setCornerRadius(dp(c, 22));
        g.setStroke(dp(c, 1), Color.argb(90, 53, 208, 255));
        l.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(c, 10), 0, dp(c, 10));
        l.setLayoutParams(lp);
        l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return l;
    }

    public static Button button(Context c, String s) {
        Button b = new Button(c);
        b.setText(s);
        b.setTextColor(Color.rgb(2, 16, 25));
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{GOOD, ACCENT});
        gd.setCornerRadius(dp(c, 18));
        b.setBackground(gd);
        b.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(c, 52));
        lp.setMargins(0, dp(c, 8), 0, dp(c, 8));
        b.setLayoutParams(lp);
        return b;
    }

    public static Button ghost(Context c, String s) {
        Button b = button(c, s);
        b.setTextColor(TEXT);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.argb(40, 53, 208, 255));
        gd.setStroke(dp(c, 1), Color.argb(120, 53, 208, 255));
        gd.setCornerRadius(dp(c, 18));
        b.setBackground(gd);
        return b;
    }

    public static EditText input(Context c, String hint) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setHintTextColor(Color.argb(140, 244, 248, 255));
        e.setTextColor(TEXT);
        e.setTextSize(16);
        e.setSingleLine(false);
        e.setMinLines(1);
        e.setMaxLines(4);
        e.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.argb(80, 255, 255, 255));
        gd.setStroke(dp(c, 1), Color.argb(70, 255, 255, 255));
        gd.setCornerRadius(dp(c, 16));
        e.setBackground(gd);
        e.setPadding(dp(c, 12), 0, dp(c, 12), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(c, 54));
        lp.setMargins(0, dp(c, 6), 0, dp(c, 6));
        e.setLayoutParams(lp);
        return e;
    }

    public static void addSpace(LinearLayout l, int dp) {
        Space s = new Space(l.getContext());
        l.addView(s, new LinearLayout.LayoutParams(1, dp(l.getContext(), dp)));
    }

    public static TextView badge(Context c, String s, int color) {
        TextView t = text(c, s, 13, TEXT, true);
        t.setGravity(Gravity.CENTER);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.argb(45, Color.red(color), Color.green(color), Color.blue(color)));
        gd.setStroke(dp(c, 1), Color.argb(180, Color.red(color), Color.green(color), Color.blue(color)));
        gd.setCornerRadius(dp(c, 14));
        t.setBackground(gd);
        t.setPadding(dp(c, 10), dp(c, 5), dp(c, 10), dp(c, 5));
        return t;
    }
}
