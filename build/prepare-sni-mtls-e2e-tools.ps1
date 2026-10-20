[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string] $OutputDirectory
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
[Net.ServicePointManager]::SecurityProtocol =
    [Net.ServicePointManager]::SecurityProtocol -bor
    [Net.SecurityProtocolType]::Tls12

function Invoke-WithRetry {
    param(
        [Parameter(Mandatory = $true)]
        [scriptblock] $Operation,

        [Parameter(Mandatory = $true)]
        [string] $Description,

        [int] $MaximumAttempts = 3
    )

    for ($attempt = 1; $attempt -le $MaximumAttempts; $attempt++) {
        try {
            return & $Operation
        } catch {
            if ($attempt -eq $MaximumAttempts) {
                throw
            }
            Write-Warning (
                "$Description failed on attempt $attempt. Retrying..."
            )
            Start-Sleep -Seconds (5 * $attempt)
        }
    }
}

$jdkArchiveUrl =
    "https://github.com/adoptium/temurin21-binaries/releases/download/" +
    "jdk-21.0.12.1%2B1/" +
    "OpenJDK21U-jdk_x64_windows_hotspot_21.0.12.1_1.zip"
$expectedJdkHash =
    "f9d6e191ab098c0d416e7d588a24420a8621cd2f4720dab2459b8b7b2d2d8b4e"

New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$jdkArchive = Join-Path $OutputDirectory "temurin-jdk21.zip"
Invoke-WithRetry `
    -Description "JDK 21 download" `
    -Operation {
        Invoke-WebRequest `
            -Uri $jdkArchiveUrl `
            -OutFile $jdkArchive
    }
$actualJdkHash = (Get-FileHash `
    -LiteralPath $jdkArchive `
    -Algorithm SHA256).Hash
if ($actualJdkHash -ne $expectedJdkHash) {
    throw "Downloaded JDK 21 archive failed SHA-256 verification."
}
Set-Content `
    -LiteralPath "$jdkArchive.sha256" `
    -Value $expectedJdkHash `
    -NoNewline
