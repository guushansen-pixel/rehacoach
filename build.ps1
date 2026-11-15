<#
.SYNOPSIS
    Baut RehaCoach ueber apk-builder und haengt die native Bruecke
    an (Backup als Datei, Teilen-Menue).
.DESCRIPTION
    Aufbau wie gym-coach\build.ps1. apps\RehaCoach unter apk-builder wird bei
    jedem Lauf per -Force neu aus dem Template erzeugt, der Patch laeuft
    deshalb jedes Mal erneut. Throw-Guards: Aendert sich das Template, bricht
    der Build laut ab, statt still eine APK ohne Bruecke zu bauen.

    - android-src\FileBridge.java wird mit dem echten Paketnamen kopiert.
    - MainActivity: Bruecke "RcAndroid" registrieren und onActivityResult
      an FileBridge.onResult() weiterleiten.

    Mit -Release landet die signierte APK zusaetzlich als
    dist\rehacoach-<Version>.apk (aeltere dist-APKs werden entfernt).
.EXAMPLE
    .\build.ps1 -VersionName 1.1 -VersionCode 2 -Release
#>
[CmdletBinding()]
param(
    [string]$VersionName = '1.0',
    [int]$VersionCode = 1,
    [switch]$Release,
    [switch]$Install
)

$ErrorActionPreference = 'Stop'

# javac lehnt eine UTF-8-BOM ab; PS 5.1 schreibt mit -Encoding utf8 immer eine.
$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
function Write-TextNoBom {
    param([string]$Path, [string]$Content)
    [System.IO.File]::WriteAllText($Path, $Content, $Utf8NoBom)
}
function Replace-Once {
    param([string]$Text, [string]$Needle, [string]$Replacement, [string]$What)
    $idx = $Text.IndexOf($Needle)
    if ($idx -lt 0) {
        throw "Patchziel '$What' nicht gefunden - hat sich das apk-builder-Template geaendert?"
    }
    return $Text.Substring(0, $idx) + $Replacement + $Text.Substring($idx + $Needle.Length)
}

$ProjectRoot = $PSScriptRoot
$ApkBuilder  = "D:\claude code projects\apk-builder"
$PackageId   = "com.daniel.rehacoach"
$AppName     = "RehaCoach"
$appDir      = Join-Path $ApkBuilder "apps\$AppName"

& "$ApkBuilder\new-app.ps1" -Name $AppName -PackageId $PackageId `
    -WebRoot (Join-Path $ProjectRoot 'web') `
    -Icon (Join-Path $ProjectRoot 'icon.xml') `
    -IconBackground "#12171E" -Portrait -KeepScreenOn `
    -VersionName $VersionName -VersionCode $VersionCode -Force

# --- strings.xml: Anzeigename mit Bindestrich --------------------------------
$stringsPath = Join-Path $appDir 'app\src\main\res\values\strings.xml'
$strings = [System.IO.File]::ReadAllText($stringsPath)
$strings = Replace-Once $strings "<string name=`"app_name`">$AppName</string>" '<string name="app_name">Reha-Coach</string>' 'app_name'
Write-TextNoBom -Path $stringsPath -Content $strings

$javaDir = Join-Path $appDir ("app\src\main\java\" + $PackageId.Replace('.', '\'))

# --- FileBridge.java -------------------------------------------------------
$bridge = [System.IO.File]::ReadAllText((Join-Path $ProjectRoot 'android-src\FileBridge.java'))
$bridge = Replace-Once $bridge 'package __PACKAGE__;' "package $PackageId;" 'package-Platzhalter'
Write-TextNoBom -Path (Join-Path $javaDir 'FileBridge.java') -Content $bridge

# --- MainActivity.java -----------------------------------------------------
$mainActivityPath = Join-Path $javaDir 'MainActivity.java'
$java = [System.IO.File]::ReadAllText($mainActivityPath)

$java = Replace-Once $java 'webView.setWebViewClient(new LocalWebViewClient());' @'
webView.setWebViewClient(new LocalWebViewClient());

        // Native Bruecke: siehe reha-coach\android-src\FileBridge.java.
        fileBridge = new FileBridge(this, webView);
        webView.addJavascriptInterface(fileBridge, "RcAndroid");
'@.TrimEnd() 'setWebViewClient'

$java = Replace-Once $java '    /** Haelt lokale Seiten im WebView' @'
    // ---- Native Bruecke (eingefuegt von reha-coach\build.ps1) ----
    private FileBridge fileBridge;

    @SuppressWarnings("deprecation")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (fileBridge != null) fileBridge.onResult(requestCode, resultCode, data);
    }

    /** Haelt lokale Seiten im WebView
'@ 'LocalWebViewClient-Kommentar'

Write-TextNoBom -Path $mainActivityPath -Content $java
Write-Host "  [ok] Native Bruecke eingebaut" -ForegroundColor Green

$buildArgs = @{ App = $AppName }
if ($Release) { $buildArgs.Release = $true }
if ($Install) { $buildArgs.Install = $true }
& "$ApkBuilder\build-apk.ps1" @buildArgs

if ($Release) {
    $latest = Get-ChildItem (Join-Path $ApkBuilder 'out') -Filter "$AppName-release-*.apk" |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $latest) { throw "Keine Release-APK in apk-builder\out gefunden" }
    $dist = Join-Path $ProjectRoot 'dist'
    if (-not (Test-Path $dist)) { New-Item -ItemType Directory -Path $dist | Out-Null }
    Get-ChildItem $dist -Filter 'rehacoach-*.apk' | Remove-Item -Force
    $target = Join-Path $dist "rehacoach-$VersionName.apk"
    Copy-Item $latest.FullName $target -Force
    Write-Host "  [ok] $target" -ForegroundColor Green
}
