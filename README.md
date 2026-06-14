# Azretr Ultra

אפליקציית Android בעברית מלאה, RTL, בעיצוב חמ״ל מודרני, מותאמת במיוחד ל־Galaxy Z Fold 6 ולמסכים רחבים.

## מה יש בפרויקט

- ממשק Dashboard מודרני בעברית.
- התאמה למסך מתקפל: תפריט צד במסך רחב ותצוגת כרטיסים במסך רגיל.
- מודיעין מהיר דרך GDELT API.
- אימות דיווחים ראשוני לפי מקורות פתוחים.
- מסך הכנה לחיבור Telegram TDLib ושמירת API ID / API Hash / ערוץ יעד.
- קישורי מעקב טיסות: OpenSky, ADS-B Exchange, FlightRadar24.
- מפה וניווט ל־Google Maps ול־Waze.
- יצירת דוח אירוע לשיתוף.
- מקורות OSINT מהירים.
- יומן תצפיות מקומי.
- GitHub Actions שמקמפל APK אוטומטית ומעלה Artifact.

> הערה חשובה: חיבור Telegram אמיתי דרך TDLib דורש הוספת ספריית TDLib ובניית תהליך Login מלא. בפרויקט הזה יש מסך, שמירת הגדרות ונקודת הרחבה מוכנה, בלי לשמור סודות בשרת חיצוני.

## פקודות Termux לחילוץ והעלאה לגיטהאב

שים את הקובץ `AzretrUltra.zip` בתיקיית Downloads ואז הרץ:

```bash
pkg update -y
pkg install -y git unzip
termux-setup-storage
cd /sdcard/Download
unzip AzretrUltra.zip
cd AzretrUltra

git init
git branch -M main
git add .
git commit -m "Initial Azretr Ultra Android project"
```

### העלאה לריפו חדש

צור ריפו חדש בגיטהאב בשם `AzretrUltra`, ואז:

```bash
git remote add origin https://github.com/azrefriedman1234/AzretrUltra.git
git push -u origin main
```

### העלאה לריפו הקיים Azretr

אם אתה רוצה להעלות לתוך הריפו הקיים במקום הפרויקט הישן:

```bash
git remote add origin https://github.com/azrefriedman1234/Azretr.git
git push -u origin main
```

אם GitHub אומר שהריפו כבר מכיל היסטוריה, עדיף לפתוח ריפו חדש. אם אתה בטוח שאתה רוצה להחליף הכל:

```bash
git push -u origin main --force
```

## איפה מורידים את ה־APK אחרי Push

1. פתח את הריפו בגיטהאב.
2. עבור ל־Actions.
3. פתח את הריצה בשם `Build Android APK`.
4. בתחתית העמוד הורד את Artifact בשם `AzretrUltra-debug-apk`.

## קומפילציה מקומית למי שיש Android SDK

```bash
gradle :app:assembleDebug
```

ה־APK יופיע כאן:

```bash
app/build/outputs/apk/debug/app-debug.apk
```
