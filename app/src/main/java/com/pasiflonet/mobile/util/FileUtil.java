package com.pasiflonet.mobile.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class FileUtil {
    private FileUtil() {}

    public static File copyToCache(Context c, Uri uri, String prefix) throws IOException {
        String name = displayName(c, uri);
        String ext = extension(c, uri, name);
        File out = new File(c.getCacheDir(), prefix + "_" + System.currentTimeMillis() + ext);
        try (InputStream in = c.getContentResolver().openInputStream(uri); OutputStream os = new FileOutputStream(out)) {
            if (in == null) throw new FileNotFoundException("לא ניתן לפתוח קובץ");
            byte[] buf = new byte[1024 * 64];
            int n;
            while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
        }
        return out;
    }

    public static String displayName(Context c, Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = c.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = cursor.getString(idx);
                }
            } catch (Exception ignored) {}
        }
        if (result == null) result = uri.getLastPathSegment();
        return result == null ? "media" : result;
    }

    public static String extension(Context c, Uri uri, String name) {
        String ext = null;
        String mime = c.getContentResolver().getType(uri);
        if (mime != null) ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
        if (ext != null) return "." + ext;
        if (name != null && name.contains(".")) return name.substring(name.lastIndexOf('.'));
        return ".bin";
    }

    public static File outputFile(Context c, String ext) {
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File dir = new File(c.getExternalFilesDir(null), "exports");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "azretr_ultra_" + stamp + ext);
    }

    public static String escapeTelegram(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
