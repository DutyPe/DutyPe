# Get Release SHA-1 Fingerprint
# This script generates the SHA-1 fingerprint for your release keystore
# Add this SHA-1 to Google Cloud Console > OAuth Consent Screen > Credentials

# Check if keystore.properties exists
if (Test-Path ".\keystore.properties") {
    Write-Host "Reading keystore configuration from keystore.properties..." -ForegroundColor Green
    
    $content = Get-Content ".\keystore.properties" | ConvertFrom-StringData
    $storePath = $content.storeFile
    $storePassword = $content.storePassword
    $keyAlias = $content.keyAlias
    $keyPassword = $content.keyPassword
    
    if (Test-Path $storePath) {
        Write-Host "Keystore found at: $storePath" -ForegroundColor Green
        Write-Host "`n========================================" -ForegroundColor Yellow
        Write-Host "Release SHA-1 Fingerprint:" -ForegroundColor Yellow
        Write-Host "========================================" -ForegroundColor Yellow
        
        # Generate SHA-1 fingerprint
        $output = & keytool -list -v -keystore $storePath -alias $keyAlias -storepass $storePassword -keypass $keyPassword 2>&1
        
        # Extract SHA-1
        $sha1Line = $output | Select-String -Pattern "SHA1:"
        if ($sha1Line) {
            Write-Host $sha1Line.Line -ForegroundColor Cyan
            Write-Host "`n📋 Copy the SHA-1 above and add to:" -ForegroundColor Green
            Write-Host "   Google Cloud Console > OAuth Consent Screen > Credentials" -ForegroundColor Green
            Write-Host "   Android App > Add Fingerprint (SHA1 only, without SHA1 prefix)" -ForegroundColor Green
        } else {
            Write-Host "Could not extract SHA-1" -ForegroundColor Red
            Write-Host "Full output:" -ForegroundColor Yellow
            $output
        }
    } else {
        Write-Host "Keystore not found at: $storePath" -ForegroundColor Red
    }
} else {
    Write-Host "keystore.properties not found!" -ForegroundColor Red
    Write-Host "Please ensure you're in the project root directory." -ForegroundColor Yellow
}
