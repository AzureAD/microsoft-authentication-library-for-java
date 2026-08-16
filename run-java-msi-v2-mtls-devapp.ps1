[CmdletBinding()]
param(
    [string]$Maven,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$repoRoot = $PSScriptRoot

function Assert-EnvironmentVariable {
    param([string]$Name)
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($Name))) {
        throw "Required environment variable is missing: $Name"
    }
}

if (-not $IsWindows -and $PSVersionTable.PSEdition -eq "Core") {
    throw "Managed Identity v2 KeyGuard mTLS PoP requires Windows."
}

$tokenOnly = [bool]::Parse(
    $(if ([string]::IsNullOrWhiteSpace($env:MSAL_JAVA_MTLS_TOKEN_ONLY)) {
        "false"
    } else {
        $env:MSAL_JAVA_MTLS_TOKEN_ONLY
    }))
if (-not $tokenOnly) {
    Assert-EnvironmentVariable "MSAL_JAVA_MTLS_AKV_URL"
    Assert-EnvironmentVariable "MSAL_JAVA_MTLS_AKV_SECRET_NAME"
}

$java = Get-Command java -ErrorAction SilentlyContinue
if ($null -eq $java) {
    throw "java.exe was not found on PATH."
}

$tpm = Get-Tpm
if (-not $tpm.TpmPresent -or -not $tpm.TpmReady) {
    throw "A present and ready TPM is required."
}

try {
    if (-not (Confirm-SecureBootUEFI)) {
        throw "Secure Boot is not enabled."
    }
} catch [System.PlatformNotSupportedException] {
    throw "Secure Boot status could not be verified on this platform."
}

if (-not $SkipBuild) {
    if ([string]::IsNullOrWhiteSpace($Maven)) {
        $mavenCommand = Get-Command mvn.cmd, mvn -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if ($null -ne $mavenCommand) {
            $Maven = $mavenCommand.Source
        }
    }
    if ([string]::IsNullOrWhiteSpace($Maven) -or
        -not (Test-Path -LiteralPath $Maven -PathType Leaf)) {
        throw "Maven was not found on PATH. Pass -Maven with the path to mvn.cmd."
    }
    Push-Location $repoRoot
    try {
        & $Maven -q -pl msal4j-mtls-extensions-e2e -am `
            '-Pe2e' '-DskipTests' '-Dmaven.javadoc.skip=true' package
        if ($LASTEXITCODE -ne 0) {
            throw "Maven build failed with exit code $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
}

$jar = Get-ChildItem `
    (Join-Path $repoRoot "msal4j-mtls-extensions-e2e\target") `
    -Filter "*-e2e.jar" |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if ($null -eq $jar) {
    throw "The manual validation JAR was not found. Run without -SkipBuild."
}

Write-Host "Attestation DLL: bundled Microsoft.Azure.Security.KeyGuardAttestation 1.1.5"
Write-Host "Validation JAR: $($jar.FullName)"
& $java.Source -jar $jar.FullName
if ($LASTEXITCODE -ne 0) {
    throw "Managed Identity v2 mTLS PoP validation failed with exit code $LASTEXITCODE."
}
