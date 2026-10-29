[CmdletBinding()]
param(
    [string]$LibraryPath,
    [string]$DestinationDirectory = (Join-Path $PSScriptRoot "..\msal4j-mtls-extensions\target\key-attestation-test")
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

# This pin belongs to the test harness, not to the production Java artifact.
$version = "1.1.5"
$expectedHash = "90dfcce20e1a74519b49796eeee17e6e59a257c3acf754f454a49380d28a568b"
$destination = [IO.Path]::GetFullPath($DestinationDirectory)
New-Item -ItemType Directory -Path $destination -Force | Out-Null
$library = Join-Path $destination "AttestationClientLib.dll"

if ([string]::IsNullOrWhiteSpace($LibraryPath)) {
    $package = "microsoft.azure.security.keyguardattestation"
    $archive = Join-Path $destination "$package.$version.nupkg"
    Invoke-WebRequest `
        -Uri "https://api.nuget.org/v3-flatcontainer/$package/$version/$package.$version.nupkg" `
        -OutFile $archive
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [IO.Compression.ZipFile]::OpenRead($archive)
    try {
        $entry = $zip.GetEntry("runtimes/win-x64/native/AttestationClientLib.dll")
        if ($null -eq $entry) {
            throw "The test package does not contain the Windows x64 attestation DLL."
        }
        $LibraryPath = Join-Path $destination "AttestationClientLib.download.dll"
        [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $LibraryPath, $true)
    } finally {
        $zip.Dispose()
    }
}

$source = (Resolve-Path -LiteralPath $LibraryPath).ProviderPath
if ((Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash -ne $expectedHash) {
    throw "The test attestation DLL does not match Microsoft.Azure.Security.KeyGuardAttestation $version."
}
$signature = Get-AuthenticodeSignature -LiteralPath $source
if ($signature.Status -ne "Valid") {
    throw "The test attestation DLL failed Authenticode validation: $($signature.Status)."
}
if ($source -ne $library) {
    Copy-Item -LiteralPath $source -Destination $library -Force
}
Write-Output $library
