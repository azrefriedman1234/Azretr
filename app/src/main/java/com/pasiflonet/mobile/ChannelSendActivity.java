package com.pasiflonet.mobile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import com.pasiflonet.mobile.bridge.TdlibReflectClient;
import com.pasiflonet.mobile.net.TelegramBotSender;
import com.pasiflonet.mobile.ui.UltraUi;
import com.pasiflonet.mobile.util.FileUtil;
import com.pasiflonet.mobile.util.Prefs;
import com.pasiflonet.mobile.util.UiThread;

import java.io.File;

public class ChannelSendActivity extends Activity {
    private static final int PICK = 701;
    EditText channel, caption;
    TextView status;
    Uri mediaUri;
    String mime;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        UltraUi.setup(this);
        LinearLayout root = new LinearLayout(this);
        setContentView(UltraUi.page(this, root));
        root.addView(UltraUi.title(this, "שליחה לערוץ"));
        root.addView(UltraUi.sub(this, "ברירת מחדל: שליחה דרך TDLib והחשבון המחובר, כמו באפליקציה המקורית. Bot API הוא רק גיבוי אופציונלי."));

        LinearLayout c = UltraUi.card(this);
        channel = UltraUi.input(this, "@channel או username יעד"); channel.setText(Prefs.get(this,"channel_username", ""));
        caption = UltraUi.input(this, "טקסט/קפशन לשליחה");
        String prefill = getIntent().getStringExtra("text");
        if (prefill != null) caption.setText(prefill);
        c.addView(channel); c.addView(caption);
        Button pick = UltraUi.ghost(this, "בחר מדיה");
        Button sendText = UltraUi.button(this, "שלח טקסט בלבד");
        Button sendMedia = UltraUi.button(this, "שלח מדיה + קפشن");
        status = UltraUi.sub(this, "לא נבחר קובץ");
        c.addView(pick); c.addView(sendText); c.addView(sendMedia); c.addView(status); root.addView(c);

        pick.setOnClickListener(v -> pickMedia());
        sendText.setOnClickListener(v -> send(null));
        sendMedia.setOnClickListener(v -> {
            if (mediaUri == null) { toast("בחר מדיה קודם"); return; }
            send(mediaUri);
        });
    }

    private void pickMedia() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*", "application/pdf", "text/*"});
        startActivityForResult(i, PICK);
    }

    @Override protected void onActivityResult(int r, int res, Intent d) {
        super.onActivityResult(r, res, d);
        if (r == PICK && res == RESULT_OK && d != null) {
            mediaUri = d.getData(); mime = getContentResolver().getType(mediaUri);
            status.setText("נבחר: " + FileUtil.displayName(this, mediaUri) + " • " + mime);
        }
    }

    private void send(Uri uri) {
        String target = channel.getText().toString().trim();
        Prefs.set(this,"channel_username", target);
        String text = caption.getText().toString();
        status.setText("שולח דרך TDLib...");
        if (uri == null) {
            TdlibReflectClient.get(this).sendToUsername(target, text, null, null, (ok,msg) -> UiThread.run(() -> {
                if (ok || !Prefs.getBool(this,"bot_fallback",false)) status.setText(msg); else sendTextBot(text, msg);
            }));
            return;
        }
        new Thread(() -> {
            try {
                File f = FileUtil.copyToCache(this, uri, "channel_send");
                TdlibReflectClient.get(this).sendToUsername(target, text, f.getAbsolutePath(), mime, (ok,msg) -> UiThread.run(() -> {
                    if (ok || !Prefs.getBool(this,"bot_fallback",false)) status.setText(msg); else sendFileBot(uri, text, msg);
                }));
            } catch (Throwable t) { UiThread.run(() -> status.setText("הכנת קובץ נכשלה: " + t.getMessage())); }
        }).start();
    }

    private void sendTextBot(String text, String prev) {
        status.setText(prev + "\nמנסה Bot API...");
        TelegramBotSender.sendText(Prefs.get(this,"bot_token",""), Prefs.get(this,"bot_chat",""), text, (ok,body) -> UiThread.run(() -> status.setText((ok ? "נשלח דרך Bot API" : "גם Bot API נכשל") + "\n" + trim(body))));
    }
    private void sendFileBot(Uri uri, String text, String prev) {
        status.setText(prev + "\nמנסה Bot API...");
        TelegramBotSender.sendFile(this, Prefs.get(this,"bot_token",""), Prefs.get(this,"bot_chat",""), uri, text, mime, (ok,body) -> UiThread.run(() -> status.setText((ok ? "נשלח דרך Bot API" : "גם Bot API נכשל") + "\n" + trim(body))));
    }
    private String trim(String s) { return s == null ? "" : (s.length() > 900 ? s.substring(0,900) + "..." : s); }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
