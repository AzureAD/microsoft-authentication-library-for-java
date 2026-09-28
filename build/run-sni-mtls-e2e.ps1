[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string] $ToolsArchiveDirectory
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$mavenVersion = "3.9.11"
$mavenArchivePath = Join-Path `
    $ToolsArchiveDirectory `
    "apache-maven-$mavenVersion-bin.zip"
$mavenRoot = Join-Path $env:Agent_TempDirectory "apache-maven-$mavenVersion"
$maven = Join-Path $mavenRoot "bin\mvn.cmd"
$mavenRepository = Join-Path $ToolsArchiveDirectory "maven-repository"

$jdkRoot = Join-Path $env:Agent_TempDirectory "temurin-jdk21"
$javaExecutable = Get-ChildItem `
    -Path $jdkRoot `
    -Filter java.exe `
    -File `
    -Recurse `
    -ErrorAction SilentlyContinue |
    Where-Object { $_.DirectoryName -like "*\bin" } |
    Select-Object -First 1

if ($null -eq $javaExecutable) {
    $jdkArchive = Join-Path $ToolsArchiveDirectory "temurin-jdk21.zip"
    $expectedJdkHash = Get-Content `
        -LiteralPath "$jdkArchive.sha256" `
        -Raw
    $actualJdkHash = (Get-FileHash `
        -LiteralPath $jdkArchive `
        -Algorithm SHA256).Hash
    if ($actualJdkHash -ne $expectedJdkHash) {
        throw "Downloaded JDK 21 archive failed SHA-256 verification."
    }

    New-Item -ItemType Directory -Force -Path $jdkRoot | Out-Null
    Expand-Archive `
        -LiteralPath $jdkArchive `
        -DestinationPath $jdkRoot `
        -Force
    $javaExecutable = Get-ChildItem `
        -Path $jdkRoot `
        -Filter java.exe `
        -File `
        -Recurse |
        Where-Object { $_.DirectoryName -like "*\bin" } |
        Select-Object -First 1
    if ($null -eq $javaExecutable) {
        throw "Downloaded JDK 21 archive did not contain java.exe."
    }
}

$env:JAVA_HOME = $javaExecutable.Directory.Parent.FullName
$env:PATH = "$($javaExecutable.Directory.FullName);$env:PATH"

if (-not (Test-Path -LiteralPath $maven -PathType Leaf)) {
    $expectedMavenHash = Get-Content `
        -LiteralPath "$mavenArchivePath.sha512" `
        -Raw
    $actualMavenHash = (Get-FileHash `
        -LiteralPath $mavenArchivePath `
        -Algorithm SHA512).Hash
    if ($actualMavenHash -ne $expectedMavenHash) {
        throw "Downloaded Maven archive failed SHA-512 verification."
    }
    Expand-Archive `
        -LiteralPath $mavenArchivePath `
        -DestinationPath $env:Agent_TempDirectory `
        -Force
}

$warningDays = 60
$certificate = Get-ChildItem -Path Cert:\LocalMachine\My |
    Where-Object {
        $_.GetNameInfo(
            [System.Security.Cryptography.X509Certificates.X509NameType]::SimpleName,
            $false) -eq "LabAuth.MSIDLab.com" -and
        $_.HasPrivateKey -and
        $_.NotBefore -le [DateTime]::Now -and
        [DateTime]::Now -le $_.NotAfter
    } |
    Sort-Object -Property NotBefore -Descending |
    Select-Object -First 1
if ($null -eq $certificate) {
    throw "A currently valid LabAuth.MSIDLab.com certificate with a private key was not found in LocalMachine\My."
}
$daysRemaining = [Math]::Floor(
    ($certificate.NotAfter.ToUniversalTime() - [DateTime]::UtcNow).TotalDays)
if ($daysRemaining -le $warningDays) {
    Write-Host (
        "##vso[task.logissue type=warning]" +
        "SNI certificate CN=LabAuth.MSIDLab.com expires in " +
        "$daysRemaining day(s) on " +
        "$($certificate.NotAfter.ToUniversalTime().ToString('u')). " +
        "Rotate it within the 60-day window."
    )
}

& $javaExecutable.FullName -version
& $maven -version
& $maven `
    "-o" `
    "-pl" "msal4j-sdk" `
    "-am" `
    "-Drevapi.failBuildOnProblemsFound=false" `
    "-Dskip.unit.tests=true" `
    "-Dskip.integration.tests=false" `
    "-Dadfs.disabled=true" `
    "-Dit.test=SniMtlsPopE2E" `
    "-Dmaven.repo.local=$mavenRepository" `
    "test-compile" `
    "failsafe:integration-test" `
    "failsafe:verify"
if ($LASTEXITCODE -ne 0) {
    throw "SniMtlsPopE2E failed with exit code $LASTEXITCODE."
}

$reportPath = Join-Path `
    $env:BUILD_SOURCESDIRECTORY `
    "msal4j-sdk\target\failsafe-reports\TEST-com.microsoft.aad.msal4j.SniMtlsPopE2E.xml"
if (-not (Test-Path -LiteralPath $reportPath -PathType Leaf)) {
    throw "SNI mTLS E2E report was not produced."
}
[xml] $report = Get-Content -LiteralPath $reportPath -Raw
$suite = $report.testsuite
if ([int] $suite.tests -ne 2 -or
    [int] $suite.skipped -ne 0 -or
    [int] $suite.failures -ne 0 -or
    [int] $suite.errors -ne 0) {
    throw (
        "Expected exactly two passing SNI mTLS E2E tests; " +
        "tests=$($suite.tests), skipped=$($suite.skipped), " +
        "failures=$($suite.failures), errors=$($suite.errors)."
    )
}
