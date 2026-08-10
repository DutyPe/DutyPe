# Where a generated auto_* key duplicates an existing key with identical English,
# reuses the canonical translation so the same phrase reads the same on every screen.
# Context-specific pairs (neither key generated) are left alone deliberately.
param([string]$Locale = 'values-hi', [switch]$Apply)
$ErrorActionPreference = 'Stop'
$root = Join-Path $PSScriptRoot '..\app\src\main\res'
$utf8 = New-Object System.Text.UTF8Encoding($false)

function Load([string]$dir) {
    $doc = New-Object System.Xml.XmlDocument
    $doc.PreserveWhitespace = $true
    $doc.LoadXml([System.IO.File]::ReadAllText((Join-Path $root "$dir\strings.xml"), $utf8))
    return $doc
}

$baseDoc = Load 'values'
$locDoc = Load $Locale

$baseEn = @{}
foreach ($s in $baseDoc.resources.string) {
    if ($s.name -and $s.translatable -ne 'false') { $baseEn[$s.name] = [string]$s.InnerText }
}
$locNodes = @{}
foreach ($s in $locDoc.resources.string) { if ($s.name) { $locNodes[$s.name] = $s } }

# Group canonical (non-generated) keys by their English text.
# Keys are sorted so the chosen canonical is stable across runs and locales.
$canonicalByEn = @{}
foreach ($k in ($baseEn.Keys | Sort-Object)) {
    if ($k -like 'auto_*') { continue }
    $en = $baseEn[$k].Trim()
    if ($en.Length -lt 3) { continue }
    if (-not $canonicalByEn.ContainsKey($en)) { $canonicalByEn[$en] = $k }
}

$changed = 0
foreach ($k in ($baseEn.Keys | Sort-Object)) {
    if ($k -notlike 'auto_*') { continue }
    $en = $baseEn[$k].Trim()
    if (-not $canonicalByEn.ContainsKey($en)) { continue }
    $canonical = $canonicalByEn[$en]
    if (-not $locNodes.ContainsKey($k) -or -not $locNodes.ContainsKey($canonical)) { continue }

    $current = [string]$locNodes[$k].InnerText
    $target = [string]$locNodes[$canonical].InnerText
    if ($current -eq $target) { continue }

    Write-Output ("{0}`n   was: {1}`n   now: {2}   (from {3})" -f $k, $current, $target, $canonical)
    $locNodes[$k].InnerText = $target
    $changed++
}

Write-Output ""
Write-Output ("{0}  duplicates unified: {1}" -f $Locale, $changed)

if ($Apply -and $changed -gt 0) {
    $settings = New-Object System.Xml.XmlWriterSettings
    $settings.Encoding = $utf8
    $settings.Indent = $false
    $writer = [System.Xml.XmlWriter]::Create((Join-Path $root "$Locale\strings.xml"), $settings)
    $locDoc.Save($writer)
    $writer.Close()
    Write-Output "written"
}
