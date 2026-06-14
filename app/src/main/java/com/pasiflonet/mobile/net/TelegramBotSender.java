package com.pasiflonet.mobile.net;

import android.content.Context;
import android.net.Uri;

import com.pasiflonet.mobile.util.FileUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class TelegramBotSender {
    public interface Callback { void onResult(boolean ok, String body); }
    private TelegramBotSender() {}

    public static void sendText(String token, String chatId, String text, Callback cb) {
        new Thread(() -> {
            try {
                String url = "https://api.telegram.org/bot" + token + "/sendMessage";
                String data = "chat_id=" + enc(chatId) + "&text=" + enc(text == null ? "" : text) + "&parse_mode=HTML";
                HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                try (OutputStream os = con.getOutputStream()) { os.write(data.getBytes(StandardCharsets.UTF_8)); }
                String body = read(con);
                cb.onResult(con.getResponseCode() >= 200 && con.getResponseCode() < 300, body);
            } catch (Throwable t) { cb.onResult(false, t.getClass().getSimpleName() + ": " + t.getMessage()); }
        }).start();
    }

    public static void sendFile(Context c, String token, String chatId, Uri uri, String caption, String kind, Callback cb) {
        new Thread(() -> {
            try {
                File file = FileUtil.copyToCache(c, uri, "tg_send");
                String method = "sendDocument";
                String part = "document";
                if (kind != null && kind.startsWith("image")) { method = "sendPhoto"; part = "photo"; }
                if (kind != null && kind.startsWith("video")) { method = "sendVideo"; part = "video"; }
                String boundary = "AzretrBoundary" + System.currentTimeMillis();
                HttpURLConnection con = (HttpURLConnection) new URL("https://api.telegram.org/bot" + token + "/" + method).openConnection();
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                try (OutputStream out = con.getOutputStream()) {
                    field(out, boundary, "chat_id", chatId);
                    field(out, boundary, "caption", caption == null ? "" : caption);
                    field(out, boundary, "parse_mode", "HTML");
                    file(out, boundary, part, file, contentType(kind));
                    out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
                }
                String body = read(con);
                cb.onResult(con.getResponseCode() >= 200 && con.getResponseCode() < 300, body);
            } catch (Throwable t) { cb.onResult(false, t.getClass().getSimpleName() + ": " + t.getMessage()); }
        }).start();
    }

    private static void field(OutputStream out, String b, String name, String val) throws IOException {
        out.write(("--" + b + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + val + "\r\n").getBytes(StandardCharsets.UTF_8));
    }
    private static void file(OutputStream out, String b, String name, File f, String ct) throws IOException {
        out.write(("--" + b + "\r\nContent-Disposition: form-data; name=\"" + name + "\"; filename=\"" + f.getName() + "\"\r\nContent-Type: " + ct + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        try (InputStream in = new FileInputStream(f)) { byte[] buf = new byte[1024 * 64]; int n; while ((n = in.read(buf)) > 0) out.write(buf, 0, n); }
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }
    private static String contentType(String kind) { return kind == null ? "application/octet-stream" : kind; }
    private static String enc(String s) throws UnsupportedEncodingException { return java.net.URLEncoder.encode(s == null ? "" : s, "UTF-8"); }
    private static String read(HttpURLConnection con) throws IOException {
        InputStream in = con.getResponseCode() >= 400 ? con.getErrorStream() : con.getInputStream();
        if (in == null) return "";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192]; int n; while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        return bos.toString("UTF-8");
    }
}
