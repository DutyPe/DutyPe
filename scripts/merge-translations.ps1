# Merges key<TAB>value TSV files into a locale's strings.xml.
# TSV files must be UTF-8. Existing keys are overwritten, new keys appended.
param(
    [Parameter(Mandatory = $true)][string]$Locale,
    [Parameter(Mandatory = $true)][string]$Tsv
)
$ErrorActionPreference = 'Stop'
$utf8 = New-Object System.Text.UTF8Encoding($false)
$repo = Resolve-Path (Join-Path $PSScriptRoot '..')
$target = Join-Path $repo "app\src\main\res\$Locale\strings.xml"

$pairs = [ordered]@{}
foreach ($file in ($Tsv -split ',' | Where-Object { $_ })) {
    $path = Join-Path $repo $file.Trim()
    if (-not (Test-Path $path)) { throw "missing tsv: $path" }
    foreach ($line in [System.IO.File]::ReadAllLines($path, $utf8)) {
        if (-not $line -or $line.StartsWith('#')) { continue }
        $i = $line.IndexOf("`t")
        if ($i -lt 1) { continue }
        $pairs[$line.Substring(0, $i)] = $line.Substring($i + 1)
    }
}

$xml = New-Object System.Xml.XmlDocument
$xml.PreserveWhitespace = $true
$xml.Load($target)
$root = $xml.DocumentElement

$existing = @{}
foreach ($n in $root.SelectNodes('string')) { $existing[$n.GetAttribute('name')] = $n }

$updated = 0
$added = 0
foreach ($k in $pairs.Keys) {
    $v = $pairs[$k]
    if ($existing.ContainsKey($k)) {
        $existing[$k].InnerText = $v
        $updated++
    }
    else {
        $node = $xml.CreateElement('string')
        $node.SetAttribute('name', $k)
        $node.InnerText = $v
        $root.AppendChild($xml.CreateTextNode("    ")) | Out-Null
        $root.AppendChild($node) | Out-Null
        $root.AppendChild($xml.CreateTextNode("`n")) | Out-Null
        $added++
    }
}

$settings = New-Object System.Xml.XmlWriterSettings
$settings.Encoding = $utf8
$settings.Indent = $false
$settings.OmitXmlDeclaration = $false
$writer = [System.Xml.XmlWriter]::Create($target, $settings)
$xml.Save($writer)
$writer.Close()

Write-Output "$Locale  updated=$updated added=$added total=$($pairs.Count)"
