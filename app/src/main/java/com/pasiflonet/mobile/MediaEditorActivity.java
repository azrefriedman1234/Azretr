package com.pasiflonet.mobile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;

import com.pasiflonet.mobile.bridge.FfmpegKitBridge;
import com.pasiflonet.mobile.bridge.TdlibReflectClient;
import com.pasiflonet.mobile.media.BitmapProcessor;
import com.pasiflonet.mobile.media.EditorCanvasView;
import com.pasiflonet.mobile.net.TelegramBotSender;
import com.pasiflonet.mobile.ui.UltraUi;
import com.pasiflonet.mobile.util.FileUtil;
import com.pasiflonet.mobile.util.Prefs;
import com.pasiflonet.mobile.util.UiThread;

import java.io.File;
import java.io.FileOutputStream;

public class MediaEditorActivity extends Activity {
    private static final int PICK = 601;
    EditorCanvasView canvas;
    EditText watermark, caption;
    TextView status;
    Uri selectedUri;
    String selectedMime;
    File exported;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        UltraUi.setup(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UltraUi.BG);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        setContentView(root);

        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.VERTICAL); top.setPadding(UltraUi.dp(this,14),UltraUi.dp(this,10),UltraUi.dp(this,14),UltraUi.dp(this,6));
        top.addView(UltraUi.title(this, "עורך מדיה"));
        top.addView(UltraUi.sub(this, "גרור את סימן המים עם האצבע. עבור לטשטוש וסמן מלבן על התמונה."));
        root.addView(top);

        canvas = new EditorCanvasView(this);
        root.addView(canvas, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout controls = UltraUi.card(this);
        watermark = UltraUi.input(this, "טקסט סימן מים"); watermark.setText(Prefs.get(this,"watermark","Azretr Ultra"));
        caption = UltraUi.input(this, "קפشن לשליחה לערוץ");
        status = UltraUi.sub(this, "לא נבחרה מדיה");
        controls.addView(watermark);
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        Button pick = UltraUi.button(this, "בחר"); Button move = UltraUi.ghost(this, "גרירה"); Button blur = UltraUi.ghost(this, "טשטוש"); Button clear = UltraUi.ghost(this, "נקה");
        row.addView(pick, new LinearLayout.LayoutParams(0, UltraUi.dp(this,50), 1));
        row.addView(move, new LinearLayout.LayoutParams(0, UltraUi.dp(this,50), 1));
        row.addView(blur, new LinearLayout.LayoutParams(0, UltraUi.dp(this,50), 1));
        row.addView(clear, new LinearLayout.LayoutParams(0, UltraUi.dp(this,50), 1));
        controls.addView(row);
        controls.addView(caption);
        Button export = UltraUi.button(this, "ייצא מדיה ערוכה");
        Button send = UltraUi.button(this, "שלח לערוץ עכשיו");
        controls.addView(export); controls.addView(send); controls.addView(status);
        root.addView(controls);

        watermark.setOnFocusChangeListener((v, has) -> canvas.setWatermark(watermark.getText().toString()));
        pick.setOnClickListener(v -> pickMedia());
        move.setOnClickListener(v -> { canvas.setBlurMode(false); toast("מצב גרירת סימן מים"); });
        blur.setOnClickListener(v -> { canvas.setBlurMode(true); toast("מצב טשטוש — גרור מלבן על התמונה"); });
        clear.setOnClickListener(v -> { canvas.clearBlur(); toast("הטשטושים נמחקו"); });
        export.setOnClickListener(v -> exportMedia(false));
        send.setOnClickListener(v -> exportMedia(true));
    }

    private void pickMedia() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        startActivityForResult(i, PICK);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PICK && result == RESULT_OK && data != null) {
            selectedUri = data.getData();
            selectedMime = getContentResolver().getType(selectedUri);
            status.setText("נבחר: " + FileUtil.displayName(this, selectedUri) + " • " + selectedMime);
            canvas.setWatermark(watermark.getText().toString());
            try {
                if (selectedMime != null && selectedMime.startsWith("image")) {
                    Bitmap bm = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedUri);
                    canvas.setBitmap(bm);
                } else {
                    canvas.setBitmap(null);
                    status.setText(status.getText() + "\nוידאו נבחר. תצוגת עריכה מלאה לתמונה; וידאו יעובד דרך FFmpeg אם ה־AAR קיים.");
                }
            } catch (Throwable t) { status.setText("שגיאה בטעינת מדיה: " + t.getMessage()); }
        }
    }

    private void exportMedia(boolean sendAfter) {
        canvas.setWatermark(watermark.getText().toString());
        if (selectedUri == null) { toast("בחר מדיה קודם"); return; }
        if (selectedMime != null && selectedMime.startsWith("image")) exportImage(sendAfter);
        else exportVideo(sendAfter);
    }

    private void exportImage(boolean sendAfter) {
        try {
            Bitmap src = canvas.getBitmap();
            if (src == null) throw new IllegalStateException("אין תמונה");
            int size = Math.max(32, src.getWidth() / 22);
            Bitmap out = BitmapProcessor.render(src, watermark.getText().toString(), canvas.getWatermarkX(), canvas.getWatermarkY(), size, canvas.getZones());
            exported = FileUtil.outputFile(this, ".jpg");
            try (FileOutputStream fos = new FileOutputStream(exported)) { out.compress(Bitmap.CompressFormat.JPEG, 94, fos); }
            status.setText("נשמר: " + exported.getAbsolutePath());
            if (sendAfter) sendExported("image/jpeg");
        } catch (Throwable t) { status.setText("ייצוא תמונה נכשל: " + t.getMessage()); }
    }

    private void exportVideo(boolean sendAfter) {
        if (!FfmpegKitBridge.isAvailable()) {
            status.setText("FFmpeg AAR לא זמין. שולח את הווידאו המקורי ללא עיבוד.");
            if (sendAfter) sendUri(selectedUri, selectedMime);
            return;
        }
        new Thread(() -> {
            try {
                File input = FileUtil.copyToCache(this, selectedUri, "video_input");
                File out = FileUtil.outputFile(this, ".mp4");
                String wm = watermark.getText().toString().replace("'", "").replace(":", "");
                String vf = "drawtext=text='" + wm + "':x=w-tw-40:y=h-th-40:fontsize=42:fontcolor=white:box=1:boxcolor=black@0.35";
                String cmd = "-y -i " + FfmpegKitBridge.q(input.getAbsolutePath()) + " -vf \"" + vf + "\" -c:a copy " + FfmpegKitBridge.q(out.getAbsolutePath());
                UiThread.run(() -> status.setText("מעבד וידאו עם FFmpeg..."));
                FfmpegKitBridge.execute(cmd, (ok, log) -> UiThread.run(() -> {
                    exported = out;
                    status.setText((ok ? "וידאו נשמר: " : "FFmpeg הסתיים עם בעיה: ") + out.getAbsolutePath() + "\n" + trim(log));
                    if (sendAfter) sendExported("video/mp4");
                }));
            } catch (Throwable t) { UiThread.run(() -> status.setText("ייצוא וידאו נכשל: " + t.getMessage())); }
        }).start();
    }

    private void sendExported(String mime) {
        if (exported == null || !exported.exists()) { toast("אין קובץ מיוצא"); return; }
        sendPath(exported.getAbsolutePath(), mime);
    }

    private void sendUri(Uri uri, String mime) {
        if (Prefs.getBool(this,"bot_fallback",false)) {
            TelegramBotSender.sendFile(this, Prefs.get(this,"bot_token",""), Prefs.get(this,"bot_chat",""), uri, caption.getText().toString(), mime, (ok, body) -> UiThread.run(() -> status.setText((ok ? "נשלח" : "נכשל") + "\n" + trim(body))));
        } else {
            try {
                File f = FileUtil.copyToCache(this, uri, "send_original");
                sendPath(f.getAbsolutePath(), mime);
            } catch (Throwable t) { status.setText("הכנת קובץ לשליחה נכשלה: " + t.getMessage()); }
        }
    }

    private void sendPath(String path, String mime) {
        String channel = Prefs.get(this,"channel_username","");
        status.setText("שולח לערוץ " + channel + "...");
        TdlibReflectClient.get(this).sendToUsername(channel, caption.getText().toString(), path, mime, (ok, msg) -> UiThread.run(() -> {
            if (ok || !Prefs.getBool(this,"bot_fallback",false)) status.setText(msg);
            else status.setText(msg + "\nTDLib נכשל. אם הפעלת Bot API, שלח דרך מסך שליחה עם URI מקורי.");
        }));
    }

    private String trim(String s) { return s == null ? "" : (s.length() > 900 ? s.substring(0,900) + "..." : s); }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
