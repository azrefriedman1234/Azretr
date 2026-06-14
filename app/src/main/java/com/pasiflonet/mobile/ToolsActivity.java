package com.pasiflonet.mobile;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.*;

import com.pasiflonet.mobile.bridge.FfmpegKitBridge;
import com.pasiflonet.mobile.bridge.TdlibReflectClient;
import com.pasiflonet.mobile.ui.UltraUi;

public class ToolsActivity extends Activity {
    EditText input;
    TextView output;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        UltraUi.setup(this);
        LinearLayout root = new LinearLayout(this);
        setContentView(UltraUi.page(this, root));
        root.addView(UltraUi.title(this, "כלים מהירים"));
        root.addView(UltraUi.sub(this, "כלים קטנים להכנת הודעות, ניקוי טקסט וסטטוס רכיבי מדיה."));

        LinearLayout c = UltraUi.card(this);
        input = UltraUi.input(this, "הדבק כאן טקסט מקור"); input.setMinLines(5);
        output = UltraUi.sub(this, "פלט יופיע כאן");
        Button clean = UltraUi.button(this, "נקה טקסט ויישר עברית");
        Button caption = UltraUi.ghost(this, "הכן קפشن חדשותי");
        Button copy = UltraUi.ghost(this, "העתק פלט");
        c.addView(input); c.addView(clean); c.addView(caption); c.addView(copy); c.addView(output); root.addView(c);

        LinearLayout status = UltraUi.card(this);
        status.addView(UltraUi.text(this, "סטטוס רכיבים", 20, UltraUi.TEXT, true));
        status.addView(UltraUi.sub(this, "TDLib: " + (TdlibReflectClient.get(this).isAvailable() ? "קיים" : "חסר") + "\nFFmpeg: " + (FfmpegKitBridge.isAvailable() ? "קיים" : "חסר")));
        root.addView(status);

        clean.setOnClickListener(v -> output.setText(clean(input.getText().toString())));
        caption.setOnClickListener(v -> output.setText(makeCaption(input.getText().toString())));
        copy.setOnClickListener(v -> {
            ((ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Azretr", output.getText()));
            Toast.makeText(this, "הועתק", Toast.LENGTH_SHORT).show();
        });
    }

    private String clean(String s) {
        return s.replaceAll("[\\t ]+", " ").replaceAll("\\n{3,}", "\\n\\n").trim();
    }
    private String makeCaption(String s) {
        String x = clean(s);
        if (x.isEmpty()) return "עדכון מהיר:\n\nמקור: ";
        return "עדכון מהיר:\n" + x + "\n\nמקור: \n#AzretrUltra";
    }
}
