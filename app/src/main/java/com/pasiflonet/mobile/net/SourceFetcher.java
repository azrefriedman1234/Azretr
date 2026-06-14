package com.pasiflonet.mobile.net;

import com.pasiflonet.mobile.model.IntelSource;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public final class SourceFetcher {
    public interface Callback { void onResult(List<IntelSource> list, String error); }
    private SourceFetcher() {}

    public static List<IntelSource> builtIns() {
        List<IntelSource> l = new ArrayList<>();
        l.add(new IntelSource("GDELT ישראל", "חיפוש חדשות בזמן אמת לפי מילת מפתח", "https://api.gdeltproject.org/api/v2/doc/doc", "חדשות"));
        l.add(new IntelSource("OpenSky", "מטוסים גלויים באזור ישראל לפי ADS-B פתוח", "https://opensky-network.org/api/states/all", "תעופה"));
        l.add(new IntelSource("פיקוד העורף", "מקור רשמי להתראות והנחיות לציבור", "https://www.oref.org.il", "רשמי"));
        l.add(new IntelSource("משטרת ישראל", "עדכונים רשמיים ודוברות", "https://www.police.gov.il", "רשמי"));
        l.add(new IntelSource("כבאות והצלה", "דיווחי שריפות וחילוץ", "https://www.gov.il/he/departments/israel_fire_and_rescue_authority", "רשמי"));
        l.add(new IntelSource("מד״א", "עדכוני חירום והצלה", "https://www.mdais.org", "רשמי"));
        l.add(new IntelSource("נתב״ג", "מידע טיסות רשמי", "https://www.iaa.gov.il", "תעופה"));
        l.add(new IntelSource("USGS Earthquakes", "רעידות אדמה גלובליות", "https://earthquake.usgs.gov", "גלובלי"));
        return l;
    }

    public static void searchGdelt(String query, Callback cb) {
        new Thread(() -> {
            try {
                String q = URLEncoder.encode(query == null || query.trim().isEmpty() ? "ישראל" : query.trim(), "UTF-8");
                URL u = new URL("https://api.gdeltproject.org/api/v2/doc/doc?query=" + q + "&mode=artlist&format=json&maxrecords=25&sort=hybridrel");
                HttpURLConnection con = (HttpURLConnection) u.openConnection();
                con.setConnectTimeout(15000);
                con.setReadTimeout(20000);
                String body = read(con.getInputStream());
                JSONObject root = new JSONObject(body);
                JSONArray arr = root.optJSONArray("articles");
                List<IntelSource> out = new ArrayList<>();
                if (arr != null) for (int i=0; i<arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    out.add(new IntelSource(o.optString("title"), o.optString("seendate") + " • " + o.optString("domain"), o.optString("url"), "GDELT"));
                }
                cb.onResult(out, null);
            } catch (Throwable t) { cb.onResult(new ArrayList<>(), t.getClass().getSimpleName() + ": " + t.getMessage()); }
        }).start();
    }

    private static String read(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] b = new byte[8192]; int n; while ((n = in.read(b)) > 0) bos.write(b, 0, n);
        return bos.toString("UTF-8");
    }
}
