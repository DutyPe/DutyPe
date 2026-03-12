@echo off
REM ============================================================
REM  DutyPe Web Deploy — One command to build & deploy
REM  Usage: deploy.bat
REM ============================================================

echo.
echo ========================================
echo   DutyPe Web Deploy
echo ========================================
echo.

REM Step 1: Deploy web + hosting + functions
echo [1/2] Deploying to Firebase Hosting...
call firebase deploy --only hosting
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Firebase deploy failed.
    pause
    exit /b 1
)

echo.
echo [2/2] Setting Cloud Run public access...
REM Fix the 403 Forbidden — allow unauthenticated access to SSR function
powershell -NoProfile -Command ^
  "$cred = Get-Content \"$env:APPDATA\firebase\vamsib298_gmail_com_application_default_credentials.json\" -Raw | ConvertFrom-Json; ^
   $body = @{ client_id=$cred.client_id; client_secret=$cred.client_secret; refresh_token=$cred.refresh_token; grant_type='refresh_token' }; ^
   $token = (Invoke-RestMethod -Uri 'https://oauth2.googleapis.com/token' -Method POST -Body $body).access_token; ^
   $headers = @{ Authorization=\"Bearer $token\" }; ^
   $policy = @{ policy=@{ bindings=@(@{ role='roles/run.invoker'; members=@('allUsers') }) } }; ^
   Invoke-RestMethod -Uri 'https://run.googleapis.com/v2/projects/dutypeapp/locations/us-central1/services/ssrdutypeapp:setIamPolicy' -Headers $headers -Method POST -Body ($policy | ConvertTo-Json -Depth 5) -ContentType 'application/json' | Out-Null; ^
   Write-Host 'Cloud Run public access set successfully.'"

echo.
echo ========================================
echo   Deploy complete!
echo   https://dutype.in
echo ========================================
echo.
pause
