<#
.SYNOPSIS
  Derler, emülatör yoksa AVD'yi başlatır, APK yükler ve uygulamayı açar.
  Kod güncelledikten sonra doğrulama için: .\run-on-emulator.ps1

.PARAMETER AvdName
  Kullanılacak sanal cihaz adı (varsayılan: Vango_API34).

.PARAMETER NoLaunchEmulator
  Sadece çalışan emülatöre yükle; emülatör başlatma denemesi yapma.
#>
[CmdletBinding()]
param(
    [string] $AvdName = "Vango_API34",
    [switch] $NoLaunchEmulator
)

$ErrorActionPreference = "Stop"

if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $candidates = @(
        (Join-Path $env:USERPROFILE ".jdks\temurin-17.0.18\jdk-17.0.18+8"),
        (Join-Path $env:USERPROFILE ".jdks\jdk-17.0.18+8")
    )
    foreach ($jdk in $candidates) {
        if (Test-Path "$jdk\bin\java.exe") {
            $env:JAVA_HOME = $jdk
            $env:Path = "$jdk\bin;$env:Path"
            break
        }
    }
}

$sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$adb = Join-Path $sdk "platform-tools\adb.exe"
$emulatorExe = Join-Path $sdk "emulator\emulator.exe"

if (-not (Test-Path $adb)) {
    Write-Error "adb not found at $adb"
}

function Get-EmulatorSerial {
    $lines = & $adb devices 2>&1 | Where-Object { $_ -match "`tdevice$" }
    $emu = ($lines | Where-Object { $_ -match "^emulator-" } | Select-Object -First 1)
    if ($emu) { return ($emu -split "`t")[0].Trim() }
    return $null
}

function Wait-EmulatorBoot {
    param([string]$Serial)
    Write-Host "Waiting for emulator to finish booting..."
    $deadline = (Get-Date).AddMinutes(4)
    while ((Get-Date) -lt $deadline) {
        $boot = & $adb -s $Serial shell getprop sys.boot_completed 2>$null
        if ($boot -match "1") {
            Write-Host "Emulator ready."
            return
        }
        Start-Sleep -Seconds 2
    }
    Write-Warning "Boot wait timed out; continuing anyway."
}

$serial = Get-EmulatorSerial

if (-not $serial -and -not $NoLaunchEmulator) {
    if (-not (Test-Path $emulatorExe)) {
        Write-Error "Emulator not found at $emulatorExe. Install Android SDK Emulator or start an AVD from Android Studio."
    }
    Write-Host "Starting AVD '$AvdName'..."
    Start-Process -FilePath $emulatorExe -ArgumentList "-avd", $AvdName, "-no-snapshot-load" -WindowStyle Normal
    Write-Host "Waiting for adb to see the emulator..."
    $deadline = (Get-Date).AddMinutes(3)
    while ((Get-Date) -lt $deadline) {
        $serial = Get-EmulatorSerial
        if ($serial) { break }
        Start-Sleep -Seconds 2
    }
    if (-not $serial) {
        Write-Error "Emulator did not appear in 'adb devices' in time. Start Device Manager manually and retry."
    }
    Wait-EmulatorBoot -Serial $serial
}
elseif (-not $serial) {
    Write-Error "No running emulator. Remove -NoLaunchEmulator or start an AVD first."
}

$env:ANDROID_HOME = $sdk
$env:ANDROID_SERIAL = $serial
Write-Host "Using device: $serial"

Set-Location $PSScriptRoot
Write-Host "Installing debug build..."
# installDebug sonrası composeApp/build.gradle.kts içinde MainActivity otomatik başlatılır
& .\gradlew.bat :composeApp:installDebug --no-daemon
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host ""
Write-Host "OK: install finished; Gradle should have launched the app on the emulator. Script: .\run-on-emulator.ps1"
