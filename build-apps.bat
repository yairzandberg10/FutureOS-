@echo off
rem Double-click to build FutureOS apps: a window opens, you tick the apps to
rem SKIP, and everything else is built one app at a time. Logic is in
rem build-apps.ps1 next to this file. The console stays open only if
rem something failed, so the error can be read.
title FutureOS build
powershell.exe -NoProfile -ExecutionPolicy Bypass -STA -File "%~dp0build-apps.ps1"
if errorlevel 1 (
    echo.
    echo Press any key to close this window.
    pause >nul
)
