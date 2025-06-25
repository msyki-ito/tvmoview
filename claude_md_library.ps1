# =====================================================================
# claude_md_debug.ps1 - CLAUDE.md を自動生成（デバッグ強化版）
# =====================================================================
param(
    [string]$projectRoot = ".",
    [string]$module      = "app",
    [switch]$verbose     = $false
)

function Write-Debug($message, $color = "Cyan") {
    if ($verbose) {
        Write-Host "[DEBUG] $message" -ForegroundColor $color
    }
}

function Test-GradleWrapper {
    $gradlewBat = Join-Path $projectRoot "gradlew.bat"
    $gradlew = Join-Path $projectRoot "gradlew"
    
    if (Test-Path $gradlewBat) {
        Write-Debug "Found gradlew.bat at: $gradlewBat"
        return $gradlewBat
    } elseif (Test-Path $gradlew) {
        Write-Debug "Found gradlew at: $gradlew"
        return $gradlew
    } else {
        Write-Host "❌ Gradle Wrapper not found!" -ForegroundColor Red
        Write-Host "Looking for: $gradlewBat or $gradlew" -ForegroundColor Yellow
        return $null
    }
}

# メイン処理開始
Write-Host "🚀 Starting dependency extraction..." -ForegroundColor Green
Write-Debug "Project root: $(Resolve-Path $projectRoot)"

Set-Location $projectRoot
$ErrorActionPreference = 'Continue'  # エラーでも続行

# 0. プロジェクト構造確認
Write-Debug "Checking project structure..."
Get-ChildItem -Name | ForEach-Object { Write-Debug "Found: $_" }

# 1. settings.gradle からモジュール名を自動推定
$settingsFiles = Get-ChildItem -Filter 'settings.gradle*' -ErrorAction SilentlyContinue
if ($settingsFiles.Count -eq 0) {
    Write-Host "⚠️  settings.gradle not found, using default module: $module" -ForegroundColor Yellow
} else {
    Write-Debug "Found settings file: $($settingsFiles[0].Name)"
    $settingsContent = Get-Content $settingsFiles[0] -Raw -ErrorAction SilentlyContinue
    if ($settingsContent -match 'include\((["''])(.+?)\1\)') {
        $module = $Matches[2].Replace(":", "")  # ":app" → "app"
        Write-Debug "Extracted module from settings: $module"
    }
}
Write-Host "📦 Target Module: $module" -ForegroundColor Yellow

# 2. Gradle Wrapper 確認
$gradleCmd = Test-GradleWrapper
if (-not $gradleCmd) {
    Write-Host "❌ Cannot proceed without Gradle Wrapper" -ForegroundColor Red
    exit 1
}

# 3. 依存関係取得（複数の設定を試行）
$temp = [IO.Path]::GetTempFileName()
$errorTemp = [IO.Path]::GetTempFileName()
Write-Debug "Temp files: $temp, $errorTemp"

$configurations = @(
    "debugRuntimeClasspath",
    "releaseRuntimeClasspath", 
    "runtimeClasspath",
    "compileClasspath"
)

$deps = @()
foreach ($config in $configurations) {
    $arg = "$module`:dependencies --configuration $config --console plain"
    Write-Host "🔍 Trying configuration: $config" -ForegroundColor Cyan
    Write-Debug "Command: $gradleCmd $arg"
    
    try {
        $process = Start-Process $gradleCmd -ArgumentList $arg -RedirectStandardOutput $temp -RedirectStandardError $errorTemp -NoNewWindow -Wait -PassThru
        
        if ($process.ExitCode -eq 0) {
            $tempContent = Get-Content $temp -ErrorAction SilentlyContinue
            if ($tempContent -and $tempContent.Count -gt 5) {
                Write-Host "✅ Success with $config" -ForegroundColor Green
                $deps = $tempContent
                break
            } else {
                Write-Debug "Configuration $config returned empty or minimal output"
            }
        } else {
            $errorContent = Get-Content $errorTemp -ErrorAction SilentlyContinue
            Write-Debug "Configuration $config failed with exit code: $($process.ExitCode)"
            Write-Debug "Error output: $($errorContent -join "`n")"
        }
    } catch {
        Write-Debug "Exception with $config`: $($_.Exception.Message)"
    }
}

# フォールバック: 設定なしで試行
if ($deps.Count -eq 0) {
    Write-Host "🔄 Trying fallback without configuration..." -ForegroundColor Yellow
    $arg = "$module`:dependencies --console plain"
    Write-Debug "Fallback command: $gradleCmd $arg"
    
    try {
        $process = Start-Process $gradleCmd -ArgumentList $arg -RedirectStandardOutput $temp -RedirectStandardError $errorTemp -NoNewWindow -Wait -PassThru
        $deps = Get-Content $temp -ErrorAction SilentlyContinue
        $errorContent = Get-Content $errorTemp -ErrorAction SilentlyContinue
        
        Write-Debug "Fallback exit code: $($process.ExitCode)"
        Write-Debug "Output lines: $($deps.Count)"
        if ($errorContent) {
            Write-Debug "Error output: $($errorContent -join "`n")"
        }
    } catch {
        Write-Debug "Fallback exception: $($_.Exception.Message)"
    }
}

# 4. 出力結果の確認
Write-Host "📊 Captured $($deps.Count) lines from Gradle" -ForegroundColor Green

if ($verbose -and $deps.Count -gt 0) {
    Write-Debug "Sample output (first 10 lines):"
    $deps | Select-Object -First 10 | ForEach-Object { Write-Debug "  $_" }
}

# 5. 依存関係抽出（複数パターンの正規表現）
$patterns = @(
    '([A-Za-z0-9._-]+):([A-Za-z0-9._-]+):([A-Za-z0-9._+-]+)',  # 基本パターン
    '\+---\s+([A-Za-z0-9._-]+):([A-Za-z0-9._-]+):([A-Za-z0-9._+-]+)',  # ツリー表示パターン
    '\\---\s+([A-Za-z0-9._-]+):([A-Za-z0-9._-]+):([A-Za-z0-9._+-]+)'   # 別のツリー表示パターン
)

$libs = @()
foreach ($pattern in $patterns) {
    $matches = $deps | Where-Object { $_ -match $pattern } | ForEach-Object { 
        "$($Matches[1]):$($Matches[2]):$($Matches[3])"
    }
    $libs += $matches
}

$libs = $libs | Sort-Object | Get-Unique | Where-Object { $_ -notmatch '^:' }  # 空文字列除去
Write-Host "🔍 Extracted $($libs.Count) unique dependencies" -ForegroundColor Green

if ($verbose -and $libs.Count -gt 0) {
    Write-Debug "Sample dependencies (first 5):"
    $libs | Select-Object -First 5 | ForEach-Object { Write-Debug "  $_" }
}

# 6. 重複バージョン検出
$dupLines = @()
if ($libs.Count -gt 0) {
    $libs | Group-Object { ($_ -split ':')[0..1] -join ':' } | Where-Object { $_.Count -gt 1 } |
    ForEach-Object {
        $vers = $_.Group | ForEach-Object { ($_ -split ':')[-1] } | Sort-Object
        $dupLines += "- **$($_.Name)** → $([string]::Join(', ', $vers))"
    }
}

# 7. CLAUDE.md 生成
$md = @"
# 📦 プロジェクト依存関係概要

## ❌ 禁止事項
- BOM / Version Catalog に無い依存を直接 version 指定で追加しない
- `api` で外部ライブラリを公開しない（基本は implementation）
- Material2, AppCompat UI を新規導入しない（Compose Material3 統一）
- 重複 Kotlin stdlib を追加しない
- build.gradle に新依存を追加する場合は “提案 PR” のみ可


## ⚙ 環境
- Kotlin 2.0.0 ／ Compose Compiler 2.0.0
- Gradle 8.11.1 ／ Java 21
- minSdk 26 ／ targetSdk 34

## ✅ 使用可能ライブラリ
"@

if ($libs.Count -gt 0) {
    $libs | ForEach-Object { $md += "- $_`n" }
} else {
    $md += "_（依存を検出できませんでした）_`n`n"
    $md += "### 🔧 トラブルシューティング`n"
    $md += "1. プロジェクトルートで実行していますか？`n"
    $md += "2. \`gradlew.bat\` または \`gradlew\` が存在しますか？`n"
    $md += "3. \`./gradlew $module`:dependencies\` を手動実行してみてください`n"
    $md += "4. \`-verbose\` オプションでデバッグ情報を確認してください`n"
}

if ($dupLines.Count -gt 0) {
    $md += "`n---`n## 🔍 重複バージョン要統合`n"
    $dupLines | ForEach-Object { $md += "$_`n" }
}

# 8. ファイル出力
$outputPath = Join-Path $projectRoot "CLAUDE.md"
$md | Out-File -FilePath $outputPath -Encoding UTF8 -Force

# 9. 結果表示
Write-Host "`n🎉 CLAUDE.md 生成完了: $outputPath" -ForegroundColor Green
Write-Host "📊 検出された依存関係: $($libs.Count) 個" -ForegroundColor Yellow

if ($libs.Count -eq 0) {
    Write-Host "`n❗ 依存関係が検出されませんでした。以下を確認してください:" -ForegroundColor Red
    Write-Host "   1. 正しいプロジェクトルートで実行していますか？" -ForegroundColor Yellow
    Write-Host "   2. Gradle プロジェクトですか？" -ForegroundColor Yellow
    Write-Host "   3. モジュール名 '$module' は正しいですか？" -ForegroundColor Yellow
    Write-Host "   4. 手動実行: .\gradlew.bat $module`:dependencies" -ForegroundColor Yellow
    Write-Host "   5. デバッグ実行: .\claude_md_debug.ps1 -verbose" -ForegroundColor Yellow
}

# 10. 一時ファイル削除
Remove-Item $temp -ErrorAction SilentlyContinue
Remove-Item $errorTemp -ErrorAction SilentlyContinue