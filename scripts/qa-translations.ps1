# Flags translation defects that a machine can detect, so a native reviewer only has to
# read the strings that are actually suspicious rather than all 1600.
param([string]$Locale = 'values-hi', [string]$Out = '')
$ErrorActionPreference = 'Stop'
$root = Join-Path $PSScriptRoot '..\app\src\main\res'
$utf8 = New-Object System.Text.UTF8Encoding($false)

function Read-Map([string]$dir) {
    $doc = New-Object System.Xml.XmlDocument
    $doc.LoadXml([System.IO.File]::ReadAllText((Join-Path $root "$dir\strings.xml"), $utf8))
    $m = [ordered]@{}
    foreach ($s in $doc.resources.string) {
        if (-not $s.name) { continue }
        if ($s.translatable -eq 'false') { continue }
        $m[$s.name] = [string]$s.InnerText
    }
    return $m
}

$base = Read-Map 'values'
$loc = Read-Map $Locale
$script = if ($Locale -eq 'values-hi') { '\p{IsDevanagari}' } else { '\p{IsTelugu}' }

$findings = New-Object System.Collections.Generic.List[object]
function Add-Finding($key, $severity, $issue, $en, $tr) {
    $findings.Add([pscustomobject]@{
            key = $key; severity = $severity; issue = $issue; english = $en; translation = $tr
        })
}

# 1. Same English string translated inconsistently. Real risk: the same button reads
#    differently on two screens, which makes the app feel unfinished.
$byEnglish = @{}
foreach ($k in $base.Keys) {
    if (-not $loc.Contains($k)) { continue }
    $en = $base[$k].Trim()
    if ($en.Length -lt 3) { continue }
    if (-not $byEnglish.ContainsKey($en)) { $byEnglish[$en] = @{} }
    $byEnglish[$en][$loc[$k]] = $k
}
foreach ($en in $byEnglish.Keys) {
    $variants = $byEnglish[$en]
    if ($variants.Count -gt 1) {
        foreach ($tr in $variants.Keys) {
            Add-Finding $variants[$tr] 'INCONSISTENT' "same English has $($variants.Count) different translations" $en $tr
        }
    }
}

# 2. No target-script characters at all, but the English has real words.
foreach ($k in $loc.Keys) {
    $tr = $loc[$k]
    $en = if ($base.Contains($k)) { $base[$k] } else { '' }
    if ($en -match '[A-Za-z]{4}' -and $tr -notmatch $script) {
        Add-Finding $k 'UNTRANSLATED' 'no target-script characters' $en $tr
    }
}

# 3. Suspicious length. A translation far shorter than the English usually means
#    dropped meaning; far longer usually means it will clip in the UI.
foreach ($k in $loc.Keys) {
    if (-not $base.Contains($k)) { continue }
    $en = $base[$k]; $tr = $loc[$k]
    if ($en.Length -lt 12) { continue }
    $ratio = [math]::Round($tr.Length / [double]$en.Length, 2)
    if ($ratio -lt 0.45) { Add-Finding $k 'TOO-SHORT' "ratio $ratio - meaning may be dropped" $en $tr }
    elseif ($ratio -gt 2.2) { Add-Finding $k 'TOO-LONG' "ratio $ratio - may clip in UI" $en $tr }
}

# 4. Escaping and markup that must survive translation verbatim.
foreach ($k in $loc.Keys) {
    if (-not $base.Contains($k)) { continue }
    $en = $base[$k]; $tr = $loc[$k]
    if (($en -split '\\n').Count -ne ($tr -split '\\n').Count) {
        Add-Finding $k 'NEWLINE' 'literal \n count differs' $en $tr
    }
    if ($en.Contains("\'") -and $tr.Contains("'") -and -not $tr.Contains("\'")) {
        Add-Finding $k 'UNESCAPED-QUOTE' "apostrophe not escaped, will fail to compile or render wrong" $en $tr
    }
    # Deliberately not comparing \uXXXX escapes: aapt2 compiles a literal bullet or rupee
    # sign to the same value as its escape, so a difference there is not a defect.
}

# 5. Money and safety strings get read first by a reviewer regardless of other signals.
$critical = '^(refer_|withdraw|amount|pay_|sub_|unlock|payment|benefit_|job_safety_|apply_tip_|report_non_payment)'
foreach ($k in $loc.Keys) {
    if ($k -match $critical -and $base.Contains($k)) {
        Add-Finding $k 'REVIEW-PRIORITY' 'money or safety wording' $base[$k] $loc[$k]
    }
}

$order = @{ 'UNESCAPED-QUOTE' = 0; 'NEWLINE' = 1; 'UNTRANSLATED' = 2; 'INCONSISTENT' = 3; 'TOO-SHORT' = 4; 'TOO-LONG' = 5; 'REVIEW-PRIORITY' = 6 }
$sorted = $findings | Sort-Object @{ Expression = { $order[$_.severity] } }, key

Write-Output "locale=$Locale  translatable=$($loc.Count)"
foreach ($sev in $order.Keys | Sort-Object { $order[$_] }) {
    $n = @($findings | Where-Object severity -eq $sev).Count
    if ($n -gt 0) { Write-Output ("  {0,-20} {1}" -f $sev, $n) }
}

if ($Out) {
    $sorted | Export-Csv -Path (Join-Path $PSScriptRoot "..\$Out") -NoTypeInformation -Encoding UTF8
    Write-Output "-> $Out"
}
