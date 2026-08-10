# Reports mojibake (double-encoded UTF-8) sequences in resource and source files.
$ErrorActionPreference = 'Stop'
$repo = Resolve-Path (Join-Path $PSScriptRoot '..')
$utf8 = New-Object System.Text.UTF8Encoding($false)

# Each entry is the mangled form followed by what it should have been.
$patterns = @(
    @{ bad = [char]0x00E2 + [char]0x201A + [char]0x00B9; good = 'rupee sign' },
    @{ bad = [char]0x00E2 + [char]0x20AC + [char]0x2122; good = 'apostrophe' },
    @{ bad = [char]0x00E2 + [char]0x20AC + [char]0x0153; good = 'left quote' },
    @{ bad = [char]0x00E2 + [char]0x20AC + [char]0x009D; good = 'right quote' },
    @{ bad = [char]0x00E2 + [char]0x20AC + [char]0x201C; good = 'en dash' },
    @{ bad = [char]0x00C3 + [char]0x00A9; good = 'e-acute' },
    @{ bad = [char]0x00F0 + [char]0x0178; good = 'emoji' }
)

$targets = @()
$targets += Get-ChildItem (Join-Path $repo 'app\src\main\res') -Recurse -Filter '*.xml'
$targets += Get-ChildItem (Join-Path $repo 'app\src\main\java') -Recurse -Filter '*.kt'

$total = 0
foreach ($f in $targets) {
    $text = [System.IO.File]::ReadAllText($f.FullName, $utf8)
    $hits = @()
    foreach ($p in $patterns) {
        $c = ([regex]::Matches($text, [regex]::Escape($p.bad))).Count
        if ($c -gt 0) { $hits += "$($p.good)=$c" }
    }
    if ($hits.Count -gt 0) {
        $rel = $f.FullName.Substring($repo.Path.Length + 1)
        Write-Output ("{0,-70} {1}" -f $rel, ($hits -join ' '))
        foreach ($p in $patterns) { $total += ([regex]::Matches($text, [regex]::Escape($p.bad))).Count }
    }
}
Write-Output ""
Write-Output "total mojibake sequences = $total"
