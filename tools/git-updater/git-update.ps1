param(
    [string]$RepositoryPath = $PSScriptRoot,
    [string]$CommitMessage
)

$originalLocation = Get-Location
try {
    if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
        Write-Error "Git is not installed or not in PATH. Install Git and try again."
        exit 1
    }

    $resolvedRepoPath = Resolve-Path -Path $RepositoryPath -ErrorAction Stop
    Set-Location -Path $resolvedRepoPath

    if (-not (Test-Path -Path ".git")) {
        Write-Error "The target path '$resolvedRepoPath' is not a Git repository."
        exit 1
    }

    $statusOutput = git status --short
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to retrieve git status."
        exit $LASTEXITCODE
    }

    if ($statusOutput) {
        Write-Host "Pending changes:" -ForegroundColor Yellow
        $statusOutput | ForEach-Object { Write-Host "  $_" }
    }

    $hasChanges = $statusOutput -and $statusOutput.Trim().Length -gt 0
    if (-not $hasChanges) {
        Write-Host "No pending changes. Nothing to commit."
        exit 0
    }

    if (-not $CommitMessage) {
        $defaultMessage = "Auto update: $(Get-Date -Format 'yyyy-MM-dd HH:mm')"
        $inputMessage = Read-Host "Commit message (press Enter for '$defaultMessage')"
        $CommitMessage = if ([string]::IsNullOrWhiteSpace($inputMessage)) { $defaultMessage } else { $inputMessage }
    }

    Write-Host "Staging changes..." -ForegroundColor Cyan
    git add -A
    if ($LASTEXITCODE -ne 0) {
        Write-Error "git add failed."
        exit $LASTEXITCODE
    }

    Write-Host "Committing..." -ForegroundColor Cyan
    git commit -m $CommitMessage
    if ($LASTEXITCODE -ne 0) {
        Write-Error "git commit failed. Resolve the issue and re-run the script."
        exit $LASTEXITCODE
    }

    Write-Host "Pushing to upstream..." -ForegroundColor Cyan
    git push
    if ($LASTEXITCODE -ne 0) {
        Write-Error "git push failed."
        exit $LASTEXITCODE
    }

    Write-Host "Success! Repository updated." -ForegroundColor Green
}
finally {
    Set-Location -Path $originalLocation
}
