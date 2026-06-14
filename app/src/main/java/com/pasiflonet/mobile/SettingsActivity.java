package com.pasiflonet.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;

import com.pasiflonet.mobile.ui.UltraUi;
import com.pasiflonet.mobile.util.Prefs;

public class SettingsActivity extends Activity {
    EditText channel, watermark, botToken, botChat, apiId, apiHash;
    CheckBox botFallback;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        UltraUi.setup(this);
        LinearLayout root = new LinearLayout(this);
        setContentView(UltraUi.page(this, root));
        root.addView(UltraUi.title(this, "הגדרות Azretr Ultra"));
        root.addView(UltraUi.sub(this, "הפרטים נשמרים מקומית במכשיר. לא לשים כאן טוקנים אם המכשיר לא מאובטח."));

        apiId = UltraUi.input(this, "api_id של Telegram"); apiId.setInputType(InputType.TYPE_CLASS_NUMBER); apiId.setText(Prefs.get(this,"api_id", ""));
        apiHash = UltraUi.input(this, "api_hash של Telegram"); apiHash.setText(Prefs.get(this,"api_hash", ""));
        channel = UltraUi.input(this, "ערוץ יעד TDLib — למשל @myChannel"); channel.setText(Prefs.get(this,"channel_username", ""));
        watermark = UltraUi.input(this, "סימן מים ברירת מחדל"); watermark.setText(Prefs.get(this,"watermark", "Azretr Ultra"));
        botToken = UltraUi.input(this, "Bot Token אופציונלי לגיבוי"); botToken.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); botToken.setText(Prefs.get(this,"bot_token", ""));
        botChat = UltraUi.input(this, "Chat ID / @channel לגיבוי Bot API"); botChat.setText(Prefs.get(this,"bot_chat", ""));
        botFallback = new CheckBox(this); botFallback.setText("אפשר מצב גיבוי Bot API לשליחה"); botFallback.setTextColor(UltraUi.TEXT); botFallback.setTextSize(16); botFallback.setChecked(Prefs.getBool(this,"bot_fallback", false));

        LinearLayout c = UltraUi.card(this);
        c.addView(UltraUi.text(this, "Telegram / TDLib", 20, UltraUi.TEXT, true));
        c.addView(apiId); c.addView(apiHash); c.addView(channel); root.addView(c);
        LinearLayout m = UltraUi.card(this);
        m.addView(UltraUi.text(this, "מדיה ושליחה", 20, UltraUi.TEXT, true));
        m.addView(watermark); m.addView(botFallback); m.addView(botToken); m.addView(botChat); root.addView(m);

        Button save = UltraUi.button(this, "שמור הגדרות");
        save.setOnClickListener(v -> {
            Prefs.set(this,"api_id", apiId.getText().toString().trim());
            Prefs.set(this,"api_hash", apiHash.getText().toString().trim());
            Prefs.set(this,"channel_username", channel.getText().toString().trim());
            Prefs.set(this,"watermark", watermark.getText().toString());
            Prefs.set(this,"bot_token", botToken.getText().toString().trim());
            Prefs.set(this,"bot_chat", botChat.getText().toString().trim());
            Prefs.setBool(this,"bot_fallback", botFallback.isChecked());
            Toast.makeText(this, "נשמר", Toast.LENGTH_SHORT).show();
        });
        root.addView(save);
    }
}
