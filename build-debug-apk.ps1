# Debug APK: set JAVA_HOME (JDK 17+) and Android SDK; sdk.dir in local.properties.
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
if (Test-Path $sdk) {
    $env:ANDROID_HOME = $sdk
}
Set-Location $PSScriptRoot
# Makro/mikro Sync (VERSION.json -> gradle/plist/Kotlin)
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "scripts\Sync-VersionFiles.ps1")
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& .\gradlew.bat :composeApp:assembleDebug --no-daemon
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host ""
Write-Host "APK:" (Resolve-Path "composeApp\build\outputs\apk\debug\composeApp-debug.apk")
