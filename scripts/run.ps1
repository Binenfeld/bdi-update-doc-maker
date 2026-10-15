<#
.SYNOPSIS
    Runs BDIUpdateDocMaker: reads the newest .csv in the input folder, writes the BDI report (.docx and .pdf)
    next to it.
.PARAMETER InputFolder
    Folder holding the exported BDI sheet CSVs. Default: C:\bdiupdatedocmaker
.PARAMETER Debug
    Also print every company read, and the full stack trace of any error.
.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\run.ps1
.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\run.ps1 -InputFolder D:\reports
#>
param(
    [string]$InputFolder = 'C:\bdiupdatedocmaker',
    [switch]$Debug
)
$ErrorActionPreference = 'Stop'

# A relative folder means relative to where the script was started, not to the project folder used below.
$InputFolder = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($InputFolder)

# Run from the project folder, like IntelliJ does.
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot

$jar = Join-Path $projectRoot 'build\libs\BDIUpdateDocMaker.jar'
if (-not (Test-Path $jar)) {
    Write-Host "ERROR: $jar not found. Build it first: double-click scripts\build.cmd" -ForegroundColor Red
    exit 1
}

# Java: JAVA_HOME, else the newest JDK in ~\.jdks (IntelliJ's download folder), else java on the PATH.
if ($env:JAVA_HOME) {
    $java = Join-Path $env:JAVA_HOME 'bin\java.exe'
    if (-not (Test-Path $java)) {
        Write-Host "ERROR: JAVA_HOME is set to $env:JAVA_HOME, but there is no bin\java.exe there. Fix or remove JAVA_HOME." -ForegroundColor Red
        exit 1
    }
} else {
    $jdk = Get-ChildItem "$HOME\.jdks" -Directory -ErrorAction SilentlyContinue |
        Where-Object { Test-Path (Join-Path $_.FullName 'bin\java.exe') } |
        Sort-Object Name -Descending |
        Select-Object -First 1
    if ($jdk) {
        $java = Join-Path $jdk.FullName 'bin\java.exe'
    } elseif (Get-Command java -ErrorAction SilentlyContinue) {
        $java = 'java'
    } else {
        Write-Host "ERROR: Java not found. Install JDK 21 or newer, or set JAVA_HOME." -ForegroundColor Red
        exit 1
    }
}

# Show the program's Hebrew output correctly in this console.
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$programArgs = @($InputFolder)
if ($Debug) { $programArgs += '--debug' }

# The program prints its own error messages; don't let PowerShell turn its error output into extra noise.
$ErrorActionPreference = 'Continue'
& $java '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -jar $jar @programArgs
exit $LASTEXITCODE
