<#
.SYNOPSIS
  VanGO tablet/telefon makro/mikro bump. micro=v0.90-000N++ ; macro=v0.91-0001
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("micro", "macro")]
    [string]$Bump,
    [string]$Notes = "",
    [string]$Module = "platform"
)
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$wsTools = Join-Path (Split-Path -Parent (Split-Path -Parent $Root)) "tools\hexnet-version\HexnetVersion.psm1"
Import-Module $wsTools -Force
Invoke-HexnetVersionBump -Root $Root -Bump $Bump -Notes $Notes -Module $Module | Out-Null
& (Join-Path $PSScriptRoot "Sync-VersionFiles.ps1")
