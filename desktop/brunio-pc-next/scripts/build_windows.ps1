$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    $pcVersion = if ($env:GITHUB_RUN_NUMBER) { '2.0.0-rc' + $env:GITHUB_RUN_NUMBER } else { '2.0.0-rc1' }
    if ($pcVersion -notmatch '^2\.0\.0-rc\d+$') { throw 'Invalid PC build version' }
    Set-Content -Path 'backend/build_version.py' -Value ('PC_VERSION=' + [char]39 + $pcVersion + [char]39) -Encoding utf8
    $resourceRoot = Join-Path $projectRoot 'runtime-resources/windows'
    if (Test-Path $resourceRoot) { Remove-Item $resourceRoot -Recurse -Force }
    New-Item -ItemType Directory -Force $resourceRoot | Out-Null
    python -m pip install PyInstaller==6.16.0
    if ($LASTEXITCODE -ne 0) { throw 'PyInstaller installation failed' }
    $legacyAssets = (Resolve-Path '../brunio-pc/assets').Path
    $calendarTeams = (Resolve-Path 'assets/calendar_teams.json').Path
    python -m PyInstaller --noconfirm --clean --onedir --console --name BruniO-service --distpath build/service --workpath build/pyinstaller --specpath build --paths "$projectRoot/backend" --paths "$projectRoot/../brunio-pc" --add-data "$legacyAssets/final_channels_rp97.m3u;assets" --add-data "$calendarTeams;assets" backend/bridge.py
    if ($LASTEXITCODE -ne 0) { throw 'Service packaging failed' }
    Copy-Item 'build/service/BruniO-service' "$resourceRoot/bridge" -Recurse -Force
    Copy-Item '../brunio-pc/assets' "$resourceRoot/assets" -Recurse -Force
    if (-not (Test-Path "$resourceRoot/assets/brunio_splash.png")) { throw 'Bundled artwork missing from resource input' }
    Copy-Item '../brunio-pc/LICENSE.txt' $resourceRoot -Force
    Copy-Item 'THIRD_PARTY_NOTICES.md' $resourceRoot -Force
    if (-not $env:BRUNIO_VLC_HOME -or -not (Test-Path "$env:BRUNIO_VLC_HOME/libvlc.dll")) { throw 'Verified VLC runtime must be prepared first' }
    Copy-Item $env:BRUNIO_VLC_HOME "$resourceRoot/vlc" -Recurse -Force
    $savedAppData = $env:LOCALAPPDATA
    $smokeDataRoot = Join-Path ([System.IO.Path]::GetTempPath()) ('BruniO-smoke-' + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Force $smokeDataRoot | Out-Null
    $env:LOCALAPPDATA = $smokeDataRoot
    $probe = '{"id":1,"method":"session.restore"}' | & "$resourceRoot/bridge/BruniO-service.exe"
    if ($LASTEXITCODE -ne 0) { throw 'Packaged service launch failed' }
    $parsed = $probe | ConvertFrom-Json
    if ($parsed.id -ne 1 -or $parsed.error) { throw 'Packaged IPC probe failed' }
    Push-Location compose
    try {
        gradle --no-daemon createDistributable
        if ($LASTEXITCODE -ne 0) { throw 'JVM application image failed' }
    } finally { Pop-Location }
    $launcher = Join-Path $projectRoot 'compose/build/compose/binaries/main/app/BruniO-PC/BruniO-PC.exe'
    if (-not (Test-Path $launcher)) { throw 'Packaged launcher missing' }
    Get-ChildItem (Join-Path (Split-Path $launcher) 'app/resources') -Depth 1 | Select-Object FullName | Format-Table -AutoSize
    # Smoke test must use bundled natives, not the VLC directory prepared for unit tests.
    $savedVlcHome = $env:BRUNIO_VLC_HOME
    $savedPlugins = $env:VLC_PLUGIN_PATH
    $savedProcessPath = $env:PATH
    try {
        Remove-Item Env:BRUNIO_VLC_HOME -ErrorAction SilentlyContinue
        Remove-Item Env:VLC_PLUGIN_PATH -ErrorAction SilentlyContinue
        $env:PATH = (($env:PATH -split ';') | Where-Object { $_ -ne $savedVlcHome }) -join ';'
        $smoke = Start-Process $launcher -ArgumentList '--smoke' -PassThru -Wait
        if ($smoke.ExitCode -ne 0) { throw 'Bundled application smoke test failed' }
        $reviewRoot = Join-Path $projectRoot 'ui-review'
        $visual = Start-Process $launcher -ArgumentList @('--visual-smoke', ('"' + $reviewRoot + '"')) -PassThru
        if (-not $visual.WaitForExit(120000)) { $visual.Kill(); throw 'Visual smoke timed out' }
        if ($visual.ExitCode -ne 0) { throw 'Visual smoke failed' }
    } finally {
        $env:BRUNIO_VLC_HOME = $savedVlcHome
        $env:VLC_PLUGIN_PATH = $savedPlugins
        $env:PATH = $savedProcessPath
        $env:LOCALAPPDATA = $savedAppData
    }
    $compiler = 'C:\Program Files (x86)\Inno Setup 6\ISCC.exe'
    if (-not (Test-Path $compiler)) { choco install innosetup -y --no-progress; if ($LASTEXITCODE -ne 0) { throw 'Inno Setup installation failed' } }
    & $compiler /DAppVersion=$pcVersion scripts/installer.iss
    if ($LASTEXITCODE -ne 0) { throw 'Installer compilation failed' }
    python scripts/prepare_update.py
    if ($LASTEXITCODE -ne 0) { throw 'Update metadata failed' }
} finally {
    if ($savedAppData) { $env:LOCALAPPDATA = $savedAppData }
    Pop-Location
}

