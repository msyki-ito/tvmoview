# Gradleファイル修正スクリプト
# org.jetbrains.kotlin.plugin.compose プラグインの削除対応

Write-Host "Gradleファイル修正を開始します..." -ForegroundColor Cyan

# 1. プロジェクトレベル build.gradle.kts の修正
$projectBuildGradlePath = "build.gradle.kts"
if (Test-Path $projectBuildGradlePath) {
    $projectBuildGradleContent = @"
plugins {
    id("com.android.application") version "8.1.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.21" apply false
    // org.jetbrains.kotlin.plugin.compose を削除
    id("com.google.devtools.ksp") version "1.0.15" apply false
}
"@

    Set-Content -Path $projectBuildGradlePath -Value $projectBuildGradleContent -Encoding UTF8
    Write-Host "✅ Updated: $projectBuildGradlePath" -ForegroundColor Green
} else {
    Write-Host "⚠️  Warning: $projectBuildGradlePath not found" -ForegroundColor Yellow
}

# 2. app/build.gradle.kts の修正
$appBuildGradlePath = "app/build.gradle.kts"
if (Test-Path $appBuildGradlePath) {
    $appBuildGradleContent = @"
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // org.jetbrains.kotlin.plugin.compose を削除
    id("com.google.devtools.ksp") version "1.0.15"
}

android {
    namespace = "com.example.tvmoview"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.tvmoview"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        // Kotlin 1.9.21 に対応する Compose Compiler バージョン
        kotlinCompilerExtensionVersion = "1.5.7"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose BOM - 開発ルールに従って 2023.10.00 を使用
    implementation(platform("androidx.compose:compose-bom:2023.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // TV Support
    implementation("androidx.tv:tv-foundation:1.0.0-alpha10")
    implementation("androidx.tv:tv-material:1.0.0-alpha10")
    implementation("androidx.leanback:leanback:1.0.0")

    // Media3 ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-common:1.2.1")

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil:2.5.0")

    // Room Database - KSP使用
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // HTTP通信
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
"@

    Set-Content -Path $appBuildGradlePath -Value $appBuildGradleContent -Encoding UTF8
    Write-Host "✅ Updated: $appBuildGradlePath" -ForegroundColor Green
} else {
    Write-Host "⚠️  Warning: $appBuildGradlePath not found" -ForegroundColor Yellow
}

# 3. gradle.properties の確認・作成
$gradlePropertiesPath = "gradle.properties"
$gradlePropertiesContent = @"
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.enableJetifier=true
"@

if (Test-Path $gradlePropertiesPath) {
    $existingContent = Get-Content $gradlePropertiesPath -Raw -ErrorAction SilentlyContinue
    $needsUpdate = $false
    
    if ($existingContent -notmatch "android.useAndroidX=true") {
        Add-Content -Path $gradlePropertiesPath -Value "`nandroid.useAndroidX=true"
        $needsUpdate = $true
    }
    if ($existingContent -notmatch "kotlin.code.style=official") {
        Add-Content -Path $gradlePropertiesPath -Value "kotlin.code.style=official"
        $needsUpdate = $true
    }
    if ($existingContent -notmatch "android.nonTransitiveRClass=true") {
        Add-Content -Path $gradlePropertiesPath -Value "android.nonTransitiveRClass=true"
        $needsUpdate = $true
    }
    
    if ($needsUpdate) {
        Write-Host "✅ Updated: $gradlePropertiesPath" -ForegroundColor Green
    } else {
        Write-Host "ℹ️  $gradlePropertiesPath は既に最新です" -ForegroundColor Cyan
    }
} else {
    Set-Content -Path $gradlePropertiesPath -Value $gradlePropertiesContent -Encoding UTF8
    Write-Host "✅ Created: $gradlePropertiesPath" -ForegroundColor Green
}

Write-Host "`n🎯 Gradleファイル修正が完了しました！" -ForegroundColor Green
Write-Host "`n📝 修正内容:" -ForegroundColor Cyan
Write-Host "  ❌ org.jetbrains.kotlin.plugin.compose プラグインを削除" -ForegroundColor Yellow
Write-Host "  ✅ composeOptions で kotlinCompilerExtensionVersion = 1.5.7 を設定" -ForegroundColor Green
Write-Host "  ✅ Compose BOM 2023.10.00 (安定版) を使用" -ForegroundColor Green
Write-Host "  ✅ KSP 1.0.15 (安定版) を使用" -ForegroundColor Green
Write-Host "  ✅ gradle.properties を最適化" -ForegroundColor Green
Write-Host "`n💡 次のステップ:" -ForegroundColor Cyan
Write-Host "  1. Android Studio で Gradle Sync を実行" -ForegroundColor White
Write-Host "  2. Clean Project を実行" -ForegroundColor White
Write-Host "  3. Rebuild Project を実行" -ForegroundColor White