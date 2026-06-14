package com.pasiflonet.mobile.bridge;

import android.util.Log;

import java.lang.reflect.Method;

/** Uses local ffmpeg-kit AAR when present: app/libs/ffmpeg-kit-full-gpl-6.0.aar */
public final class FfmpegKitBridge {
    public interface Callback { void onDone(boolean ok, String log); }
    private FfmpegKitBridge() {}

    public static boolean isAvailable() {
        try { Class.forName("com.arthenica.ffmpegkit.FFmpegKit"); return true; }
        catch (Throwable t) { return false; }
    }

    public static void execute(String command, Callback cb) {
        try {
            Class<?> kit = Class.forName("com.arthenica.ffmpegkit.FFmpegKit");
            Method exec = kit.getMethod("execute", String.class);
            Object session = exec.invoke(null, command);
            boolean ok = false;
            String out = "";
            try {
                Method code = session.getClass().getMethod("getReturnCode");
                Object returnCode = code.invoke(session);
                out += "ReturnCode=" + returnCode + "\n";
                if (returnCode != null) ok = returnCode.toString().contains("SUCCESS") || returnCode.toString().equals("0");
            } catch (Throwable ignored) {}
            try {
                Method logs = session.getClass().getMethod("getAllLogsAsString");
                Object all = logs.invoke(session);
                if (all != null) out += all.toString();
            } catch (Throwable ignored) {}
            if (cb != null) cb.onDone(ok, out.isEmpty() ? "FFmpeg הסתיים" : out);
        } catch (Throwable t) {
            Log.e("AzretrFFmpeg", "execute", t);
            if (cb != null) cb.onDone(false, t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    public static String q(String path) {
        if (path == null) return "''";
        return "'" + path.replace("'", "'\\''") + "'";
    }
}
