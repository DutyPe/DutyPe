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

REM Step 1: Deploy web + hosting
echo [1/3] Deploying to Firebase Hosting...
call firebase deploy --only hosting
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Firebase deploy failed.
    pause
    exit /b 1
)

echo.
echo [2/3] Setting Firebase Admin SDK env vars on Cloud Run...
powershell -NoProfile -Command ^
  "$pk = '-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCg9QMTSeEhBErx\n4zHBRRa8tDpmXs7geCNrEJQHeawA9SsDTXILPTQccfgkEoEzbmB9u9cOsVDq/ERD\nM298qhr4ZfD1IL1teXiUYsLznBnZt4RVvIXolcyCv05f58+1IjIWE4RZTz6nLWc3\nQJXeEwyirhX3I6wr9MJliNCYGa8cah0m2j/PNam+0rxWCOpERGhhmRhLklrICGXr\nIl5ZjfZE/kdTwhUiEPygojI0E/TEKPWxfMFsE2AlTMvAZp7R04t47sudpHQ1HhNw\nitUZlbbg46V3q5F6nUeb8+HdgzUMWgjlYIBMVmY95K1Cny8uPgmY+F05yGcNRAyW\nQK00D9ZjAgMBAAECggEAB32fmQQ1L/4tJri2FhKLDlks5pq+8f9lVpQ872QgNyMx\nz0OJbTB5/mDndXZPAP/ACdn2Fj72TxlSa/Y22AxqmOjVn/LYHgIrNGysOf4nzdQW\nmo7dhIeELwmMMjZtmjZvMkvSOv3PIqSgSxf8YYttaRzE9O16E8meSm9llc6MMbxD\ng4kmdw0ayBx8zQOJyTZFXXoBTy+YY2014L3nheuWoiDEPxSvKTAg6CE3Q1uvv62J\nHT2eTUC+YsxkLKKefYMRgNqs6gJR5/KxdjER9VgAnNSMwC8TplHIbVFLl1pAkNfV\nb13B9FbucRP0t3UHoBe2hMVaPV8YkenEa7rqS60Y8QKBgQDhcVRjOPH7y6XJFGIx\nclEf717pIuLXBC3LIdTeXUpl3FBLU70QVjlfeX8ch9WZppTpsq0Y95HN82yt1ghQ\nvphNWpMbmXgE/ZnWASUnpaBSOeAH6m95HvXcmNy0dDnlJILHE89AkbKo5SZTIp9I\ncLYPXTqtzZdf27nPb7+0T5EAhwKBgQC2xhYPmilWjlkF7ocYcUDKOuInrDgHHt9d\nA71HQqzwdCUjzDzmFkSNhPcnazLZEg4wW5TGQzuZnpbYKeyQNFawG6hBVVGKY52j\nQdwuLHMhb1oaYnFS07pTFRNocl1LnfHsxrlCPSrO+NtpHVXCYvABPkUSEZkcmJVK\nSU7x9dY+RQKBgBk0UWnycONkxjkv5TaAAF/gpCzOcKv3VKNUOfdOWMYAlp2FVCri\naPGqpLJs4U2XWSiziDS1YQC0iV76Ad39IQvs52t7gfaU38EUSbIgC0eGHuzjoejZ\ntEUgdfRa1iovJcvaBB7E60OEsv8ybLptl30qQCg0Rws5hpGRfc5L4vs7AoGAbo5f\nZcygWKT+IGJqAVTwFeOMTOcVdOktvvu2EFa6eTAoDwFJjPvHN7tILOXg+gC76HBd\nC5g7gu6028hwOcIO9bOlEO/kxMsA3XHHF373nS/X1sHZPnqn5/2FodYbrNO7NLf3\n86NAM7XAMlL2PwDNoFLkQMu4S93X3/l78Uv87ekCgYEAiIXTILbQMmyRBZQ9GaUm\nyluNr5QjWNlfemLEFmyomV+mKZf6lEq23BO0hgN2404pk0BV8+DnhuA5/qaP1fil\nT2B3RAL8mvtWdz7PEDQLnLg2ctRvDrAS4s42VoRsYUnK/7PFOEHx9iHdhoEvIO/1\nsXaPQXVzTflVBs7JiVNDtnQ=\n-----END PRIVATE KEY-----\n'; ^
   $cred = Get-Content \"$env:APPDATA\firebase\vamsib298_gmail_com_application_default_credentials.json\" -Raw | ConvertFrom-Json; ^
   $bt = @{ client_id=$cred.client_id; client_secret=$cred.client_secret; refresh_token=$cred.refresh_token; grant_type='refresh_token' }; ^
   $tok = (Invoke-RestMethod 'https://oauth2.googleapis.com/token' -Method POST -Body $bt).access_token; ^
   $h = @{ Authorization=\"Bearer $tok\"; 'Content-Type'='application/json' }; ^
   $raw = (Invoke-WebRequest 'https://run.googleapis.com/v2/projects/dutypeapp/locations/us-central1/services/ssrdutypeapp' -Headers $h -UseBasicParsing).Content; ^
   $svc = $raw | ConvertFrom-Json; ^
   $svc.template.revision = $null; ^
   $adminNames = @('FIREBASE_ADMIN_PROJECT_ID','FIREBASE_ADMIN_CLIENT_EMAIL','FIREBASE_ADMIN_PRIVATE_KEY'); ^
   $existing = @($svc.template.containers[0].env | Where-Object { $adminNames -notcontains $_.name }); ^
   $svc.template.containers[0].env = $existing + @([PSCustomObject]@{name='FIREBASE_ADMIN_PROJECT_ID';value='dutypeapp'},[PSCustomObject]@{name='FIREBASE_ADMIN_CLIENT_EMAIL';value='firebase-adminsdk-fbsvc@dutypeapp.iam.gserviceaccount.com'},[PSCustomObject]@{name='FIREBASE_ADMIN_PRIVATE_KEY';value=$pk}); ^
   $body = $svc | ConvertTo-Json -Depth 30 -Compress; ^
   $result = Invoke-RestMethod 'https://run.googleapis.com/v2/projects/dutypeapp/locations/us-central1/services/ssrdutypeapp' -Headers $h -Method PATCH -Body $body -UseBasicParsing; ^
   Write-Host 'Admin SDK env vars set. Waiting for rollout...'; ^
   Start-Sleep -Seconds 30; ^
   $s = Invoke-RestMethod 'https://run.googleapis.com/v2/projects/dutypeapp/locations/us-central1/services/ssrdutypeapp' -Headers $h -UseBasicParsing; ^
   Write-Host \"Ready revision: $($s.latestReadyRevision.Split('/')[-1])\""

echo.
echo [3/3] Setting Cloud Run public access...
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
