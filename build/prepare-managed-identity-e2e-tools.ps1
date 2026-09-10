[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string] $OutputDirectory
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
[Net.ServicePointManager]::SecurityProtocol = `
    [Net.ServicePointManager]::SecurityProtocol -bor `
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

New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null

$jdkAssetsUrl = "https://api.adoptium.net/v3/assets/latest/8/hotspot" +
    "?architecture=x64&heap_size=normal&image_type=jdk" +
    "&jvm_impl=hotspot&os=windows&vendor=eclipse"
$jdkAsset = (Invoke-WithRetry `
    -Description "Adoptium metadata request" `
    -Operation { Invoke-RestMethod -Uri $jdkAssetsUrl })[0]
if ($null -eq $jdkAsset.binary.package.link -or
    $null -eq $jdkAsset.binary.package.checksum) {
    throw "Adoptium did not return a downloadable JDK 8 package."
}

$jdkArchive = Join-Path $OutputDirectory "temurin-jdk8.zip"
Invoke-WithRetry `
    -Description "JDK 8 download" `
    -Operation {
        Invoke-WebRequest `
            -Uri $jdkAsset.binary.package.link `
            -OutFile $jdkArchive
    }
Set-Content `
    -LiteralPath "$jdkArchive.sha256" `
    -Value $jdkAsset.binary.package.checksum `
    -NoNewline

$jdkExtractRoot = Join-Path $env:Agent_TempDirectory "mi-e2e-jdk"
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

$mavenVersion = "3.9.11"
$mavenArchiveName = "apache-maven-$mavenVersion-bin.zip"
$mavenArchiveUrl =
    "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/" +
    $mavenArchiveName
$mavenArchive = Join-Path $OutputDirectory $mavenArchiveName
Invoke-WithRetry `
    -Description "Maven download" `
    -Operation {
        Invoke-WebRequest `
            -Uri $mavenArchiveUrl `
            -OutFile $mavenArchive
    }
$mavenHash = (
    (Invoke-WithRetry `
        -Description "Maven checksum request" `
        -Operation {
            Invoke-RestMethod -Uri "$mavenArchiveUrl.sha512"
        }) -split "\s+"
)[0]
Set-Content `
    -LiteralPath "$mavenArchive.sha512" `
    -Value $mavenHash `
    -NoNewline

$mavenExtractRoot = Join-Path $env:Agent_TempDirectory "mi-e2e-maven"
Expand-Archive `
    -LiteralPath $mavenArchive `
    -DestinationPath $mavenExtractRoot `
    -Force
$maven = Join-Path `
    $mavenExtractRoot `
    "apache-maven-$mavenVersion\bin\mvn.cmd"
$mavenRepository = Join-Path $OutputDirectory "maven-repository"

& $maven `
    "-f" (Join-Path $env:BUILD_SOURCESDIRECTORY "pom.xml") `
    "-pl" "msal4j-sdk" `
    "-am" `
    "-Drevapi.failBuildOnProblemsFound=false" `
    "-Dskip.unit.tests=true" `
    "-Dskip.integration.tests=false" `
    "-Dadfs.disabled=true" `
    "-Dit.test=ManagedIdentityImdsE2E" `
    "-DmanagedIdentity.e2e.enabled=false" `
    "-Dmaven.repo.local=$mavenRepository" `
    "verify"
if ($LASTEXITCODE -ne 0) {
    throw "Failed to prepare the offline Maven repository."
}
