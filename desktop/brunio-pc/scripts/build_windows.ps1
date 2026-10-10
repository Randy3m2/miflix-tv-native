$ErrorActionPreference = 'Stop'
$version = '1.0.0-rc2'
$zipUrl = 'https://downloads.videolan.org/videolan/vlc/3.0.23/win64/vlc-3.0.23-win64.zip'
New-Item -ItemType Directory -Force vendor | Out-Null
Invoke-WebRequest $zipUrl -OutFile vendor/vlc.zip
Invoke-WebRequest "$zipUrl.sha256" -OutFile vendor/vlc.sha256
$expected = ((Get-Content vendor/vlc.sha256 -Raw).Trim() -split '\s+')[0].ToLowerInvariant()
if ($expected -notmatch '^[a-f0-9]{64}$' -or (Get-FileHash vendor/vlc.zip -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expected) { throw 'VLC archive checksum failed' }
Expand-Archive vendor/vlc.zip -DestinationPath vendor -Force
python scripts/smoke_vlc.py
if ($LASTEXITCODE -ne 0) { throw 'VLC smoke test failed' }
python -m PyInstaller --noconfirm --clean --windowed --onedir --name BruniO-PC --icon assets/brunio.ico --add-data 'assets;assets' --add-data 'LICENSE.txt;.' --add-data 'THIRD_PARTY_NOTICES.md;.' --add-data 'vendor/vlc-3.0.23;vlc' --hidden-import vlc main.py
if ($LASTEXITCODE -ne 0) { throw 'PyInstaller failed' }
$smoke = Start-Process -FilePath dist/BruniO-PC/BruniO-PC.exe -ArgumentList '--smoke-vlc' -PassThru -Wait
if ($smoke.ExitCode -ne 0) { throw 'Packaged launch smoke test failed' }
$iscc = 'C:\Program Files (x86)\Inno Setup 6\ISCC.exe'
if (-not (Test-Path $iscc)) { choco install innosetup -y --no-progress; if ($LASTEXITCODE -ne 0) { throw 'Inno Setup installation failed' } }
& $iscc /DAppVersion=$version scripts/installer.iss
if ($LASTEXITCODE -ne 0) { throw 'Installer failed' }
python scripts/prepare_update.py
if ($LASTEXITCODE -ne 0) { throw 'Update metadata failed' }
