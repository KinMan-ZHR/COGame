# ==============================================================================
# COGame Windows Platform Standalone Packaging Script
# Uses JDK 17 jlink minimal JRE + jpackage native executable bundling
# ==============================================================================
[CmdletBinding()]
param(
    [string]$Version = ""
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ProjectRoot = (Resolve-Path "$ScriptDir\..").Path
Set-Location $ProjectRoot

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "[BUILD] Packaging COGame Windows Standalone Bundle" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

# 1. Detect & Configure Java 17+
if (-not (Get-Command java -ErrorAction SilentlyContinue) -or -not (Get-Command jlink -ErrorAction SilentlyContinue)) {
    $JavaCandidates = @(
        $env:JAVA_HOME,
        "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot",
        "C:\Program Files\Eclipse Adoptium\jdk-17*",
        "C:\Program Files\Java\jdk-17*"
    )
    foreach ($candidate in $JavaCandidates) {
        if ($candidate -and (Test-Path $candidate)) {
            $resolved = (Resolve-Path $candidate | Select-Object -First 1).Path
            if (Test-Path "$resolved\bin\jlink.exe") {
                $env:JAVA_HOME = $resolved
                $env:PATH = "$resolved\bin;" + $env:PATH
                Write-Host "[JAVA] Activated JDK: $resolved" -ForegroundColor Green
                break
            }
        }
    }
}

if (-not (Get-Command jlink -ErrorAction SilentlyContinue) -or -not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    Write-Error "[ERROR] Missing jlink or jpackage. Ensure JDK 17+ is installed with full toolchain."
    exit 1
}

# 2. Detect & Configure Maven
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    $MavenCandidates = @(
        "D:\software\apache-maven-3.9.6\bin",
        "D:\software\apache-maven*\bin",
        "C:\Program Files\apache-maven*\bin"
    )
    foreach ($candidate in $MavenCandidates) {
        if (Test-Path $candidate) {
            $resolved = (Resolve-Path $candidate | Select-Object -First 1).Path
            $env:PATH = "$resolved;" + $env:PATH
            Write-Host "[MAVEN] Activated Maven: $resolved" -ForegroundColor Green
            break
        }
    }
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Error "[ERROR] Missing mvn command. Ensure Maven is installed and configured in PATH."
    exit 1
}

# 3. Resolve Version
if (-not $Version) {
    $pomContent = Get-Content "$ProjectRoot\pom.xml" -Raw
    if ($pomContent -match '<version>([^<]+)</version>') {
        $Version = $Matches[1]
    } else {
        $Version = "2.6.0"
    }
}
Write-Host "[VERSION] Target Version: v$Version" -ForegroundColor Yellow

# 4. Maven package
Write-Host "`n[1/4] Compiling and packaging shaded jars (mvn clean package -DskipTests)..." -ForegroundColor Cyan
& mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Error "[ERROR] Maven packaging failed."
    exit $LASTEXITCODE
}

$ClientTargetDir = "$ProjectRoot\cogame-client\target"
$ShadedJar = Get-ChildItem -Path $ClientTargetDir -Filter "cogame-client-$Version.jar" | Select-Object -First 1
if (-not $ShadedJar) {
    $ShadedJar = Get-ChildItem -Path $ClientTargetDir -Filter "cogame-client-*.jar" | Where-Object { $_.Name -notlike "original-*" } | Select-Object -First 1
}
if (-not $ShadedJar) {
    Write-Error "[ERROR] Client jar not found in $ClientTargetDir"
    exit 1
}
Write-Host "[OK] Located shaded fat JAR: $($ShadedJar.FullName)" -ForegroundColor Green

# 5. Check Windows Icon
$IcoPath = "$ProjectRoot\packaging\windows\cogame.ico"
if (-not (Test-Path $IcoPath)) {
    $IcnsPath = "$ProjectRoot\packaging\macos\template\COGame.app\Contents\Resources\AppIcon.icns"
    if (Test-Path $IcnsPath) {
        Write-Host "[ICON] Converting macOS icns to Windows ico..." -ForegroundColor Cyan
        New-Item -ItemType Directory -Force -Path "$ProjectRoot\packaging\windows" | Out-Null
        $pyCode = "from PIL import Image; img=Image.open(r'$IcnsPath'); img.save(r'$IcoPath', format='ICO', sizes=[(16,16),(32,32),(48,48),(64,64),(128,128),(256,256)])"
        & python -c $pyCode
    }
}

# 6. jlink minimal runtime
$RuntimeDir = "$ClientTargetDir\runtime"
if (Test-Path $RuntimeDir) {
    Remove-Item -Recurse -Force $RuntimeDir
}
Write-Host "`n[2/4] Generating stripped minimal JRE via jlink..." -ForegroundColor Cyan
$Modules = "java.base,java.desktop,java.logging,jdk.unsupported,java.management"
& jlink --no-header-files --no-man-pages --strip-debug --compress=2 `
        --add-modules $Modules `
        --output $RuntimeDir

if ($LASTEXITCODE -ne 0 -or -not (Test-Path "$RuntimeDir\bin\javaw.exe")) {
    Write-Error "[ERROR] jlink execution failed."
    exit 1
}
$runtimeSize = [Math]::Round(((Get-ChildItem -Recurse $RuntimeDir | Measure-Object -Property Length -Sum).Sum / 1MB), 2)
Write-Host "[OK] Minimal JRE created: $runtimeSize MB" -ForegroundColor Green

# 7. jpackage native app-image with clean isolated staging input
$InputStageDir = "$ClientTargetDir\jpackage-input"
if (Test-Path $InputStageDir) {
    Remove-Item -Recurse -Force $InputStageDir
}
New-Item -ItemType Directory -Force -Path $InputStageDir | Out-Null
Copy-Item $ShadedJar.FullName "$InputStageDir\COGame-Client.jar" -Force

$JPackageDist = "$ClientTargetDir\jpackage-dist"
if (Test-Path $JPackageDist) {
    Remove-Item -Recurse -Force $JPackageDist
}
New-Item -ItemType Directory -Force -Path $JPackageDist | Out-Null

Write-Host "`n[3/4] Generating Windows native app bundle (COGame.exe) via jpackage..." -ForegroundColor Cyan
$JPackageArgs = @(
    "--type", "app-image",
    "--name", "COGame",
    "--app-version", $Version,
    "--input", $InputStageDir,
    "--main-jar", "COGame-Client.jar",
    "--main-class", "person.kinman.cogame.client.ClientMain",
    "--runtime-image", $RuntimeDir,
    "--dest", $JPackageDist,
    "--java-options", "-Dfile.encoding=UTF-8",
    "--java-options", "-Dawt.useSystemAAFontSettings=on",
    "--java-options", "-Dswing.aatext=true"
)
if (Test-Path $IcoPath) {
    $JPackageArgs += @("--icon", $IcoPath)
}

& jpackage @JPackageArgs
if ($LASTEXITCODE -ne 0 -or -not (Test-Path "$JPackageDist\COGame\COGame.exe")) {
    Write-Error "[ERROR] jpackage execution failed."
    exit 1
}
Write-Host "[OK] Windows application bundle generated: $JPackageDist\COGame\COGame.exe" -ForegroundColor Green

# 8. Write readme guide for end users
$ReadmeTxt = @"
================================================================================
COGame - Duannao Board Game Client (Windows Standalone v$Version)
================================================================================

[Plug & Play - No Java Required]
This standalone package comes with an embedded, optimized private JRE runtime.
You do NOT need to install Java, configure JAVA_HOME, or know anything about JARs.
Simply double-click 'COGame.exe' to launch and play immediately!

[Game Features]
1. Local 2-Player Battle: Face-to-face match on the same screen
2. Dual Grandmaster AI: Challenge 'CeTian (Voronoi Macro)' or 'JueYing (Manhattan Assassin)'
3. Online Lobby: Join rooms, set passwords, and play over network

[Controls]
- Left Mouse Click: Pathfind & move character to target cell / Turn around
- Right Mouse Click: Instantly lock cell edge based on clicked quadrant and pass turn
- Keyboard: WASD to move, L to lock edge, SPACE to advance, R to rotate, F11 for fullscreen
================================================================================
"@
Set-Content -Path "$JPackageDist\COGame\使用说明 (Windows).txt" -Value $ReadmeTxt -Encoding UTF8

# 9. Compress to distribution zip
Write-Host "`n[4/4] Archiving standalone distribution zip..." -ForegroundColor Cyan
$DistDir = "$ProjectRoot\distribution"
New-Item -ItemType Directory -Force -Path $DistDir | Out-Null
$DistZip = "$DistDir\COGame-Windows-x64.zip"

if (Test-Path $DistZip) {
    Remove-Item -Force $DistZip
}

$pyZip = @"
import zipfile, os
from pathlib import Path

source_dir = Path(r'$JPackageDist\COGame')
output_zip = r'$DistZip'

with zipfile.ZipFile(output_zip, 'w', compression=zipfile.ZIP_DEFLATED) as zout:
    for root, dirs, files in os.walk(source_dir):
        for f in files:
            full_path = Path(root) / f
            rel_path = Path('COGame') / full_path.relative_to(source_dir)
            zout.write(full_path, rel_path.as_posix())
"@
& python -c $pyZip

$zipSize = [Math]::Round(((Get-Item $DistZip).Length / 1MB), 2)

# Copy artifacts to distribution directory
Copy-Item $ShadedJar.FullName "$DistDir\COGame-Client.jar" -Force
$Launch4jExe = "$ClientTargetDir\COGame-Client.exe"
if (Test-Path $Launch4jExe) {
    Copy-Item $Launch4jExe "$DistDir\COGame-Client.exe" -Force
}
$ServerJar = Get-ChildItem -Path "$ProjectRoot\cogame-server\target" -Filter "cogame-server-*.jar" | Where-Object { $_.Name -notlike "original-*" } | Select-Object -First 1
if ($ServerJar) {
    Copy-Item $ServerJar.FullName "$DistDir\COGame-Server.jar" -Force
}

# Clean temporary staging directories
Remove-Item -Recurse -Force $InputStageDir -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force $JPackageDist -ErrorAction SilentlyContinue

Write-Host "==================================================" -ForegroundColor Green
Write-Host "[SUCCESS] Standalone Windows distribution created!" -ForegroundColor Green
Write-Host "[ZIP] $DistZip ($zipSize MB)" -ForegroundColor Green
Write-Host "[RUN] Double click COGame\COGame.exe to play without Java installed!" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green

