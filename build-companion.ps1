param([string]$Python = 'python')
$ErrorActionPreference = 'Stop'
& "$PSScriptRoot\companion\build-windows.ps1" -Python $Python
