# Script to find keystore files and check their SHA1 fingerprints
# This helps locate the correct keystore file

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Keystore Finder and Fingerprint Checker" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$expectedSHA1 = "BC:C7:8D:E1:A6:D7:BD:F3:3F:90:B5:43:D6:32:F1:63:D2:F9:02:D3"
Write-Host "Expected SHA1: $expectedSHA1" -ForegroundColor Yellow
Write-Host ""

# Search for keystore files
Write-Host "Searching for keystore files (.keystore, .jks)..." -ForegroundColor Green
Write-Host "This may take a few minutes..." -ForegroundColor Yellow
Write-Host ""

$keystoreFiles = @()

# Search in common locations
$searchPaths = @(
    "$env:USERPROFILE",
    "$env:USERPROFILE\Android",
    "$env:USERPROFILE\.android",
    "$env:USERPROFILE\StudioProjects",
    "$env:USERPROFILE\Documents",
    "$env:USERPROFILE\Desktop"
)

foreach ($path in $searchPaths) {
    if (Test-Path $path) {
        Write-Host "Searching in: $path" -ForegroundColor Gray
        try {
            $files = Get-ChildItem -Path $path -Recurse -Include *.keystore,*.jks -ErrorAction SilentlyContinue -Depth 3
            $keystoreFiles += $files
        } catch {
            # Ignore permission errors
        }
    }
}

if ($keystoreFiles.Count -eq 0) {
    Write-Host "No keystore files found in common locations." -ForegroundColor Red
    Write-Host ""
    Write-Host "Please manually check:" -ForegroundColor Yellow
    Write-Host "1. C:\Users\banot\KeyStore\" -ForegroundColor White
    Write-Host "2. Project directories" -ForegroundColor White
    Write-Host "3. Backup folders" -ForegroundColor White
    Write-Host "4. Cloud storage (Google Drive, OneDrive)" -ForegroundColor White
    exit
}

Write-Host ""
Write-Host "Found $($keystoreFiles.Count) keystore file(s):" -ForegroundColor Green
Write-Host ""

$foundMatch = $false

foreach ($file in $keystoreFiles) {
    Write-Host "Checking: $($file.FullName)" -ForegroundColor Cyan
    Write-Host "  File size: $([math]::Round($file.Length/1KB, 2)) KB" -ForegroundColor Gray
    
    # Try to get certificate info
    # Note: This requires knowing the alias and password, so we'll just list the file
    Write-Host "  To check fingerprint, run:" -ForegroundColor Yellow
    Write-Host "    keytool -list -v -keystore `"$($file.FullName)`" -alias YOUR_ALIAS" -ForegroundColor White
    Write-Host ""
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Manual Check Instructions" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "For each keystore file found, run this command:" -ForegroundColor Yellow
Write-Host ""
Write-Host '  keytool -list -v -keystore "PATH_TO_KEYSTORE" -alias "KEY_ALIAS" -storepass "KEYSTORE_PASSWORD"' -ForegroundColor White
Write-Host ""
Write-Host "Look for the SHA1 fingerprint in the output." -ForegroundColor Yellow
Write-Host "It should match: $expectedSHA1" -ForegroundColor Green
Write-Host ""
Write-Host "If you find a match, update keystore.properties with:" -ForegroundColor Yellow
Write-Host "  - storeFile: Path to the matching keystore" -ForegroundColor White
Write-Host "  - keyAlias: The alias you used in the command" -ForegroundColor White
Write-Host "  - storePassword: The keystore password" -ForegroundColor White
Write-Host "  - keyPassword: The key password (usually same as storePassword)" -ForegroundColor White
Write-Host ""

