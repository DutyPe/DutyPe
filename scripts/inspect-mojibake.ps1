# Shows every mojibake run in one file with its proposed repair, as code points.
param([Parameter(Mandatory = $true)][string]$Path)
$ErrorActionPreference = 'Stop'
$strict = New-Object System.Text.UTF8Encoding($false, $true)
$cp1252 = [System.Text.Encoding]::GetEncoding(1252)
$map = @{}
for ($b = 0x80; $b -le 0xFF; $b++) {
    $ch = $cp1252.GetString([byte[]]@($b))
    if ($ch.Length -eq 1) { $map[$ch[0]] = [byte]$b }
}
function Show-Codes([string]$value) {
    ($value.ToCharArray() | ForEach-Object {
        $code = [int][char]$_
        if ($code -ge 32 -and $code -lt 127) { [string]$_ } else { "<U+{0:X4}>" -f $code }
    }) -join ''
}
$text = [System.IO.File]::ReadAllText((Resolve-Path $Path), $strict)
$idx = 0
$found = 0
while ($idx -lt $text.Length) {
    if (-not $map.ContainsKey($text[$idx])) { $idx++; continue }
    $end = $idx
    while ($end -lt $text.Length -and $map.ContainsKey($text[$end])) { $end++ }
    $run = $text.Substring($idx, $end - $idx)
    $bytes = [byte[]]($run.ToCharArray() | ForEach-Object { $map[$_] })
    $decoded = $null
    try { $decoded = $strict.GetString($bytes) } catch { $decoded = $null }
    $ok = ($decoded -and $decoded.Length -lt $run.Length -and $decoded -notmatch "`uFFFD")
    $found++
    Write-Output ("[{0}] run={1}" -f $found, (Show-Codes $run))
    if ($ok) { Write-Output ("     -> {0}   ({1})" -f (Show-Codes $decoded), $decoded) }
    else { Write-Output "     -> LEFT AS IS" }
    $idx = $end
}
Write-Output "runs=$found"
