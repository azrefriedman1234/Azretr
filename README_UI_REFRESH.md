Azretr UI Refresh Overlay

מה בפנים
- activity_main.xml
- activity_details.xml
- activity_settings.xml
- item_message_row.xml
- item_chat.xml
- drawable resources
- colors.xml

איך להחיל
1. חלץ את ה-ZIP מעל שורש הריפו Azretr.
2. ודא שלא נמחקו app/libs/tdlib.aar ו-app/libs/ffmpeg-kit-full-gpl-6.0.aar
3. בצע:
   git add .
   git commit -m "Full UI refresh for small mobile screens"
   git push origin main

הערות
- השמות וה-IDs נשמרו לפי הקוד הקיים כדי לא לשבור את MainActivity / DetailsActivity / SettingsActivity / ChatAdapter.
- זה overlay עיצובי ולא מחליף את הספריות המקומיות.
