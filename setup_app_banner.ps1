# =====================================
# OneDrive統合ファイル生成 + Claude依頼ガイド自動出力（Windows PowerShell）
# =====================================

Write-Host "📦 OneDrive統合ファイルのセットアップを開始..." -ForegroundColor Green

# -------------------------------------
# 1. ディレクトリ作成
# -------------------------------------
$directories = @(
    "app\src\main\java\com\example\tvmoview\data\model",
    "app\src\main\java\com\example\tvmoview\data\api", 
    "app\src\main\java\com\example\tvmoview\data\auth",
    "app\src\main\java\com\example\tvmoview\data\repository",
    "app\src\main\java\com\example\tvmoview\presentation\screens"
)

foreach ($dir in $directories) {
    if (!(Test-Path $dir)) {
        New-Item -ItemType Directory -Force -Path $dir | Out-Null
    }
}
Write-Host "✅ ディレクトリ作成完了" -ForegroundColor Yellow

# -------------------------------------
# 2. 各ソースコードファイルを生成
# -------------------------------------
# OneDriveModels.kt
$oneDriveModels = @'
（←ここに OneDriveModels.kt の内容をそのまま貼る）
'@
$oneDriveModels | Out-File -FilePath "app\src\main\java\com\example\tvmoview\data\model\OneDriveModels.kt" -Encoding UTF8 -NoNewline
Write-Host "✅ OneDriveModels.kt 作成完了" -ForegroundColor Cyan

# OneDriveApiService.kt
$apiService = @'
（←ここに OneDriveApiService.kt の内容をそのまま貼る）
'@
$apiService | Out-File -FilePath "app\src\main\java\com\example\tvmoview\data\api\OneDriveApiService.kt" -Encoding UTF8 -NoNewline
Write-Host "✅ OneDriveApiService.kt 作成完了" -ForegroundColor Cyan

# AuthenticationManager.kt
$authManager = @'
（←ここに AuthenticationManager.kt の内容をそのまま貼る）
'@
$authManager | Out-File -FilePath "app\src\main\java\com\example\tvmoview\data\auth\AuthenticationManager.kt" -Encoding UTF8 -NoNewline
Write-Host "✅ AuthenticationManager.kt 作成完了" -ForegroundColor Cyan

# OneDriveRepository.kt
$repository = @'
（←ここに OneDriveRepository.kt の内容をそのまま貼る）
'@
$repository | Out-File -FilePath "app\src\main\java\com\example\tvmoview\data\repository\OneDriveRepository.kt" -Encoding UTF8 -NoNewline
Write-Host "✅ OneDriveRepository.kt 作成完了" -ForegroundColor Cyan

# LoginScreen.kt
$loginScreen = @'
（←ここに LoginScreen.kt の内容をそのまま貼る）
'@
$loginScreen | Out-File -FilePath "app\src\main\java\com\example\tvmoview\presentation\screens\LoginScreen.kt" -Encoding UTF8 -NoNewline
Write-Host "✅ LoginScreen.kt 作成完了" -ForegroundColor Cyan

# -------------------------------------
# 3. Claude用ガイド CLAUDE.md を生成
# -------------------------------------
$claudeGuide = @'
# CLAUDE.md - Claude用ガイドライン

## ✅ 使用可能な依存ライブラリ（これ以外は禁止）
- okhttp 4.12.0
- retrofit2 2.9.0
- kotlinx.coroutines 1.7.3
- gson 2.10.1
- androidx.security:security-crypto:1.1.0-alpha06

## ❌ 禁止事項
- 未導入の依存（Ktor, Apollo等）を使わないこと
- build.gradle.kts を自動で書き換えない（提案レベルに留める）
- Compose以外のUIライブラリは禁止（例: XML, Jetpack ViewSystem）

## ⚙ 環境設定
- Kotlin: 2.0.0 / Compose Compiler: 2.0.0
- minSdk: 26 / targetSdk: 34
- Android Studio: Giraffe+
'@
$claudeGuide | Out-File -FilePath "CLAUDE.md" -Encoding UTF8 -NoNewline
Write-Host "📘 CLAUDE.md 作成完了" -ForegroundColor Green

# -------------------------------------
# 4. build.gradle.kts 依存ヒント出力
# -------------------------------------
$gradleHint = @'
// build.gradle.kts - OneDrive連携に必要な依存
dependencies {
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
}
'@
$gradleHint | Out-File -FilePath "build_dependency_hint.kts" -Encoding UTF8 -NoNewline
Write-Host "🛠️ build_dependency_hint.kts 作成完了" -ForegroundColor Green

# -------------------------------------
# 5. 完了ログ
# -------------------------------------
Write-Host ""
Write-Host "✅ すべてのファイル生成が完了しました！" -ForegroundColor Green
Write-Host ""
Write-Host "📄 作成されたファイル：" -ForegroundColor Yellow
Write-Host "- OneDriveModels.kt"
Write-Host "- OneDriveApiService.kt"
Write-Host "- AuthenticationManager.kt"
Write-Host "- OneDriveRepository.kt"
Write-Host "- LoginScreen.kt"
Write-Host "- CLAUDE.md（AI依頼テンプレ）"
Write-Host "- build_dependency_hint.kts（Gradle依存の補助）"
Write-Host ""
Write-Host "🔒 次にやること：" -ForegroundColor Yellow
Write-Host "1. build.gradle.kts に build_dependency_hint.kts の内容をコピペ"
Write-Host "2. Claudeに依頼する際は CLAUDE.md をそのまま貼る"
Write-Host "3. libs.versions.toml（任意）で依存バージョンをロック"
Write-Host ""
