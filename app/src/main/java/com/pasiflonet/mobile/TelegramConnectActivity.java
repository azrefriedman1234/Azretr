package com.pasiflonet.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;

import com.pasiflonet.mobile.bridge.TdlibReflectClient;
import com.pasiflonet.mobile.ui.UltraUi;
import com.pasiflonet.mobile.util.Prefs;
import com.pasiflonet.mobile.util.UiThread;

public class TelegramConnectActivity extends Activity {
    TextView log;
    EditText apiId, apiHash, phone, code, pass;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        UltraUi.setup(this);
        LinearLayout root = new LinearLayout(this);
        setContentView(UltraUi.page(this, root));
        root.addView(UltraUi.title(this, "חיבור טלגרם TDLib"));
        root.addView(UltraUi.sub(this, "החיבור עובד מול ה־tdlib.aar המקומי מתוך app/libs. אין הורדה חיצונית בזמן הקימפול."));

        apiId = UltraUi.input(this, "api_id"); apiId.setInputType(InputType.TYPE_CLASS_NUMBER); apiId.setText(Prefs.get(this,"api_id", ""));
        apiHash = UltraUi.input(this, "api_hash"); apiHash.setText(Prefs.get(this,"api_hash", ""));
        phone = UltraUi.input(this, "מספר טלפון בפורמט בינלאומי +972...");
        code = UltraUi.input(this, "קוד שקיבלת בטלגרם"); code.setInputType(InputType.TYPE_CLASS_NUMBER);
        pass = UltraUi.input(this, "סיסמת 2FA אם יש"); pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        log = UltraUi.sub(this, "מוכן להפעלה");

        LinearLayout form = UltraUi.card(this);
        form.addView(UltraUi.text(this, "פרטי התחברות", 20, UltraUi.TEXT, true));
        form.addView(apiId); form.addView(apiHash); form.addView(phone); form.addView(code); form.addView(pass);
        root.addView(form);

        Button start = UltraUi.button(this, "1. הפעל TDLib ושלח פרמטרים");
        start.setOnClickListener(v -> startTd()); root.addView(start);
        Button sendPhone = UltraUi.ghost(this, "2. שלח מספר טלפון");
        sendPhone.setOnClickListener(v -> TdlibReflectClient.get(this).setPhone(phone.getText().toString().trim())); root.addView(sendPhone);
        Button sendCode = UltraUi.ghost(this, "3. שלח קוד אימות");
        sendCode.setOnClickListener(v -> TdlibReflectClient.get(this).checkCode(code.getText().toString().trim())); root.addView(sendCode);
        Button sendPass = UltraUi.ghost(this, "4. שלח סיסמת 2FA");
        sendPass.setOnClickListener(v -> TdlibReflectClient.get(this).checkPassword(pass.getText().toString())); root.addView(sendPass);

        LinearLayout lc = UltraUi.card(this);
        lc.addView(UltraUi.text(this, "לוג חיבור", 20, UltraUi.TEXT, true));
        lc.addView(log);
        root.addView(lc);
    }

    private void startTd() {
        Prefs.set(this, "api_id", apiId.getText().toString().trim());
        Prefs.set(this, "api_hash", apiHash.getText().toString().trim());
        TdlibReflectClient td = TdlibReflectClient.get(this);
        td.start(line -> UiThread.run(() -> log.append("\n" + line)));
        int id = 0;
        try { id = Integer.parseInt(apiId.getText().toString().trim()); } catch (Exception ignored) {}
        td.setParameters(id, apiHash.getText().toString().trim());
        td.checkEncryptionKey();
    }
}
