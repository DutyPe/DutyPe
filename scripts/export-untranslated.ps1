# Exports English values for keys that are missing or untranslated in a locale.
param([string]$Locale = 'values-hi', [string]$Out = 'todo-hi.txt')
$ErrorActionPreference = 'Stop'
$root = Join-Path $PSScriptRoot '..\app\src\main\res'
function Read-Map([string]$d) {
    $xml = [xml](Get-Content (Join-Path $root "$d\strings.xml") -Raw)
    $m = [ordered]@{}
    foreach ($s in $xml.resources.string) { if ($s.name) { $m[$s.name] = [string]$s.InnerText } }
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
