# Azretr Ultra

אפליקציית Android עברית RTL לניטור, עריכת מדיה ושליחה לערוץ Telegram.

## מה כלול

- ממשק עברי מלא ו־RTL.
- התאמה למסכים מתקפלים כמו Galaxy Z Fold 6 באמצעות Activities גמישות ו־resizeableActivity.
- חיבור Telegram דרך TDLib מקומי בלבד (`app/libs/tdlib.aar`).
- שימוש ב־FFmpeg Kit מקומי בלבד (`app/libs/ffmpeg-kit-full-gpl-6.0.aar`).
- עורך מדיה:
  - בחירת תמונה/וידאו.
  - סימן מים נגרר באצבע.
  - טשטוש מותאם אישית על תמונות באמצעות סימון מלבנים.
  - ייצוא תמונה עם סימן מים וטשטוש.
  - עיבוד וידאו בסיסי עם FFmpeg אם ה־AAR קיים.
- שליחה לערוץ:
  - דרך TDLib והחשבון המחובר, כמו באפליקציה המקורית.
  - מצב גיבוי אופציונלי דרך Bot API.
- מקורות מידע:
  - GDELT.
  - מקורות רשמיים בישראל.
  - תעופה / ADS-B / OpenSky.
  - חירום, כבאות, מד״א, פיקוד העורף.
- GitHub Actions לקימפול APK.

## חשוב לגבי AAR

הפרויקט לא מוריד TDLib או FFmpeg מהרשת בזמן קימפול. הוא משתמש רק בקבצים מקומיים מתוך:

```text
app/libs/
```

השאר את הקבצים שכבר קיימים אצלך בריפו:

```text
app/libs/tdlib.aar
app/libs/ffmpeg-kit-full-gpl-6.0.aar
```

## פקודות Termux להחלפה בטוחה

```bash
cd ~/storage/downloads
rm -rf ~/AzretrUltra_NEW
unzip -o AzretrUltra_FULL.zip -d ~/AzretrUltra_NEW

cd ~/Azretr
mkdir -p ~/azretr_libs_backup
cp -av app/libs/*.aar ~/azretr_libs_backup/ 2>/dev/null || true
cp -av app/libs/*.jar ~/azretr_libs_backup/ 2>/dev/null || true

rsync -av --delete \
  --exclude ".git" \
  --exclude "app/libs" \
  ~/AzretrUltra_NEW/AzretrUltra/ ~/Azretr/

mkdir -p app/libs
cp -av ~/azretr_libs_backup/* app/libs/ 2>/dev/null || true

git status
git add -A
git commit -m "Upgrade Azretr Ultra full media TDLib FFmpeg tools"
git push
```

אחרי ה־push:

```text
GitHub → Azretr → Actions → Build Android APK → Artifacts
```
