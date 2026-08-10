# Prints en/hi/te values for the given string names.
param([Parameter(Mandatory = $true)][string[]]$Names)
$ErrorActionPreference = 'Stop'
# -File passes everything as one string, so re-split on commas.
$Names = $Names -split ',' | Where-Object { $_ }
$root = Join-Path $PSScriptRoot '..\app\src\main\res'
$sets = @{}
foreach ($d in @('values', 'values-hi', 'values-te')) {
    $xml = [xml](Get-Content (Join-Path $root "$d\strings.xml") -Raw)
    $m = @{}
    foreach ($s in $xml.resources.string) { if ($s.name) { $m[$s.name] = [string]$s.InnerText } }
    $sets[$d] = $m
}
foreach ($n in $Names) {
    Write-Output "### $n"
    foreach ($d in @('values', 'values-hi', 'values-te')) {
        $v = if ($sets[$d].ContainsKey($n)) { $sets[$d][$n] } else { '<<MISSING>>' }
        Write-Output ("  {0,-10} {1}" -f $d, $v)
    }
}
