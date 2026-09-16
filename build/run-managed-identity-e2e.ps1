[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("ManagedIdentityImdsE2E", "ManagedIdentityAzureArcE2E")]
    [string] $TestClass,

    [Parameter(Mandatory = $true)]
    [string] $ToolsArchiveDirectory
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$mavenVersion = "3.9.11"
$mavenArchiveName = "apache-maven-$mavenVersion-bin.zip"
$mavenArchivePath = Join-Path $ToolsArchiveDirectory $mavenArchiveName
$mavenRoot =
    Join-Path $env:Agent_TempDirectory "apache-maven-$mavenVersion"
$maven = Join-Path $mavenRoot "bin\mvn.cmd"
$mavenRepository = Join-Path $ToolsArchiveDirectory "maven-repository"

$jdkRoot = Join-Path $env:Agent_TempDirectory "temurin-jdk8"
$javaExecutable = Get-ChildItem `
    -Path $jdkRoot `
    -Filter java.exe `
    -File `
    -Recurse `
    -ErrorAction SilentlyContinue |
    Where-Object { $_.DirectoryName -like "*\bin" } |
    Select-Object -First 1

if ($null -eq $javaExecutable) {
    $jdkArchive = Join-Path $ToolsArchiveDirectory "temurin-jdk8.zip"
    $expectedJdkHash = Get-Content `
        -LiteralPath "$jdkArchive.sha256" `
        -Raw
    $actualJdkHash = (Get-FileHash `
        -LiteralPath $jdkArchive `
        -Algorithm SHA256).Hash
    if ($actualJdkHash -ne $expectedJdkHash) {
        throw "Downloaded JDK 8 archive failed SHA-256 verification."
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
        throw "Downloaded JDK 8 archive did not contain java.exe."
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
    "-Dit.test=$TestClass" `
    "-DmanagedIdentity.e2e.enabled=true" `
    "-Dmaven.repo.local=$mavenRepository" `
    "verify"
if ($LASTEXITCODE -ne 0) {
    throw "$TestClass failed with exit code $LASTEXITCODE."
}
