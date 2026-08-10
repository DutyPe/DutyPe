# Prints en/hi/te values for the given string names.
param([Parameter(Mandatory = $true)][string[]]$Names)
$ErrorActionPreference = 'Stop'
# -File passes everything as one string, so re-split on commas.
$Names = $Names -split ',' | Where-Object { $_ }
$root = Join-Path $PSScriptRoot '..\app\src\main\res'
$utf8 = New-Object System.Text.UTF8Encoding($false)
$sets = @{}
foreach ($d in @('values', 'values-hi', 'values-te')) {
    $doc = New-Object System.Xml.XmlDocument
    $doc.LoadXml([System.IO.File]::ReadAllText((Join-Path $root "$d\strings.xml"), $utf8))
    $m = @{}
    foreach ($s in $doc.resources.string) { if ($s.name) { $m[$s.name] = [string]$s.InnerText } }
    $sets[$d] = $m
}
foreach ($n in $Names) {
    Write-Output "### $n"
    foreach ($d in @('values', 'values-hi', 'values-te')) {
        $v = if ($sets[$d].ContainsKey($n)) { $sets[$d][$n] } else { '<<MISSING>>' }
        Write-Output ("  {0,-10} {1}" -f $d, $v)
    }
}
