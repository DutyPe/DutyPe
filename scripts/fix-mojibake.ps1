# Repairs double-encoded UTF-8 (mojibake) by reversing the CP1252 misdecode.
# Runs in report mode by default; pass -Apply to write changes.
param([switch]$Apply, [int]$Show = 12)
$ErrorActionPreference = 'Stop'
$repo = Resolve-Path (Join-Path $PSScriptRoot '..')
$utf8Strict = New-Object System.Text.UTF8Encoding($false, $true)
$utf8Out = New-Object System.Text.UTF8Encoding($false)
$cp1252 = [System.Text.Encoding]::GetEncoding(1252)

# Characters that a CP1252 misdecode of a high byte can produce.
$charToByte = @{}
for ($b = 0x80; $b -le 0xFF; $b++) {
    $c = $cp1252.GetString([byte[]]@($b))
    if ($c.Length -eq 1) { $charToByte[$c[0]] = [byte]$b }
}

function Repair-Text([string]$text, [ref]$count) {
    $sb = New-Object System.Text.StringBuilder
    $i = 0
    while ($i -lt $text.Length) {
        if (-not $charToByte.ContainsKey($text[$i])) {
            [void]$sb.Append($text[$i]); $i++; continue
        }
        $j = $i
        while ($j -lt $text.Length -and $charToByte.ContainsKey($text[$j])) { $j++ }
        $run = $text.Substring($i, $j - $i)
        $bytes = [byte[]]($run.ToCharArray() | ForEach-Object { $charToByte[$_] })
        $decoded = $null
        try { $decoded = $utf8Strict.GetString($bytes) } catch { $decoded = $null }
        # A genuine mojibake run always decodes to strictly fewer characters.
        if ($decoded -and $decoded.Length -lt $run.Length -and $decoded -notmatch "`uFFFD") {
            [void]$sb.Append($decoded)
            $count.Value += ($run.Length - $decoded.Length)
        }
        else {
            [void]$sb.Append($run)
        }
        $i = $j
    }
    return $sb.ToString()
}

$targets = @()
$targets += Get-ChildItem (Join-Path $repo 'app\src\main\res') -Recurse -Filter '*.xml'
$targets += Get-ChildItem (Join-Path $repo 'app\src\main\java') -Recurse -Filter '*.kt'

$files = 0
$fixes = 0
$samples = @()
foreach ($f in $targets) {
    $text = [System.IO.File]::ReadAllText($f.FullName, $utf8Strict)
    $n = 0
    $fixed = Repair-Text $text ([ref]$n)
    if ($n -gt 0 -and $fixed -ne $text) {
        $files++; $fixes += $n
        $rel = $f.FullName.Substring($repo.Path.Length + 1)
        if ($samples.Count -lt $Show) {
            $old = ([regex]::Matches($text, '.{0,34}[\u0080-\u00FF]{2,}.{0,14}') | Select-Object -First 1).Value
            if ($old) { $samples += ("{0}`n    BEFORE {1}`n    AFTER  {2}" -f $rel, $old, (Repair-Text $old ([ref]([int]0)))) }
        }
        if ($Apply) { [System.IO.File]::WriteAllText($f.FullName, $fixed, $utf8Out) }
    }
}

$samples | ForEach-Object { Write-Output $_ }
Write-Output ""
Write-Output ("mode={0} files={1} chars_recovered={2}" -f $(if ($Apply) { 'APPLY' } else { 'REPORT' }), $files, $fixes)
