package com.pasiflonet.mobile.bridge;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

/**
 * TDLib adapter without compile-time dependency. It uses the local tdlib.aar in app/libs when present.
 * This keeps GitHub Actions from downloading TDLib and keeps the app compiling even if the AAR is missing locally.
 */
public final class TdlibReflectClient {
    public interface Callback { void onEvent(String line); }
    public interface SendCallback { void onDone(boolean ok, String message); }

    private static final String TAG = "AzretrTdlib";
    private static TdlibReflectClient instance;
    private final Context app;
    private Object client;
    private Class<?> clientClass;
    private Class<?> functionClass;
    private Class<?> resultHandlerClass;
    private Callback callback;
    private long lastChatId;
    private boolean initialized;

    public static synchronized TdlibReflectClient get(Context c) {
        if (instance == null) instance = new TdlibReflectClient(c.getApplicationContext());
        return instance;
    }

    private TdlibReflectClient(Context app) { this.app = app; }

    public boolean isAvailable() {
        try {
            Class.forName("org.drinkless.tdlib.Client");
            Class.forName("org.drinkless.tdlib.TdApi");
            return true;
        } catch (Throwable t) { return false; }
    }

    public synchronized void start(Callback cb) {
        this.callback = cb;
        if (!isAvailable()) {
            event("TDLib AAR לא נטען. ודא ש־app/libs/tdlib.aar נמצא בריפו.");
            return;
        }
        if (client != null) {
            event("TDLib כבר פעיל.");
            return;
        }
        try {
            clientClass = Class.forName("org.drinkless.tdlib.Client");
            functionClass = Class.forName("org.drinkless.tdlib.TdApi$Function");
            resultHandlerClass = Class.forName("org.drinkless.tdlib.Client$ResultHandler");
            Object updateHandler = proxy(handlerArgs -> {
                if (handlerArgs != null && handlerArgs.length > 0) handleUpdate(handlerArgs[0]);
            });
            Object exceptionHandler = proxy(handlerArgs -> {
                if (handlerArgs != null && handlerArgs.length > 0) event("TDLib exception: " + handlerArgs[0]);
            });
            Method create = clientClass.getMethod("create", resultHandlerClass, resultHandlerClass, resultHandlerClass);
            client = create.invoke(null, updateHandler, exceptionHandler, exceptionHandler);
            initialized = true;
            event("TDLib הופעל. המשך הזנת פרטי התחברות.");
        } catch (Throwable t) {
            Log.e(TAG, "start", t);
            event("שגיאה בהפעלת TDLib: " + shortErr(t));
        }
    }

    public void setParameters(int apiId, String apiHash) {
        try {
            Object params = obj("TdlibParameters");
            set(params, "databaseDirectory", new File(app.getFilesDir(), "tdlib-db").getAbsolutePath());
            set(params, "filesDirectory", new File(app.getFilesDir(), "tdlib-files").getAbsolutePath());
            set(params, "useMessageDatabase", true);
            set(params, "useSecretChats", false);
            set(params, "apiId", apiId);
            set(params, "apiHash", apiHash);
            set(params, "systemLanguageCode", "he");
            set(params, "deviceModel", Build.MANUFACTURER + " " + Build.MODEL);
            set(params, "systemVersion", String.valueOf(Build.VERSION.SDK_INT));
            set(params, "applicationVersion", "Azretr Ultra 3.0");
            set(params, "enableStorageOptimizer", true);
            set(params, "useFileDatabase", true);
            set(params, "useChatInfoDatabase", true);

            Object fn = obj("SetTdlibParameters");
            set(fn, "parameters", params);
            send(fn, r -> event("פרמטרים נשלחו ל־TDLib: " + cls(r)));
        } catch (Throwable t) { event("SetTdlibParameters נכשל: " + shortErr(t)); }
    }

    public void checkEncryptionKey() {
        try {
            Object fn = obj("CheckDatabaseEncryptionKey");
            set(fn, "encryptionKey", new byte[0]);
            send(fn, r -> event("מפתח הצפנת DB אושר: " + cls(r)));
        } catch (Throwable t) { event("CheckDatabaseEncryptionKey נכשל: " + shortErr(t)); }
    }

    public void setPhone(String phone) {
        try {
            Object fn = obj("SetAuthenticationPhoneNumber");
            set(fn, "phoneNumber", phone);
            send(fn, r -> event("טלפון נשלח: " + cls(r)));
        } catch (Throwable t) { event("שליחת טלפון נכשלה: " + shortErr(t)); }
    }

    public void checkCode(String code) {
        try {
            Object fn = obj("CheckAuthenticationCode");
            set(fn, "code", code);
            send(fn, r -> event("קוד אימות נשלח: " + cls(r)));
        } catch (Throwable t) { event("שליחת קוד נכשלה: " + shortErr(t)); }
    }

    public void checkPassword(String pass) {
        try {
            Object fn = obj("CheckAuthenticationPassword");
            set(fn, "password", pass);
            send(fn, r -> event("סיסמת 2FA נשלחה: " + cls(r)));
        } catch (Throwable t) { event("שליחת סיסמה נכשלה: " + shortErr(t)); }
    }

    public void sendToUsername(String username, String text, String filePath, String mediaKind, SendCallback cb) {
        if (client == null) {
            cb.onDone(false, "TDLib לא פעיל. התחבר קודם במסך חיבור טלגרם.");
            return;
        }
        String clean = username == null ? "" : username.trim().replace("@", "");
        if (clean.isEmpty()) { cb.onDone(false, "חסר username של ערוץ/משתמש."); return; }
        try {
            Object search = obj("SearchPublicChat");
            set(search, "username", clean);
            send(search, result -> {
                try {
                    if (!"Chat".equals(cls(result))) {
                        cb.onDone(false, "לא נמצא צ׳אט: " + cls(result));
                        return;
                    }
                    lastChatId = ((Number) get(result, "id")).longValue();
                    Object content = buildContent(text, filePath, mediaKind);
                    Object fn = obj("SendMessage");
                    set(fn, "chatId", lastChatId);
                    set(fn, "messageThreadId", 0L);
                    set(fn, "replyTo", null);
                    set(fn, "options", null);
                    set(fn, "replyMarkup", null);
                    set(fn, "inputMessageContent", content);
                    send(fn, sent -> cb.onDone("Message".equals(cls(sent)), "תוצאת שליחה: " + cls(sent)));
                } catch (Throwable t) { cb.onDone(false, "שגיאת שליחה: " + shortErr(t)); }
            });
        } catch (Throwable t) { cb.onDone(false, "שליחה נכשלה: " + shortErr(t)); }
    }

    private Object buildContent(String text, String filePath, String kind) throws Throwable {
        String caption = text == null ? "" : text;
        if (filePath == null || filePath.trim().isEmpty()) {
            Object formatted = formattedText(caption);
            Object msg = obj("InputMessageText");
            set(msg, "text", formatted);
            set(msg, "disableWebPagePreview", false);
            set(msg, "clearDraft", true);
            return msg;
        }
        Object inputFile = obj("InputFileLocal");
        set(inputFile, "path", filePath);
        Object formatted = formattedText(caption);
        String k = kind == null ? "document" : kind.toLowerCase();
        if (k.contains("image") || k.contains("photo")) {
            Object msg = obj("InputMessagePhoto");
            set(msg, "photo", inputFile);
            set(msg, "thumbnail", null);
            set(msg, "addedStickerFileIds", new long[0]);
            set(msg, "width", 0);
            set(msg, "height", 0);
            set(msg, "caption", formatted);
            set(msg, "selfDestructType", null);
            return msg;
        }
        if (k.contains("video")) {
            Object msg = obj("InputMessageVideo");
            set(msg, "video", inputFile);
            set(msg, "thumbnail", null);
            set(msg, "addedStickerFileIds", new long[0]);
            set(msg, "duration", 0);
            set(msg, "width", 0);
            set(msg, "height", 0);
            set(msg, "supportsStreaming", true);
            set(msg, "caption", formatted);
            set(msg, "selfDestructType", null);
            return msg;
        }
        Object msg = obj("InputMessageDocument");
        set(msg, "document", inputFile);
        set(msg, "thumbnail", null);
        set(msg, "disableContentTypeDetection", false);
        set(msg, "caption", formatted);
        return msg;
    }

    private Object formattedText(String s) throws Throwable {
        Object ft = obj("FormattedText");
        set(ft, "text", s == null ? "" : s);
        Class<?> ent = Class.forName("org.drinkless.tdlib.TdApi$TextEntity");
        set(ft, "entities", Array.newInstance(ent, 0));
        return ft;
    }

    private void handleUpdate(Object update) {
        String name = cls(update);
        if ("UpdateAuthorizationState".equals(name)) {
            try {
                Object state = get(update, "authorizationState");
                event("מצב התחברות: " + cls(state));
            } catch (Throwable t) { event("UpdateAuth: " + shortErr(t)); }
        } else if (name.startsWith("Update")) {
            event("עדכון TDLib: " + name);
        }
    }

    private void send(Object function, Result cb) throws Throwable {
        if (!initialized || client == null) throw new IllegalStateException("TDLib client not initialized");
        Method m = clientClass.getMethod("send", functionClass, resultHandlerClass);
        m.invoke(client, function, proxy(args -> { if (args != null && args.length > 0) cb.on(args[0]); }));
    }

    private interface Result { void on(Object result); }
    private interface ProxyCall { void on(Object[] args); }

    private Object proxy(ProxyCall call) {
        return Proxy.newProxyInstance(resultHandlerClass.getClassLoader(), new Class[]{resultHandlerClass}, (p, method, args) -> {
            if (method.getName().equals("onResult")) call.on(args);
            return null;
        });
    }

    private Object obj(String nested) throws Throwable { return Class.forName("org.drinkless.tdlib.TdApi$" + nested).getDeclaredConstructor().newInstance(); }

    private void set(Object o, String field, Object value) {
        try {
            Field f = o.getClass().getField(field);
            if (value == null && f.getType().isPrimitive()) return;
            f.set(o, value);
        } catch (Throwable ignored) { }
    }

    private Object get(Object o, String field) throws Throwable { return o.getClass().getField(field).get(o); }
    private String cls(Object o) { return o == null ? "null" : o.getClass().getSimpleName(); }
    private void event(String s) { if (callback != null) callback.onEvent(s); Log.d(TAG, s); }
    private String shortErr(Throwable t) {
        Throwable x = t instanceof InvocationTargetException && ((InvocationTargetException)t).getTargetException() != null ? ((InvocationTargetException)t).getTargetException() : t;
        return x.getClass().getSimpleName() + ": " + x.getMessage();
    }
}
