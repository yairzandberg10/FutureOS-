@echo off
chcp 65001 >nul
setlocal
rem FutureOS - צריבת system_a בלבד ל-Qin F22 Pro (בלי מחיקת נתונים).
rem שימוש: flash.bat            -> צורב את FutureOS
rem        flash.bat restore    -> מחזיר את system_a המקורי מהגיבוי
set "HERE=%~dp0"
set "PT=%LOCALAPPDATA%\Android\Sdk\platform-tools"
set "ADB=%PT%\adb.exe"
set "FB=%PT%\fastboot.exe"
set "IMG=%HERE%images\system_a.img"
if /i "%1"=="restore" set "IMG=%HERE%restore\system_a.img"

if not exist "%IMG%" ( echo לא נמצא %IMG% & exit /b 1 )
echo.
echo  תמונה: %IMG%
echo  בודק SHA256...
powershell -NoProfile -Command "$h=(Get-FileHash -Algorithm SHA256 '%IMG%').Hash.ToLower(); $exp=((Get-Content (Join-Path (Split-Path '%IMG%') 'SHA256SUMS') | Select-String 'system_a.img').Line -split '\s+')[0]; if($h -ne $exp){Write-Host ' SHA256 לא תואם - עוצר'; exit 1} else {Write-Host ' SHA256 תקין'}"
if errorlevel 1 exit /b 1

echo.
echo  הפעולה תצרוב את מחיצת system_a בטלפון. הנתונים (אפליקציות, הודעות, אנשי קשר) לא נמחקים.
set /p OK= להמשיך? הקלד YES:
if not "%OK%"=="YES" ( echo בוטל. & exit /b 1 )

"%ADB%" get-state >nul 2>&1 && "%ADB%" reboot fastboot
echo  ממתין ל-fastbootd...
rem fastboot ממתין לבד עד שהמכשיר מופיע
"%FB%" getvar is-userspace 2>&1 | findstr /i /c:"is-userspace: yes" >nul
if errorlevel 1 ( echo הטלפון לא ב-fastbootd ^(userspace^). עוצר בלי לצרוב. & exit /b 1 )
"%FB%" getvar current-slot 2>&1 | findstr /i /r /c:"current-slot: *a *$" >nul
if errorlevel 1 ( echo הסלוט הפעיל אינו a. עוצר בלי לצרוב. & exit /b 1 )

"%FB%" flash system_a "%IMG%"
if errorlevel 1 ( echo הצריבה נכשלה. הטלפון עדיין ב-fastbootd - אפשר להריץ flash.bat restore. & exit /b 1 )
"%FB%" reboot
echo  הסתיים. האתחול הראשון יכול לקחת כמה דקות.
