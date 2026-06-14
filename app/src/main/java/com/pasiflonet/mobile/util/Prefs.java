package com.pasiflonet.mobile.util;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private static final String NAME = "azretr_ultra_prefs";
    private Prefs() {}
    public static SharedPreferences sp(Context c) { return c.getSharedPreferences(NAME, Context.MODE_PRIVATE); }
    public static String get(Context c, String key, String def) { return sp(c).getString(key, def); }
    public static void set(Context c, String key, String val) { sp(c).edit().putString(key, val == null ? "" : val).apply(); }
    public static int getInt(Context c, String key, int def) { return sp(c).getInt(key, def); }
    public static void setInt(Context c, String key, int val) { sp(c).edit().putInt(key, val).apply(); }
    public static boolean getBool(Context c, String key, boolean def) { return sp(c).getBoolean(key, def); }
    public static void setBool(Context c, String key, boolean val) { sp(c).edit().putBoolean(key, val).apply(); }
}
