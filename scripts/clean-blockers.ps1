#Requires -Version 5.1
$ErrorActionPreference = "SilentlyContinue"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$distUnpacked = Join-Path $Root "dist\win-unpacked"
$appAsar = Join-Path $distUnpacked "resources\app.asar"

$targets = @("Roleplay Client", "roleplay-client", "RoleplayClient")
$processes = @("electron.exe")
$processes += $targets | ForEach-Object { $_ + ".exe" }

$blockers = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object {
    $cmd = $_.CommandLine
    if (-not $cmd) { return $false }
    $processes -contains $_.Name -and ($cmd -like "*$Root*" -or ($targets | Where-Object { $cmd -like "*$_*" }))
}

if ($blockers) {
    Write-Host "Chiusura processi che bloccano la build..."
    foreach ($p in $blockers) {
        try {
            Write-Host ("    Termino PID {0} ({1})" -f $p.ProcessId, $p.Name)
            Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
        } catch {
            Write-Warning ("Impossibile terminare PID {0}: {1}" -f $p.ProcessId, $_)
        }
    }
}

Write-Host "Attendo rilascio file..."
Start-Sleep -Seconds 2

if (Test-Path $appAsar) {
    $maxAttempts = 5
    $attempt = 0
    while ($attempt -lt $maxAttempts) {
        $attempt++
        try {
            Remove-Item -LiteralPath $appAsar -Force -ErrorAction Stop
            Write-Host ("    Rimosso {0}" -f $appAsar)
            break
        } catch {
            Write-Warning ("    Tentativo {0}/{1}: app.asar ancora bloccato" -f $attempt, $maxAttempts)
            Start-Sleep -Seconds 2
        }
    }
}
