param([string]$Python = 'python')
$ErrorActionPreference = 'Stop'
# Some Python distributions require an additional distrust certificate alongside ssl.py.
$sslAsset = & $Python -c "import ssl,pathlib; p=pathlib.Path(ssl.__file__).with_name('codex-rejected-cas.pem'); print(str(p) if p.is_file() else '')"
$extraData = @()
if ($sslAsset) { $extraData = @('--add-data', "$sslAsset;.") }
& $Python -m PyInstaller @extraData --noconfirm --onefile --windowed --name MangoGoatNapper-Companion --collect-all TikTokLive --collect-all TikTokLiveProto --collect-all EulerApiSdk --copy-metadata betterproto2 --distpath "$PSScriptRoot\..\dist" --workpath "$PSScriptRoot\..\build\companion" --specpath "$PSScriptRoot\..\build" "$PSScriptRoot\launcher.py"
if ($LASTEXITCODE -ne 0) { throw 'Companion build failed' }
