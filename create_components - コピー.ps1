<#
    claude_md.ps1 (debug版)
    1. モジュール名が不明なら :app で試行
    2. release→debug→runtimeClasspath の順で取得
    3. 取得行を deps_raw.txt に保存（デバッグ用）
    4. 万能正規表現で group:name:version を抽出
#>

$ErrorActionPreference = "Stop"
$module = ":app"   # ← 固定したい場合はここを直接書き換え

# --- 依存ツリー取得 ---
$configs = @("releaseRuntimeClasspath","debugRuntimeClasspath","runtimeClasspath")
$depOutput = $null
foreach ($cfg in $configs) {
    Write-Host "🔍 $module ($cfg) を試行..." -F DarkGray
    $out = & ".\gradlew.bat" "$module:dependencies" --configuration $cfg --console plain 2>&1
    if ($LASTEXITCODE -eq 0) { $depOutput = $out; break }
}

if (-not $depOutput) { Write-Error "依存ツリー取得失敗"; exit 1 }

# デバッグ用に生ログ保存
[IO.File]::WriteAllLines("deps_raw.txt",$depOutput)

# --- group:name:version 抽出 ---
#   ^\s* (任意の枝記号) --- <group>:<name>:<version>
$regex = '^\s*[+|\\]?[\\| ]*---\s+([^\s:]+):([^\s:]+):([^\s:]+)'
$libs  = @{}

foreach ($line in $depOutput) {
    if ($line -match $regex) {
        $key = "${Matches[1]}:${Matches[2]}"
        if (-not $libs.ContainsKey($key)) { $libs[$key] = $Matches[3] }
    }
}

# --- CLAUDE.md 出力 ---
$md = @("# CLAUDE.md – 依存ライブラリ & ポリシー", "")
$md += "## ✅ 使用可能ライブラリ"
if ($libs.Count) {
    foreach ($k in $libs.Keys | Sort-Object) { $md += "- $k $($libs[$k])" }
} else { $md += "_（依存を検出できませんでした）_" }
$md += @("",
    "## ❌ 禁止事項",
    "- 未導入の依存を追加しない（Ktor, Apollo など）",
    "- build.gradle.kts の変更は必ずレビューを通す",
    "- UI は Jetpack Compose Material3 のみ",
    "",
    "## ⚙ 環境",
    "- Kotlin 2.0.0 / Compose Compiler 2.0.0",
    "- minSdk 26 / targetSdk 34"
)

[IO.File]::WriteAllLines("CLAUDE.md",$md)
Write-Host "📘 CLAUDE.md を生成しました（${($libs.Count)} 件の依存）。" -F Green
