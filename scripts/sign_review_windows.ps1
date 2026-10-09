param(
  [Parameter(Mandatory=$true)][string]$EvidenceDirectory,
  [Parameter(Mandatory=$true)][string]$OutputDirectory,
  [Parameter(Mandatory=$true)][string]$BuildToolsDirectory,
  [Parameter(Mandatory=$true)][string]$RunId
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$evidence = Get-Content -LiteralPath (Join-Path $EvidenceDirectory 'build/compliance/package-evidence.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$run = gh run view $RunId --repo illetyus/open-source-client-for-nextdns --json headSha,status,conclusion,event,headBranch | ConvertFrom-Json
if ($LASTEXITCODE -ne 0 -or $run.status -ne 'completed' -or $run.conclusion -ne 'success' -or $run.headSha -ne $evidence.commit -or $run.event -ne 'push' -or $run.headBranch -ne 'main') { throw 'Signing requires successful matching main CI' }
foreach ($package in $evidence.packages) {
  if ((Get-FileHash -LiteralPath (Join-Path $EvidenceDirectory $package.file) -Algorithm SHA256).Hash.ToLowerInvariant() -ne $package.sha256) { throw 'Input artifact hash mismatch' }
}
$keyDirectory = Join-Path $env:LOCALAPPDATA 'OpenSourceClientForNextDNS\signing'
$backupDirectory = Join-Path ([Environment]::GetFolderPath('MyDocuments')) 'CodexPrivate\OpenSourceClientForNextDNS-signing-backup'
foreach ($directory in @($keyDirectory, $backupDirectory)) {
  New-Item -ItemType Directory -Path $directory -Force | Out-Null
  $acl = New-Object System.Security.AccessControl.DirectorySecurity
  $acl.SetAccessRuleProtection($true, $false)
  $identity = [System.Security.Principal.WindowsIdentity]::GetCurrent().User
  $system = New-Object System.Security.Principal.SecurityIdentifier('S-1-5-18')
  foreach ($sid in @($identity,$system)) {
    $rule = New-Object System.Security.AccessControl.FileSystemAccessRule($sid,'FullControl','ContainerInherit,ObjectInherit','None','Allow')
    $acl.AddAccessRule($rule)
  }
  Set-Acl -LiteralPath $directory -AclObject $acl
}
$keystore = Join-Path $keyDirectory 'upload.p12'
$passwordFile = Join-Path $keyDirectory 'password.dpapi'
if ((Test-Path -LiteralPath $keystore) -ne (Test-Path -LiteralPath $passwordFile)) { throw 'Incomplete existing signing material; will not replace it' }
try {
  if (-not (Test-Path -LiteralPath $keystore)) {
    $random = New-Object byte[] 32
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    $generator.GetBytes($random)
    $generator.Dispose()
    $env:NEXTDNS_LOCAL_SIGNING_PASSWORD = ([BitConverter]::ToString($random)).Replace('-','')
    $secure = ConvertTo-SecureString -String $env:NEXTDNS_LOCAL_SIGNING_PASSWORD -AsPlainText -Force
    ConvertFrom-SecureString $secure | Set-Content -LiteralPath $passwordFile -Encoding ASCII
    keytool -genkeypair -keystore $keystore -storetype PKCS12 -alias upload -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Open Source Client for NextDNS' -storepass:env NEXTDNS_LOCAL_SIGNING_PASSWORD -keypass:env NEXTDNS_LOCAL_SIGNING_PASSWORD -noprompt
    if ($LASTEXITCODE -ne 0) { throw 'Keystore generation failed' }
  } else {
    $secure = (Get-Content -LiteralPath $passwordFile -Raw).Trim() | ConvertTo-SecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { $env:NEXTDNS_LOCAL_SIGNING_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
  }
  foreach ($file in @('upload.p12','password.dpapi')) {
    $source = Join-Path $keyDirectory $file
    $backup = Join-Path $backupDirectory $file
    if (Test-Path -LiteralPath $backup) {
      if ((Get-FileHash -LiteralPath $source).Hash -ne (Get-FileHash -LiteralPath $backup).Hash) { throw 'Existing backup differs; will not overwrite it' }
    } else { Copy-Item -LiteralPath $source -Destination $backup }
  }
  New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
  $apk = Join-Path $EvidenceDirectory (($evidence.packages | Where-Object kind -EQ 'release').file)
  $aab = Join-Path $EvidenceDirectory (($evidence.packages | Where-Object kind -EQ 'releaseBundle').file)
  $aligned = Join-Path $OutputDirectory 'aligned-input.apk'
  $signedApk = Join-Path $OutputDirectory 'OpenSourceClientForNextDNS-1.0-review.apk'
  $signedAab = Join-Path $OutputDirectory 'OpenSourceClientForNextDNS-1.0-review.aab'
  if ((Test-Path -LiteralPath $signedApk) -or (Test-Path -LiteralPath $signedAab)) { throw 'Output already contains signed packages; select a new directory' }
  & (Join-Path $BuildToolsDirectory 'zipalign.exe') -P 16 -f 4 $apk $aligned
  if ($LASTEXITCODE -ne 0) { throw 'APK alignment failed' }
  & (Join-Path $BuildToolsDirectory 'apksigner.bat') sign --ks $keystore --ks-key-alias upload --ks-pass env:NEXTDNS_LOCAL_SIGNING_PASSWORD --key-pass env:NEXTDNS_LOCAL_SIGNING_PASSWORD --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true --v4-signing-enabled false --out $signedApk $aligned
  if ($LASTEXITCODE -ne 0) { throw 'APK signing failed' }
  jarsigner -keystore $keystore -storepass:env NEXTDNS_LOCAL_SIGNING_PASSWORD -keypass:env NEXTDNS_LOCAL_SIGNING_PASSWORD -sigalg SHA256withRSA -digestalg SHA-256 -signedjar $signedAab $aab upload
  if ($LASTEXITCODE -ne 0) { throw 'Bundle signing failed' }
  keytool -exportcert -rfc -keystore $keystore -alias upload -storepass:env NEXTDNS_LOCAL_SIGNING_PASSWORD -file (Join-Path $OutputDirectory 'upload-certificate.pem')
  if ($LASTEXITCODE -ne 0) { throw 'Public certificate export failed' }
  Remove-Item -LiteralPath $aligned
  python -X utf8 (Join-Path $PSScriptRoot 'verify_signed_review.py') --evidence $EvidenceDirectory --output $OutputDirectory --tools $BuildToolsDirectory --run-id $RunId
  if ($LASTEXITCODE -ne 0) { throw 'Signed package verification failed' }
  Write-Output "Verified signing material stored outside the repository: $keyDirectory"
  Write-Output "Private local backup: $backupDirectory (DPAPI password is bound to this Windows user)"
} finally {
  Remove-Item Env:NEXTDNS_LOCAL_SIGNING_PASSWORD -ErrorAction SilentlyContinue
  if ($null -ne $secure) { $secure.Dispose() }
}
