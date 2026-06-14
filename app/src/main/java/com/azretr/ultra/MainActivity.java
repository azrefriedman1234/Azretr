package com.azretr.ultra;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(6, 17, 31);
    private final int CARD = Color.rgb(16, 32, 51);
    private final int CARD2 = Color.rgb(21, 45, 70);
    private final int TXT = Color.WHITE;
    private final int MUTED = Color.rgb(174, 196, 216);
    private final int ACCENT = Color.rgb(0, 229, 255);
    private final int GREEN = Color.rgb(76, 255, 158);
    private LinearLayout root;
    private LinearLayout content;
    private LinearLayout host;
    private boolean wide;
    private SharedPreferences prefs;

    private static final class Tool {
        final String id, icon, title, sub;
        Tool(String id, String icon, String title, String sub) { this.id = id; this.icon = icon; this.title = title; this.sub = sub; }
    }

    private final Tool[] tools = new Tool[]{
            new Tool("intel", "⚡", "מודיעין מהיר", "חיפוש כתבות ודיווחים פתוחים בזמן אמת"),
            new Tool("verify", "✅", "אימות דיווחים", "בודק כמה מקורות תומכים בטענה"),
            new Tool("telegram", "✈️", "חמ״ל טלגרם", "הכנה לחיבור TDLib, ניטור ושליחה"),
            new Tool("flights", "🛫", "מעקב טיסות", "קישורים מהירים לאזור ישראל והמזרח התיכון"),
            new Tool("map", "🗺️", "מפה וניווט", "פתיחת Waze / Google Maps לפי כתובת"),
            new Tool("report", "🧾", "דוח אירוע", "יצירת דוח מסודר לשיתוף"),
            new Tool("sources", "🌐", "מקורות OSINT", "מאגר קישורים שימושי לבדיקה מהירה"),
            new Tool("watch", "👁️", "יומן תצפיות", "רישום תצפיות מקומי על המכשיר"),
            new Tool("warroom", "📡", "חדר מלחמה", "תצוגת סטטוס למסך גדול / Fold"),
            new Tool("settings", "⚙️", "הגדרות", "ערוצים, מילות מפתח ומראה"),
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("azretr_ultra", MODE_PRIVATE);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        wide = getResources().getConfiguration().screenWidthDp >= 700;
        buildShell();
        dashboard();
    }

    private void buildShell() {
        root = new LinearLayout(this);
        root.setOrientation(wide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(BG);
        setContentView(root);

        if (wide) {
            ScrollView sideScroll = new ScrollView(this);
            LinearLayout side = new LinearLayout(this);
            side.setOrientation(LinearLayout.VERTICAL);
            side.setPadding(dp(18), dp(18), dp(18), dp(18));
            sideScroll.addView(side);
            root.addView(sideScroll, new LinearLayout.LayoutParams(dp(330), ViewGroup.LayoutParams.MATCH_PARENT));
            side.addView(title("Azretr Ultra", 26));
            side.addView(label("גרסת חמ״ל מתקפלת • עברית מלאה • RTL", MUTED));
            side.addView(space(12));
            for (Tool t : tools) side.addView(navButton(t));
            content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dp(20), dp(20), dp(20), dp(20));
            root.addView(content, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
            host = content;
        } else {
            content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dp(16), dp(16), dp(16), dp(16));
            root.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            host = content;
        }
    }

    private View navButton(final Tool t) {
        TextView v = label(t.icon + "  " + t.title, TXT);
        v.setTextSize(17);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setPadding(dp(14), dp(14), dp(14), dp(14));
        v.setBackground(round(CARD, dp(18), 1, Color.argb(40, 255,255,255)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(5), 0, dp(5));
        v.setLayoutParams(lp);
        v.setOnClickListener(x -> showTool(t.id));
        return v;
    }

    private void baseScreen(String heading, String sub) {
        host.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(wide ? dp(10) : 0, 0, wide ? dp(10) : 0, dp(20));
        scroll.addView(box);
        host.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
        content = box;
        if (!wide) {
            Button back = btn("בית", CARD2);
            back.setOnClickListener(v -> { buildShell(); dashboard(); });
            box.addView(back);
        }
        box.addView(title(heading, wide ? 32 : 28));
        box.addView(label(sub, MUTED));
        box.addView(space(14));
    }

    private void dashboard() {
        host.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0,0,0,dp(24));
        scroll.addView(box);
        host.addView(scroll, new LinearLayout.LayoutParams(-1,-1));
        LinearLayout hero = cardBox();
        hero.setPadding(dp(20), dp(22), dp(20), dp(22));
        hero.setBackground(gradient());
        hero.addView(title("Azretr Ultra", wide ? 36 : 30));
        hero.addView(label("מערכת חמ״ל OSINT בעברית — מותאמת במיוחד למסך הרחב של Galaxy Z Fold 6", Color.rgb(217, 247, 255)));
        hero.addView(space(12));
        hero.addView(rowStats());
        box.addView(hero);

        if (!wide) box.addView(label("בחר כלי:", TXT));
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(wide ? 3 : 1);
        grid.setUseDefaultMargins(true);
        for (final Tool t : tools) grid.addView(toolCard(t));
        box.addView(grid);
    }

    private View rowStats() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(stat("10", "כלים"));
        row.addView(stat("RTL", "עברית"));
        row.addView(stat("Fold", "מסך גדול"));
        return row;
    }

    private View stat(String num, String cap) {
        LinearLayout s = new LinearLayout(this);
        s.setOrientation(LinearLayout.VERTICAL);
        s.setGravity(Gravity.CENTER);
        s.setBackground(round(Color.argb(70, 255,255,255), dp(16), 1, Color.argb(40,255,255,255)));
        s.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        lp.setMargins(dp(4),0,dp(4),0);
        s.setLayoutParams(lp);
        TextView n = title(num, 21); n.setGravity(Gravity.CENTER);
        TextView c = label(cap, Color.rgb(210,230,240)); c.setGravity(Gravity.CENTER);
        s.addView(n); s.addView(c);
        return s;
    }

    private View toolCard(final Tool t) {
        LinearLayout box = cardBox();
        box.setPadding(dp(18), dp(16), dp(18), dp(16));
        GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
        glp.width = wide ? dp(250) : GridLayout.LayoutParams.MATCH_PARENT;
        glp.setMargins(dp(5), dp(7), dp(5), dp(7));
        box.setLayoutParams(glp);
        TextView icon = title(t.icon, 28);
        TextView h = title(t.title, 20);
        TextView s = label(t.sub, MUTED);
        box.addView(icon); box.addView(h); box.addView(s);
        box.setOnClickListener(v -> showTool(t.id));
        return box;
    }

    private void showTool(String id) {
        if (id.equals("intel")) intel();
        else if (id.equals("verify")) verify();
        else if (id.equals("telegram")) telegram();
        else if (id.equals("flights")) flights();
        else if (id.equals("map")) mapTool();
        else if (id.equals("report")) report();
        else if (id.equals("sources")) sources();
        else if (id.equals("watch")) watchLog();
        else if (id.equals("warroom")) warroom();
        else settings();
    }

    private void intel() {
        baseScreen("⚡ מודיעין מהיר", "חיפוש פתוח ב־GDELT לפי מילות מפתח בעברית/אנגלית.");
        EditText q = input("לדוגמה: אזעקה תל אביב / fire israel / נתבג");
        Button search = btn("חפש דיווחים", ACCENT);
        LinearLayout results = cardBox();
        results.addView(label("התוצאות יוצגו כאן", MUTED));
        content.addView(q); content.addView(search); content.addView(results);
        search.setOnClickListener(v -> {
            String query = q.getText().toString().trim();
            if (query.isEmpty()) { toast("כתוב מילת חיפוש"); return; }
            results.removeAllViews(); results.addView(label("מחפש...", MUTED));
            fetchGdelt(query, (ok, msg, articles) -> runOnUiThread(() -> {
                results.removeAllViews();
                results.addView(title(ok ? "נמצאו דיווחים" : "שגיאה", 21));
                results.addView(label(msg, ok ? GREEN : Color.rgb(255,140,140)));
                for (Article a : articles) results.addView(articleView(a));
            }));
        });
    }

    private void verify() {
        baseScreen("✅ אימות דיווחים", "הכנס טענה. האפליקציה מחפשת מקורות פתוחים ומחזירה ציון בסיסי.");
        EditText claim = input("לדוגמה: שריפה גדולה בראשון לציון");
        Button run = btn("בדוק אמינות", ACCENT);
        LinearLayout out = cardBox();
        out.addView(label("הבדיקה אינה תחליף לאימות מקצועי — היא כלי עזר מהיר בלבד.", MUTED));
        content.addView(claim); content.addView(run); content.addView(out);
        run.setOnClickListener(v -> {
            String c = claim.getText().toString().trim();
            if (c.isEmpty()) { toast("כתוב טענה לבדיקה"); return; }
            out.removeAllViews(); out.addView(label("בודק מקורות...", MUTED));
            fetchGdelt(c, (ok, msg, articles) -> runOnUiThread(() -> {
                out.removeAllViews();
                int score = Math.min(100, articles.size() * 12);
                String level = score >= 70 ? "גבוה" : score >= 35 ? "בינוני" : "נמוך";
                out.addView(title("ציון ראשוני: " + score + "% — " + level, 23));
                out.addView(label("נמצאו " + articles.size() + " מקורות פתוחים. " + msg, MUTED));
                for (int i = 0; i < Math.min(5, articles.size()); i++) out.addView(articleView(articles.get(i)));
            }));
        });
    }

    private void telegram() {
        baseScreen("✈️ חמ״ל טלגרם", "מסך הכנה לחיבור TDLib: שמירת פרטי API, ערוץ יעד ומילות ניטור.");
        EditText apiId = input("Telegram API ID"); apiId.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText apiHash = input("Telegram API Hash");
        EditText phone = input("מספר טלפון כולל קידומת בינלאומית");
        EditText channel = input("ערוץ יעד לדוגמה @my_channel");
        EditText keywords = input("מילות ניטור: אזעקה, שריפה, חסימה, נתבג");
        apiId.setText(prefs.getString("apiId", "")); apiHash.setText(prefs.getString("apiHash", "")); phone.setText(prefs.getString("phone", "")); channel.setText(prefs.getString("channel", "")); keywords.setText(prefs.getString("keywords", ""));
        Button save = btn("שמור הגדרות", ACCENT);
        Button docs = btn("פתח Telegram API", CARD2);
        LinearLayout info = cardBox();
        info.addView(title("מה מוכן כאן?", 21));
        info.addView(label("• מסך חיבור בעברית\n• שמירת הגדרות מקומית\n• נקודת הרחבה לקוד TDLib אמיתי\n• בלי שליחת מידע לשרת חיצוני", MUTED));
        content.addView(apiId); content.addView(apiHash); content.addView(phone); content.addView(channel); content.addView(keywords); content.addView(save); content.addView(docs); content.addView(info);
        save.setOnClickListener(v -> {
            prefs.edit().putString("apiId", apiId.getText().toString()).putString("apiHash", apiHash.getText().toString()).putString("phone", phone.getText().toString()).putString("channel", channel.getText().toString()).putString("keywords", keywords.getText().toString()).apply();
            toast("נשמר מקומית במכשיר");
        });
        docs.setOnClickListener(v -> open("https://my.telegram.org/apps"));
    }

    private void flights() {
        baseScreen("🛫 מעקב טיסות", "קישורים מהירים למפות טיסה פתוחות. חלק ממטוסים צבאיים לא משדרים ולא יוצגו.");
        addLink("OpenSky - אזור ישראל", "https://opensky-network.org/network/explorer");
        addLink("ADS-B Exchange", "https://globe.adsbexchange.com/");
        addLink("FlightRadar24", "https://www.flightradar24.com/31.8,35.2/7");
        LinearLayout tip = cardBox();
        tip.addView(title("טיפ Fold 6", 22));
        tip.addView(label("פתח מפה במסך מפוצל לצד Azretr Ultra כדי לראות דיווחים ומפה יחד.", MUTED));
        content.addView(tip);
    }

    private void mapTool() {
        baseScreen("🗺️ מפה וניווט", "הכנס כתובת או שם מקום ופתח במהירות במפות.");
        EditText place = input("לדוגמה: רחוב הרצל תל אביב");
        Button maps = btn("פתח Google Maps", ACCENT);
        Button waze = btn("פתח Waze", CARD2);
        content.addView(place); content.addView(maps); content.addView(waze);
        maps.setOnClickListener(v -> open("https://www.google.com/maps/search/?api=1&query=" + enc(place.getText().toString())));
        waze.setOnClickListener(v -> open("https://waze.com/ul?q=" + enc(place.getText().toString())));
    }

    private void report() {
        baseScreen("🧾 דוח אירוע", "צור דוח בעברית לשיתוף מהיר בוואטסאפ/טלגרם/מייל.");
        EditText type = input("סוג אירוע");
        EditText loc = input("מיקום");
        EditText sev = input("חומרה: נמוכה / בינונית / גבוהה");
        EditText details = input("פרטים חופשיים"); details.setMinLines(4);
        Button make = btn("צור ושתף דוח", ACCENT);
        content.addView(type); content.addView(loc); content.addView(sev); content.addView(details); content.addView(make);
        make.setOnClickListener(v -> {
            String text = "דוח אירוע - Azretr Ultra\n" +
                    "זמן: " + new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("he", "IL")).format(new Date()) + "\n" +
                    "סוג: " + type.getText() + "\n" +
                    "מיקום: " + loc.getText() + "\n" +
                    "חומרה: " + sev.getText() + "\n" +
                    "פרטים: " + details.getText() + "\n";
            share(text);
        });
    }

    private void sources() {
        baseScreen("🌐 מקורות OSINT", "מאגר קישורים מהיר לפתיחה בדפדפן.");
        addLink("GDELT Project", "https://www.gdeltproject.org/");
        addLink("Google News", "https://news.google.com/");
        addLink("פיקוד העורף", "https://www.oref.org.il/");
        addLink("משטרת ישראל", "https://www.gov.il/he/departments/israel_police");
        addLink("שירות המטאורולוגי", "https://ims.gov.il/");
        addLink("OpenStreetMap", "https://www.openstreetmap.org/");
    }

    private void watchLog() {
        baseScreen("👁️ יומן תצפיות", "רשום תצפית מקומית ושמור אותה במכשיר.");
        EditText e = input("כתוב תצפית חדשה"); e.setMinLines(3);
        Button save = btn("הוסף ליומן", ACCENT);
        Button clear = btn("נקה יומן", CARD2);
        LinearLayout log = cardBox();
        content.addView(e); content.addView(save); content.addView(clear); content.addView(log);
        Runnable refresh = () -> { log.removeAllViews(); log.addView(title("יומן", 21)); log.addView(label(prefs.getString("log", "אין תצפיות עדיין"), MUTED)); };
        refresh.run();
        save.setOnClickListener(v -> {
            String old = prefs.getString("log", "");
            String now = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date());
            prefs.edit().putString("log", now + " — " + e.getText() + "\n\n" + old).apply();
            e.setText(""); refresh.run();
        });
        clear.setOnClickListener(v -> { prefs.edit().remove("log").apply(); refresh.run(); });
    }

    private void warroom() {
        baseScreen("📡 חדר מלחמה", "תצוגת סטטוס גדולה למסך פתוח של Galaxy Z Fold 6.");
        LinearLayout panel = cardBox();
        panel.setPadding(dp(22), dp(22), dp(22), dp(22));
        panel.addView(title("סטטוס מערכת: פעילה", wide ? 34 : 26));
        panel.addView(label("מקורות: GDELT / מפות / טיסות / יומן מקומי / דוחות", GREEN));
        panel.addView(space(10));
        panel.addView(title("מילות ניטור", 23));
        panel.addView(label(prefs.getString("keywords", "אזעקה, שריפה, תאונה, חסימה, נתבג"), MUTED));
        panel.addView(space(10));
        panel.addView(title("ערוץ יעד", 23));
        panel.addView(label(prefs.getString("channel", "לא הוגדר"), MUTED));
        content.addView(panel);
    }

    private void settings() {
        baseScreen("⚙️ הגדרות", "ניהול בסיסי של פרויקט Azretr Ultra.");
        EditText name = input("שם חמ״ל / פרויקט"); name.setText(prefs.getString("projectName", "Azretr Ultra"));
        EditText keys = input("מילות ניטור ברירת מחדל"); keys.setText(prefs.getString("keywords", "אזעקה, שריפה, תאונה, חסימה, נתבג"));
        Button save = btn("שמור", ACCENT);
        Button copy = btn("העתק פרטי מערכת", CARD2);
        content.addView(name); content.addView(keys); content.addView(save); content.addView(copy);
        save.setOnClickListener(v -> { prefs.edit().putString("projectName", name.getText().toString()).putString("keywords", keys.getText().toString()).apply(); toast("נשמר"); });
        copy.setOnClickListener(v -> copy("Azretr Ultra / Android / RTL / Fold-ready / package com.azretr.ultra"));
    }

    private void addLink(String label, String url) {
        Button b = btn(label, CARD2);
        b.setOnClickListener(v -> open(url));
        content.addView(b);
    }

    private void fetchGdelt(String query, GdeltCallback cb) {
        new Thread(() -> {
            ArrayList<Article> list = new ArrayList<>();
            try {
                String url = "https://api.gdeltproject.org/api/v2/doc/doc?query=" + enc(query) + "&format=json&mode=artlist&maxrecords=20&sort=datedesc";
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(12000);
                c.setReadTimeout(12000);
                c.setRequestProperty("User-Agent", "AzretrUltra/1.0 Android");
                BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line; while ((line = r.readLine()) != null) sb.append(line);
                JSONObject json = new JSONObject(sb.toString());
                JSONArray arr = json.optJSONArray("articles");
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.getJSONObject(i);
                        list.add(new Article(o.optString("title"), o.optString("sourceCountry"), o.optString("domain"), o.optString("url")));
                    }
                }
                cb.done(true, "מקור: GDELT Doc API", list);
            } catch (Exception e) {
                cb.done(false, e.getClass().getSimpleName() + ": " + e.getMessage(), list);
            }
        }).start();
    }

    private View articleView(final Article a) {
        LinearLayout box = cardBox();
        box.setBackground(round(Color.rgb(12, 27, 43), dp(16), 1, Color.argb(40,0,229,255)));
        box.addView(title(a.title.length() == 0 ? "ללא כותרת" : a.title, 17));
        box.addView(label(a.domain + " • " + a.country, MUTED));
        box.setOnClickListener(v -> { if (a.url.length() > 0) open(a.url); });
        return box;
    }

    private interface GdeltCallback { void done(boolean ok, String msg, ArrayList<Article> articles); }
    private static final class Article { final String title, country, domain, url; Article(String t, String c, String d, String u){ title=t; country=c; domain=d; url=u; } }

    private LinearLayout cardBox() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(14), dp(16), dp(14));
        l.setBackground(round(CARD, dp(22), 1, Color.argb(36, 255,255,255)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(8), 0, dp(8));
        l.setLayoutParams(lp);
        return l;
    }

    private TextView title(String s, int sp) { TextView v = label(s, TXT); v.setTextSize(sp); v.setTypeface(Typeface.DEFAULT_BOLD); return v; }
    private TextView label(String s, int color) { TextView v = new TextView(this); v.setText(s); v.setTextColor(color); v.setTextSize(15); v.setLineSpacing(3, 1); v.setGravity(Gravity.RIGHT); v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); return v; }
    private View space(int h) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h))); return v; }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint); e.setHintTextColor(Color.rgb(120,150,175)); e.setTextColor(TXT); e.setTextSize(16); e.setSingleLine(false); e.setMinLines(1); e.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL); e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); e.setPadding(dp(14), dp(10), dp(14), dp(10)); e.setBackground(round(Color.rgb(9, 23, 38), dp(16), 1, Color.argb(90,0,229,255)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(6), 0, dp(6)); e.setLayoutParams(lp);
        return e;
    }

    private Button btn(String s, int color) {
        Button b = new Button(this); b.setText(s); b.setTextColor(color == ACCENT ? Color.rgb(3, 18, 28) : TXT); b.setTextSize(16); b.setTypeface(Typeface.DEFAULT_BOLD); b.setAllCaps(false); b.setGravity(Gravity.CENTER); b.setBackground(round(color, dp(16), 0, color));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(52)); lp.setMargins(0, dp(7), 0, dp(7)); b.setLayoutParams(lp); return b;
    }

    private GradientDrawable round(int color, int radius, int stroke, int strokeColor) { GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(radius); if (stroke > 0) g.setStroke(stroke, strokeColor); return g; }
    private GradientDrawable gradient() { GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{ Color.rgb(0, 229, 255), Color.rgb(24, 72, 180), Color.rgb(9, 16, 35)}); g.setCornerRadius(dp(26)); return g; }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    private String enc(String s) { try { return URLEncoder.encode(s == null ? "" : s, "UTF-8"); } catch (Exception e) { return ""; } }
    private void open(String url) { try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception e) { toast("אין אפליקציה לפתיחת הקישור"); } }
    private void share(String text) { Intent i = new Intent(Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(Intent.EXTRA_TEXT, text); startActivity(Intent.createChooser(i, "שיתוף דוח")); }
    private void copy(String text) { ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Azretr Ultra", text)); toast("הועתק"); }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
