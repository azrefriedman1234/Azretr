# Azretr mobile portrait overlay

החבילה הזאת היא **overlay** שמיועדת להיפרס מעל הריפו המקורי `azrefriedman1234/Azretr`.

הספריות המקומיות נשארו מקומיות כמו שביקשת:
- `app/libs/tdlib.aar`
- `app/libs/ffmpeg-kit-full-gpl-6.0.aar`

## מה השתנה
- האפליקציה נעולה ל־Portrait בלבד.
- מסך העריכה חולק ל־50% עליון לתצוגה/עריכה ו־50% תחתון לטקסט/כפתורים.
- המסכים הוקטנו והותאמו למסכים קטנים במובייל.
- נוסף מסך הגדרות לקובץ עוגיות TikTok.
- נוסף checkbox במסך העריכה ליצוא חבילת TikTok עם כותרת.

## חשוב על TikTok
כרגע הוספתי **יצוא חבילת TikTok**: אחרי עיבוד הסרטון נשמרים ל־Downloads:
- קובץ וידאו MP4
- קובץ JSON עם title/caption
- עותק של קובץ העוגיות

זה מכין את החומרים ל-uploader שמבוסס על עוגיות, אבל לא מעלה ישירות ל-TikTok מתוך הריפו הזה כי בריפו הנוכחי אין מנגנון העלאה לטיקטוק באנדרואיד.

## איך להחיל על הריפו המקורי
1. הורד או clone את הריפו המקורי.
2. חלץ את ה-ZIP הזה **לתוך השורש של הריפו** ואשר דריסה של קבצים.
3. ודא ששתי הספריות המקומיות עדיין נמצאות ב־`app/libs/`.

## clone דרך Termux
```bash
pkg update -y
pkg install -y git openjdk-17
mkdir -p ~/projects
cd ~/projects
git clone https://github.com/azrefriedman1234/Azretr.git
cd Azretr
```

## החלת ה-overlay ב-Termux
אם קובץ ה-ZIP אצלך בתיקיית Download:
```bash
pkg install -y unzip
cd ~/projects/Azretr
unzip -o /storage/emulated/0/Download/Azretr_mobile_portrait_overlay.zip -d .
```

## בנייה ב-Termux
בריפו הזה אין wrapper מלא, לכן משתמשים ב-Gradle מותקן מקומית או מתוך Debian/Ubuntu ב-Termux.

### אפשרות מומלצת: Termux + Gradle
```bash
pkg install -y gradle
cd ~/projects/Azretr
gradle assembleDebug --stacktrace --no-daemon --refresh-dependencies
```

ה-APK ייצא בנתיב:
```bash
app/build/outputs/apk/debug/app-debug.apk
```

## פוש ל-GitHub מתוך Termux
```bash
cd ~/projects/Azretr
git status
git add .
git commit -m "Portrait mobile UI + TikTok export package settings"
git push origin main
```

## בילד דרך GitHub Actions
אחרי הפוש:
1. פתח את הריפו ב-GitHub.
2. היכנס ללשונית **Actions**.
3. בחר workflow בשם **Android Build CI**.
4. המתן ל-run.
5. בסיום הורד את artifact בשם **Azretr-debug-apk**.

## בדיקות שכדאי לעשות אחרי ההחלה
- שהאפליקציה נפתחת רק לאורך.
- שבמסך העריכה הפריוויו נמצא בחצי העליון.
- שבחצי התחתון יש את הטקסט, הכפתורים ובלוק TikTok.
- שב־Settings אפשר לבחור קובץ cookies.
- שביצוא TikTok נוצרים קבצים ב־Downloads/Pasiflonet/TikTokExport.
