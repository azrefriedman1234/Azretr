package com.pasiflonet.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import com.pasiflonet.mobile.bridge.FfmpegKitBridge;
import com.pasiflonet.mobile.bridge.TdlibReflectClient;
import com.pasiflonet.mobile.ui.UltraUi;
import com.pasiflonet.mobile.util.Prefs;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        UltraUi.setup(this);
        LinearLayout root = new LinearLayout(this);
        setContentView(UltraUi.page(this, root));

        root.addView(UltraUi.title(this, "Azretr Ultra"));
        root.addView(UltraUi.sub(this, "מערכת עברית מודרנית לניטור, עריכת מדיה ושליחה לערוץ — מותאמת למסך מתקפל כמו Galaxy Z Fold 6."));
        UltraUi.addSpace(root, 10);

        LinearLayout status = UltraUi.card(this);
        status.addView(UltraUi.text(this, "סטטוס מערכת", 20, UltraUi.TEXT, true));
        String td = TdlibReflectClient.get(this).isAvailable() ? "TDLib AAR זמין" : "TDLib AAR חסר / לא נטען";
        String ff = FfmpegKitBridge.isAvailable() ? "FFmpeg AAR זמין" : "FFmpeg AAR חסר / לא נטען";
        status.addView(UltraUi.sub(this, td + "\n" + ff + "\nערוץ יעד: " + Prefs.get(this, "channel_username", "לא הוגדר")));
        root.addView(status);

        addFeature(root, "חיבור טלגרם מקורי", "התחברות TDLib עם api_id, api_hash, טלפון, קוד וסיסמת 2FA — כמו באפליקציה המקורית.", TelegramConnectActivity.class, "פתח חיבור");
        addFeature(root, "עורך מדיה מתקדם", "בחירת תמונה/וידאו, סימן מים נגרר, אזורי טשטוש מותאמים, ייצוא ושליחה לערוץ.", MediaEditorActivity.class, "פתח עורך");
        addFeature(root, "שליחה לערוץ", "שליחת טקסט/מדיה לערוץ דרך TDLib, עם מצב גיבוי Bot API אם תרצה.", ChannelSendActivity.class, "פתח שליחה");
        addFeature(root, "מקורות וכלי מודיעין פתוח", "מקורות רשמיים, GDELT, תעופה, חירום וכלי בדיקת מידע מהירה.", SourcesActivity.class, "פתח מקורות");
        addFeature(root, "כלים מהירים", "ניקוי טקסט, הכנת קפشن, תבניות פרסום, הסרת מטא־דאטה בתמונות וסטטוס FFmpeg/TDLib.", ToolsActivity.class, "פתח כלים");
        addFeature(root, "הגדרות", "ערוץ יעד, חתימת סימן מים, מצב שליחה, Bot Token אופציונלי והעדפות ברירת מחדל.", SettingsActivity.class, "פתח הגדרות");
    }

    private void addFeature(LinearLayout root, String title, String desc, Class<?> cls, String btn) {
        LinearLayout c = UltraUi.card(this);
        c.addView(UltraUi.text(this, title, 20, UltraUi.TEXT, true));
        c.addView(UltraUi.sub(this, desc));
        Button b = UltraUi.button(this, btn);
        b.setOnClickListener(v -> startActivity(new Intent(this, cls)));
        c.addView(b);
        root.addView(c);
    }
}
