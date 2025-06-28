<#  generate_docs.ps1
    ├─ command\library_code.ps1
    ├─ command\SYMBOLS_code.ps1
    ├─ command\USAGE_MAP_code.ps1
    └─ command\PROJECT_STRUCTURE.ps1
    を順番に実行するラッパー。
    使用例: pwsh ./generate_docs.ps1 [-Root <ソースルート>]
#>

param(
    [string]$Root = "."   # Kotlin / Java ソースのルート
)

$ErrorActionPreference = "Stop"

# プロジェクト直下（このファイルがある場所）
$projDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# command ディレクトリ
$cmdDir = Join-Path $projDir "..\command"

# --- 1) 共通ライブラリ（dot source で読み込み） --------------------
#. (Join-Path $cmdDir "library_code.ps1")

# --- 2) 個別ドキュメント生成スクリプトを実行 -----------------------
& (Join-Path $cmdDir "SYMBOLS_code.ps1")       -root $Root
& (Join-Path $cmdDir "USAGE_MAP_code.ps1")     -root $Root
& (Join-Path $cmdDir "PROJECT_STRUCTURE.ps1")     -root $Root

Write-Host "🎉 All docs generated successfully." -ForegroundColor Green
