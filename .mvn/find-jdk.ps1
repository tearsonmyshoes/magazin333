# Picks a JDK 17+ for the Maven wrapper on Windows.
# It keeps the configured JAVA_HOME when that JDK is new enough, otherwise it
# looks in the usual install locations. Prints only the selected path.

$ErrorActionPreference = 'Continue'

function Get-JavaMajor([string]$homePath) {
    if ([string]::IsNullOrWhiteSpace($homePath)) {
        return 0
    }

    $java = Join-Path $homePath 'bin\java.exe'
    if (-not (Test-Path $java)) {
        return 0
    }

    $javac = Join-Path $homePath 'bin\javac.exe'
    if (-not (Test-Path $javac)) {
        return 0
    }

    $firstLine = & $java -version 2>&1 | Select-Object -First 1
    if ($firstLine -match 'version\s+"?1\.(?<legacy>\d+)') {
        return [int]$Matches['legacy']
    }

    if ($firstLine -match 'version\s+"?(?<major>\d+)') {
        return [int]$Matches['major']
    }

    return 0
}

function Test-UsableJdk([string]$homePath) {
    return (Get-JavaMajor $homePath) -ge 17
}

if (Test-UsableJdk $env:JAVA_HOME) {
    exit 0
}

# When JAVA_HOME is not set Maven falls back to java.exe from PATH.
if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCommand) {
        $javaHome = Split-Path (Split-Path $javaCommand.Source -Parent) -Parent
        if (Test-UsableJdk $javaHome) {
            exit 0
        }
    }
}

$roots = @(
    "$env:USERPROFILE\.jdks",
    "$env:ProgramFiles\Java",
    "$env:ProgramFiles\Eclipse Adoptium",
    "$env:ProgramFiles\Microsoft",
    "$env:ProgramFiles\Zulu",
    "$env:ProgramFiles\Amazon Corretto",
    "$env:ProgramFiles\BellSoft",
    "$env:ProgramFiles\Semeru",
    "$env:ProgramFiles\RedHat",
    "$env:LOCALAPPDATA\Programs\Eclipse Adoptium",
    "$env:USERPROFILE\.sdkman\candidates\java"
)

$candidates = @()

foreach ($root in $roots) {
    if (-not (Test-Path $root)) {
        continue
    }

    Get-ChildItem -Path $root -Directory -ErrorAction SilentlyContinue | ForEach-Object {
        $major = Get-JavaMajor $_.FullName
        if ($major -ge 17) {
            $candidates += [pscustomobject]@{ Path = $_.FullName; Major = $major }
        }
    }
}

# IntelliJ bundles a usable JDK as well.
$jetbrains = Join-Path $env:ProgramFiles 'JetBrains'
if (Test-Path $jetbrains) {
    Get-ChildItem -Path $jetbrains -Directory -ErrorAction SilentlyContinue | ForEach-Object {
        $jbr = Join-Path $_.FullName 'jbr'
        $major = Get-JavaMajor $jbr
        if ($major -ge 17) {
            $candidates += [pscustomobject]@{ Path = $jbr; Major = $major }
        }
    }
}

$selected = $candidates |
    Sort-Object -Property @{ Expression = 'Major'; Descending = $true }, @{ Expression = 'Path'; Descending = $false } |
    Select-Object -First 1

if ($selected) {
    Write-Output $selected.Path
} else {
    [Console]::Error.WriteLine("No JDK 17+ found. Install one or set JAVA_HOME to a JDK 17+ directory.")
}
