package com.pasiflonet.mobile.util;

import android.os.Handler;
import android.os.Looper;

public final class UiThread {
    private static final Handler H = new Handler(Looper.getMainLooper());
    private UiThread() {}
    public static void run(Runnable r) { H.post(r); }
}
