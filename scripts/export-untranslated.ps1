# Exports English values for keys that are missing or untranslated in a locale.
param([string]$Locale = 'values-hi', [string]$Out = 'todo-hi.txt')
$ErrorActionPreference = 'Stop'
$root = Join-Path $PSScriptRoot '..\app\src\main\res'
$utf8 = New-Object System.Text.UTF8Encoding($false)
function Read-Map([string]$d) {
    $doc = New-Object System.Xml.XmlDocument
    $doc.LoadXml([System.IO.File]::ReadAllText((Join-Path $root "$d\strings.xml"), $utf8))
    $m = [ordered]@{}
    $skip = @{}
    foreach ($s in $doc.resources.string) {
        if (-not $s.name) { continue }
        if ($s.translatable -eq 'false') { $skip[$s.name] = $true; continue }
        $m[$s.name] = [string]$s.InnerText
    }
    return $m
}
$base = Read-Map 'values'
$loc = Read-Map $Locale
$lines = New-Object System.Collections.Generic.List[string]
$chars = 0
foreach ($k in $base.Keys) {
    $en = $base[$k]
    $needs = (-not $loc.Contains($k)) -or ($loc[$k] -eq $en -and $en -match '[A-Za-z]{4}')
    if ($needs) {
        $lines.Add("$k`t$en")
        $chars += $en.Length
    }
}
[System.IO.File]::WriteAllLines((Join-Path $PSScriptRoot "..\$Out"), $lines, (New-Object System.Text.UTF8Encoding($false)))
Write-Output "locale=$Locale needs=$($lines.Count) chars=$chars -> $Out"
