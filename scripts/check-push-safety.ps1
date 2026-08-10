# Checks whether pushing HEAD to a remote branch would rewrite history.
param([Parameter(Mandatory = $true)][string]$Branch)
$ErrorActionPreference = 'Continue'
git merge-base --is-ancestor $Branch HEAD | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Output "FAST-FORWARD: $Branch is an ancestor of HEAD. No history would be rewritten."
}
else {
    Write-Output "NOT A FAST-FORWARD: pushing would require --force. Stop and review."
}
$from = (git rev-parse --short $Branch)
$to = (git rev-parse --short HEAD)
$gained = (git rev-list --count "$Branch..HEAD")
$lost = (git rev-list --count "HEAD..$Branch")
Write-Output "  moves   : $from -> $to"
Write-Output "  gains   : $gained commit(s)"
Write-Output "  loses   : $lost commit(s)"
