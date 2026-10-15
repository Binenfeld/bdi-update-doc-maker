@echo off
rem Double-click to build BDIUpdateDocMaker (same as: powershell -ExecutionPolicy Bypass -File scripts\build.ps1).
rem The window stays open at the end so the result can be read. The exit code is the build's (1 = failed).
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build.ps1"
set rc=%errorlevel%
echo.
if %rc% neq 0 (echo The build failed - see the message above.) else (echo Build finished.)
pause
exit /b %rc%
