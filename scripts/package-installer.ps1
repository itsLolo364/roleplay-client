#Requires -Version 5.1
<#
.SYNOPSIS
  Package Roleplay Client as a Windows NSIS installer.

.DESCRIPTION
  1) Build Fabric mod (Gradle)
  2) Copy JAR into assets/mods/roleplayclient.jar
  3) electron-builder --win (NSIS wizard)
  Output: dist\RoleplayClient-Setup-*.exe

.EXAMPLE
  .\scripts\package-installer.ps1
  .\scripts\package-installer.ps1 -SkipMod
  .\Package-Installer.bat
#>
param(
    [switch]$SkipMod,
    [switch]$SkipNpmInstall
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

function Write-Step([string]$msg) {
    Write-Host ""
    Write-Host "==> $msg" -ForegroundColor Cyan
}

function Fail([string]$msg) {
    Write-Host ""
    Write-Host "ERRORE: $msg" -ForegroundColor Red
    exit 1
}

Write-Host "Roleplay Client - package installer Windows" -ForegroundColor Yellow
Write-Host "Root: $Root"

# ---- Tools ----
if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    Fail "npm non trovato. Installa Node.js 18+."
}
if (-not $SkipMod) {
    $hasGradleW = Test-Path (Join-Path $Root "loloclient-mod\gradlew.bat")
    $hasGradle = [bool](Get-Command gradle -ErrorAction SilentlyContinue)
    if (-not $hasGradleW -and -not $hasGradle) {
        Fail "Ne gradlew.bat ne gradle nel PATH. Serve Java 21 + Gradle per buildare la mod."
    }
}

# ---- npm deps ----
if (-not $SkipNpmInstall) {
    if (-not (Test-Path (Join-Path $Root "node_modules\electron-builder"))) {
        Write-Step "npm install"
        npm install
        if ($LASTEXITCODE -ne 0) { Fail "npm install fallito" }
    }
}

# ---- Mod ----
if (-not $SkipMod) {
    Write-Step "Build mod Fabric"
    Push-Location (Join-Path $Root "loloclient-mod")
    try {
        if (Test-Path ".\gradlew.bat") {
            & .\gradlew.bat build --quiet
        } else {
            & gradle build --quiet
        }
        if ($LASTEXITCODE -ne 0) { Fail "Build mod fallita (exit $LASTEXITCODE)" }
    } finally {
        Pop-Location
    }

    Write-Step "Deploy JAR in assets/mods"
    $libs = Join-Path $Root "loloclient-mod\build\libs"
    $jar = Get-ChildItem $libs -Filter "roleplayclient*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch "sources" } |
        Sort-Object Name -Descending |
        Select-Object -First 1
    if (-not $jar) { Fail "Nessun roleplayclient*.jar in $libs" }

    $modsDir = Join-Path $Root "assets\mods"
    if (-not (Test-Path $modsDir)) { New-Item -ItemType Directory -Path $modsDir | Out-Null }
    $dest = Join-Path $modsDir "roleplayclient.jar"
    Copy-Item -Force $jar.FullName $dest
    Write-Host ("    {0} -> assets\mods\roleplayclient.jar" -f $jar.Name)
} else {
    Write-Step "Skip mod (-SkipMod). Uso assets\mods\roleplayclient.jar esistente."
    if (-not (Test-Path (Join-Path $Root "assets\mods\roleplayclient.jar"))) {
        Fail "Manca assets\mods\roleplayclient.jar. Rilancia senza -SkipMod."
    }
}

if (-not (Test-Path (Join-Path $Root "assets\icon.ico"))) {
    Fail "Manca assets\icon.ico (richiesto da electron-builder)."
}

# ---- Electron NSIS ----
Write-Step "electron-builder - installer NSIS Windows"
# Evita winCodeSign (symlink Darwin falliscono senza privilegi admin / Developer Mode)
$env:CSC_IDENTITY_AUTO_DISCOVERY = "false"
$env:ELECTRON_BUILDER_DISABLE_CODE_SIGNING = "true"
npm run build:win
if ($LASTEXITCODE -ne 0) { Fail "electron-builder fallito (exit $LASTEXITCODE)" }

$dist = Join-Path $Root "dist"
$setup = Get-ChildItem $dist -Filter "RoleplayClient-Setup-*.exe" -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if (-not $setup) {
    $setup = Get-ChildItem $dist -Filter "*.exe" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match "Setup|setup" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

Write-Host ""
Write-Host "OK - installer pronto." -ForegroundColor Green
if ($setup) {
    Write-Host ("File: {0}" -f $setup.FullName) -ForegroundColor Green
    Write-Host ("Size: {0:N1} MB" -f ($setup.Length / 1MB))
    Write-Host ""
    Write-Host "Puoi reinstallarlo per aggiornare oppure inviare questo .exe."
} else {
    Write-Host "Controlla la cartella dist per l exeexe generato."
    Get-ChildItem $dist -ErrorAction SilentlyContinue | ForEach-Object { Write-Host ("  - {0}" -f $_.Name) }
}
