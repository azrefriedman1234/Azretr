package com.pasiflonet.mobile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;

import com.pasiflonet.mobile.model.IntelSource;
import com.pasiflonet.mobile.net.SourceFetcher;
import com.pasiflonet.mobile.ui.UltraUi;
import com.pasiflonet.mobile.util.UiThread;

import java.util.List;

public class SourcesActivity extends Activity {
    LinearLayout list;
    EditText query;
    TextView status;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        UltraUi.setup(this);
        LinearLayout root = new LinearLayout(this);
        setContentView(UltraUi.page(this, root));
        root.addView(UltraUi.title(this, "מקורות וכלי מידע"));
        root.addView(UltraUi.sub(this, "מקורות מובנים + חיפוש GDELT. אפשר לפתוח מקור בדפדפן, להעתיק קישור או לשלוח לערוץ דרך מסך השליחה."));

        LinearLayout search = UltraUi.card(this);
        query = UltraUi.input(this, "חיפוש מהיר: שריפה, תאונה, אזעקה, נתב״ג...");
        Button go = UltraUi.button(this, "חפש ב־GDELT");
        status = UltraUi.sub(this, "בחר חיפוש או פתח מקור מובנה");
        search.addView(query); search.addView(go); search.addView(status); root.addView(search);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); root.addView(list);

        go.setOnClickListener(v -> searchGdelt());
        render(SourceFetcher.builtIns());
    }

    private void searchGdelt() {
        status.setText("מחפש...");
        SourceFetcher.searchGdelt(query.getText().toString(), (items, err) -> UiThread.run(() -> {
            status.setText(err == null ? "נמצאו " + items.size() + " תוצאות" : "שגיאה: " + err);
            render(items);
        }));
    }

    private void render(List<IntelSource> items) {
        list.removeAllViews();
        for (IntelSource s : items) {
            LinearLayout c = UltraUi.card(this);
            c.addView(UltraUi.text(this, s.title, 18, UltraUi.TEXT, true));
            c.addView(UltraUi.sub(this, s.tag + " • " + s.description));
            Button open = UltraUi.ghost(this, "פתח מקור");
            open.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(s.url))));
            Button send = UltraUi.button(this, "הכן לשליחה לערוץ");
            send.setOnClickListener(v -> {
                Intent i = new Intent(this, ChannelSendActivity.class);
                i.putExtra("text", s.title + "\n" + s.url);
                startActivity(i);
            });
            c.addView(open); c.addView(send); list.addView(c);
        }
    }
}
