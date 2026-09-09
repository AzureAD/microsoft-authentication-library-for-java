[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$mavenVersion = "3.9.11"
$archiveName = "apache-maven-$mavenVersion-bin.zip"
$downloadRoot = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries"
$archivePath = Join-Path $env:Agent_TempDirectory $archiveName
$mavenRoot = Join-Path $env:Agent_TempDirectory "apache-maven-$mavenVersion"
$maven = Join-Path $mavenRoot "bin\mvn.cmd"

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    $jdkAssetsUrl = "https://api.adoptium.net/v3/assets/latest/8/hotspot" +
        "?architecture=x64&heap_size=normal&image_type=jdk" +
        "&jvm_impl=hotspot&os=windows&vendor=eclipse"
    $jdkAsset = (Invoke-RestMethod -Uri $jdkAssetsUrl)[0]
    if ($null -eq $jdkAsset.binary.package.link -or
        $null -eq $jdkAsset.binary.package.checksum) {
        throw "Adoptium did not return a downloadable JDK 8 package."
    }

    $jdkArchive = Join-Path $env:Agent_TempDirectory "temurin-jdk8.zip"
    $jdkExtractRoot = Join-Path $env:Agent_TempDirectory "temurin-jdk8"
    Invoke-WebRequest `
        -Uri $jdkAsset.binary.package.link `
        -OutFile $jdkArchive

    $expectedJdkHash = $jdkAsset.binary.package.checksum
    $actualJdkHash = (Get-FileHash `
        -LiteralPath $jdkArchive `
        -Algorithm SHA256).Hash
    if ($actualJdkHash -ne $expectedJdkHash) {
        throw "Downloaded JDK 8 archive failed SHA-256 verification."
    }

    New-Item -ItemType Directory -Force -Path $jdkExtractRoot | Out-Null
    Expand-Archive `
        -LiteralPath $jdkArchive `
        -DestinationPath $jdkExtractRoot `
        -Force

    $javaExecutable = Get-ChildItem `
        -Path $jdkExtractRoot `
        -Filter java.exe `
        -File `
        -Recurse |
        Where-Object { $_.DirectoryName -like "*\bin" } |
        Select-Object -First 1
    if ($null -eq $javaExecutable) {
        throw "Downloaded JDK 8 archive did not contain java.exe."
    }

    $env:JAVA_HOME = $javaExecutable.Directory.Parent.FullName
    $env:PATH = "$($javaExecutable.Directory.FullName);$env:PATH"
    Write-Host "##vso[task.setvariable variable=JAVA_HOME]$env:JAVA_HOME"
    Write-Host "##vso[task.prependpath]$($javaExecutable.Directory.FullName)"
}

if (-not (Test-Path -LiteralPath $maven -PathType Leaf)) {
    $archiveUrl = "$downloadRoot/$archiveName"
    $checksumUrl = "$archiveUrl.sha512"

    Invoke-WebRequest -Uri $archiveUrl -OutFile $archivePath
    $expectedHash = ((Invoke-RestMethod -Uri $checksumUrl) -split "\s+")[0]
    $actualHash = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA512).Hash
    if ($actualHash -ne $expectedHash) {
        throw "Downloaded Maven archive failed SHA-512 verification."
    }

    Expand-Archive `
        -LiteralPath $archivePath `
        -DestinationPath $env:Agent_TempDirectory `
        -Force
}

& $maven `
    "-Pe2e" `
    "-pl" "msal4j-mtls-extensions-e2e" `
    "-am" `
    "-Dskip.unit.tests=true" `
    "-Dskip.integration.tests=true" `
    "-Dmaven.javadoc.skip=true" `
    "package"
if ($LASTEXITCODE -ne 0) {
    throw "Managed Identity E2E Maven build failed with exit code $LASTEXITCODE."
}
