
# 🤖 生成AI開発テンプレート（Android TV × OneDrive メディアアプリ）

このプロジェクトは **Android TV 向け OneDrive メディアブラウザアプリ**です。  
**100%生成AI**で開発しており、**既存コードと整合性の取れた提案のみ**を受け付けます。

---

## ✅ 開発依頼時のルール

- **既存のクラス／プロパティ／関数を再定義しないでください**
- **既存構造と重複・衝突しないように設計してください**
- **提案は50行以内に収めてください**（生成AIエラー防止のため）
- **ViewModel / Repository / Navigation 等の役割分離は完了済みです**
- **Material3 Compose UIのみ使用してください**（Material2禁止）

---

## 📁 設計補助ファイル

以下を毎回参照してください：

| ファイル名              | 内容                                 | 更新方法                           |
|------------------------|--------------------------------------|------------------------------------|
| `PROJECT_STRUCTURE.md` | ディレクトリ構成とレイヤー責務の説明           | 手動 or スクリプト整理                |
| `SYMBOLS.md`           | クラス／関数／プロパティ一覧（階層付き）       | `SYMBOLS_code.ps1` で自動生成      |
| `USAGE_MAP.md`         | クラスの呼び出し関係（設計補助用）             | 手動整理 or スクリプト開発中          |

---

## 📦 使用可能ライブラリ一覧（これ以外は禁止）

| 分野・役割                    | ライブラリ                            | バージョン          |
|-----------------------------|-------------------------------------|---------------------|
| 言語・ビルド環境               | Kotlin                              | 2.0.0               |
|                               | Gradle                              | 8.11.1              |
|                               | Java (JDK)                          | 21                  |
|                               | Compose Compiler                    | 2.0.0               |
| Android SDK                | minSdk / targetSdk                  | 26 / 34             |
| UI                          | Jetpack Compose BOM                 | 2024.02.00（BOM）   |
|                               | Compose UI                         | BOM準拠             |
|                               | Material3                          | 1.2.0               |
|                               | Compose for TV                     | 1.0.0               |
| 画像                         | Coil                                | 2.5.0               |
| 動画                         | ExoPlayer（androidx.media3）        | 1.2.1               |
| OneDrive連携                | Microsoft Graph Java SDK            | 6.0.0               |
| 認証                         | MSAL                                | 最新安定版          |
| 通信                         | Retrofit                            | 2.9.0               |
|                               | OkHttp                              | 4.12.0              |
| JSON                        | Gson                                | 2.8.9               |
| DB                          | Room                                | 2.6.1               |
| ナビゲーション               | Navigation-Compose                  | 2.7.6               |
| Activityコンポーネント        | androidx.activity:*                 | 1.8.2               |
| ライフサイクル管理            | androidx.lifecycle:*                | 2.7.0               |
| コルーチン                   | kotlinx-coroutines-*                | 1.7.3               |
| コレクション操作             | androidx.collection:*               | 1.4.0               |
| アノテーション関連            | androidx.annotation:*               | 1.7.0               |

---
tv-foundation:1.0.0 は 存在しないため使えない（→ビルドエラーになる）
ただし、1.0.0-alpha12 は Google公式も採用中（Jetstreamなど） の実績あるバージョン
tv-material:1.0.0 は安定版なので問題なく使用可能
Compose本体（foundation）への統合も始まっているが、現時点では TvLazy* を使うのがUX的に最良


## 🛠️ Kotlin Symbol Processing (KSP) の使用方針

- KSP は `2.0.20-1.0.24` を標準バージョンとする（Kotlin 2.0.0 + Compose 2024系と整合）
- `pluginManagement` セクションに `google()` および `mavenCentral()` を明記し、KSP解決エラーを防止すること
- `libs.versions.toml` では以下のように定義する：

```toml
[plugins]
ksp = { id = "com.google.devtools.ksp", version = "2.0.20-1.0.24" }

[libraries]
ksp-api = { group = "com.google.devtools.ksp", name = "symbol-processing-api", version.ref = "ksp" }
```

- `build.gradle.kts` 側でも下記のように指定：

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.0.20-1.0.24"
}
```

---

## ☑️ ビルドツール関連ルール（Build Processor）

- ✅ RoomやHiltなどアノテーション処理系ライブラリは **KSP のみ使用可**
- ❌ `kapt(...)` の依存追加、および `id("kotlin.kapt")` の使用は禁止
- ✅ Kotlin 2.0 / Gradle 8 対応を前提に KSP を基本とする
- ✅ Room, Dagger, Hilt などは KSP 対応版に統一
- 🧪 ビルドログ中に `kaptGenerateStubsDebugKotlin` が出現した場合、KAPT依存が残っているため即時修正

---

## ❌ 禁止ライブラリ・禁止事項

- Material2 / AppCompat 系 UI
- Ktor / Apollo / Moshi（未採用のHTTP/JSON）
- Microsoft OneDrive Android SDK（非推奨）
- Guava の旧バージョン（listenablefuture:1.0など）
- Coilの混在使用（複数バージョン禁止）
- Compose関連の非BOM準拠 UIライブラリ

---

## 🚫 禁止バージョン

- Kotlin 1.9.x 系（Compose BOM 2024系と非互換のため）
- KSP 1.0.15 以下（Room 2.6.1 との最新互換性保証なし）
- Compose Compiler 1.5.x 以下

---

## 🧪 補足（旧構成との比較）

以前は以下の構成が暫定安定構成として使われていました：

- Kotlin 1.9.21
- Compose Compiler 1.5.7
- KSP 1.0.15

→ これらは現在非推奨です。生成AI・人間問わず提案には使用しないでください。

---

## 🚨 特に注意するポイント

- **新規ライブラリの導入は禁止（一覧の中で実装）**
- **バージョンカタログ（libs.versions.toml）に定義されていない依存は使わない**
- **Kotlin, Compose, OkHttpなどは全プロジェクトでバージョン統一必須**

---

（自動生成日: 2025-06-30 / Kotlin 2.0 + KSP 2.0.20 対応版）
