# Sync VERSION.json -> Android gradle, iOS Info.plist, HexnetAppVersion.kt
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$wsTools = Join-Path (Split-Path -Parent (Split-Path -Parent $Root)) "tools\hexnet-version\HexnetVersion.psm1"
Import-Module $wsTools -Force
Sync-HexnetAppFiles -Root $Root
