# Audits string resource parity across en / hi / te.
$ErrorActionPreference = 'Stop'
$root = Join-Path $PSScriptRoot '..\app\src\main\res'
# PowerShell 5.1 reads BOM-less UTF-8 as CP1252, so decode explicitly.
$utf8 = New-Object System.Text.UTF8Encoding($false)

function Load-Xml([string]$dir) {
    $path = Join-Path $root "$dir\strings.xml"
    $doc = New-Object System.Xml.XmlDocument
    $doc.LoadXml([System.IO.File]::ReadAllText($path, $utf8))
    return $doc
}

function Read-Strings([string]$dir) {
    $xml = Load-Xml $dir
    $map = @{}
    foreach ($s in $xml.resources.string) {
        if ($s.name) { $map[$s.name] = [string]$s.InnerText }
    }
    return $map
}

$base = Read-Strings 'values'
$hi = Read-Strings 'values-hi'
$te = Read-Strings 'values-te'

# Keys marked translatable="false" are intentionally English-only.
$baseXml = Load-Xml 'values'
$noTranslate = @($baseXml.resources.string | Where-Object { $_.translatable -eq 'false' } | ForEach-Object { $_.name })

$expected = @($base.Keys | Where-Object { $noTranslate -notcontains $_ })

$missingHi = @($expected | Where-Object { -not $hi.ContainsKey($_) })
$missingTe = @($expected | Where-Object { -not $te.ContainsKey($_) })
$staleHi = @($hi.Keys | Where-Object { -not $base.ContainsKey($_) })
$staleTe = @($te.Keys | Where-Object { -not $base.ContainsKey($_) })

# A translation that is byte-identical to English and contains real words is untranslated.
$sameHi = @($hi.Keys | Where-Object { $expected -contains $_ -and $base[$_] -eq $hi[$_] -and $hi[$_] -match '[A-Za-z]{4}' })
$sameTe = @($te.Keys | Where-Object { $expected -contains $_ -and $base[$_] -eq $te[$_] -and $te[$_] -match '[A-Za-z]{4}' })

# Format specifiers must match exactly or the app crashes at runtime.
# Compared as a sorted multiset, since positional args may legitimately be reordered.
function Get-Specs([string]$v) {
    return ((([regex]::Matches($v, '%(\d+\$)?[sdf]') | ForEach-Object { $_.Value }) | Sort-Object) -join ',')
}
$fmtHi = @($hi.Keys | Where-Object { $base.ContainsKey($_) -and (Get-Specs $base[$_]) -ne (Get-Specs $hi[$_]) })
$fmtTe = @($te.Keys | Where-Object { $base.ContainsKey($_) -and (Get-Specs $base[$_]) -ne (Get-Specs $te[$_]) })

Write-Output "base total          = $($base.Count)"
Write-Output "base translatable   = $($expected.Count)"
Write-Output "hi total            = $($hi.Count)"
Write-Output "te total            = $($te.Count)"
Write-Output ""
Write-Output "missing in hi       = $($missingHi.Count)"
Write-Output "missing in te       = $($missingTe.Count)"
Write-Output "stale in hi         = $($staleHi.Count)"
Write-Output "stale in te         = $($staleTe.Count)"
Write-Output "identical-to-en hi  = $($sameHi.Count)"
Write-Output "identical-to-en te  = $($sameTe.Count)"
Write-Output "format mismatch hi  = $($fmtHi.Count)"
Write-Output "format mismatch te  = $($fmtTe.Count)"

foreach ($pair in @(
    @{ n = 'MISSING-HI'; v = $missingHi },
    @{ n = 'MISSING-TE'; v = $missingTe },
    @{ n = 'FORMAT-HI'; v = $fmtHi },
    @{ n = 'FORMAT-TE'; v = $fmtTe },
    @{ n = 'SAME-HI'; v = $sameHi },
    @{ n = 'SAME-TE'; v = $sameTe }
)) {
    if ($pair.v.Count -gt 0) {
        Write-Output ""
        Write-Output "-- $($pair.n) ($($pair.v.Count)) --"
        $pair.v | Sort-Object | Select-Object -First 60 | ForEach-Object { Write-Output "   $_" }
    }
}

$fatal = $missingHi.Count + $missingTe.Count + $fmtHi.Count + $fmtTe.Count
Write-Output ""
if ($fatal -gt 0) { Write-Output "RESULT: FAIL ($fatal blocking issues)" } else { Write-Output "RESULT: PASS" }
