# 主要関数要約

このドキュメントではリポジトリ内で特に重要なクラスとその役割を簡潔にまとめます。詳細な定義や引数は `SYMBOLS.md` を参照してください。

## プロジェクト構成
- **data**: API クライアント、認証、DB
- **domain**: ドメインモデル
- **presentation**: Compose UI (components, screens, viewmodels)
- **tv**: TV 向け拡張
- **ui**: 共通テーマ

## 認証
- `AuthenticationManager`
  - デバイスコードフローを利用した認証を担当。
  - `startDeviceCodeFlow` でデバイスコード取得、`pollForToken` でトークン取得。
  - `getSavedToken` や `isAuthenticated` により保存済みトークンの確認・削除を行う。

## データベース
- `MediaDatabaseProvider`: Room データベースの初期化を行う。
- `MediaDao`, `FolderSyncDao`: メディアキャッシュとフォルダ同期情報の CRUD を提供。
- `CachedMediaItem`, `FolderSyncStatus`: これら Dao が扱うエンティティ。

## ドメインモデル
- `MediaItem`: ファイルやフォルダを表すモデル。`isVideo` や `formattedSize` 等の拡張プロパティを持つ。

## リポジトリ
- `MediaRepository`: テスト用のローカルデータを供給。
- `OneDriveRepository`: OneDrive API からアイテム取得とキャッシュを管理。`getFolderItems`、`getDownloadUrl`、`sync` を実装。

## ViewModel
- `MediaBrowserViewModel`: メディア一覧の読み込み、表示モード切替、並び替えを管理する。`loadItems`、`toggleViewMode`、`setSortBy`、`refresh` を持つ。表示モードは `ViewMode.TILE` と `ViewMode.HULU_STYLE` を切り替え。

## UI コンポーネント
`ModernMediaCard`、`HuluMediaCard`、`ModernTileView`、`HuluStyleView`、`SortDialog` など、一覧表示や並び替え操作を提供する Compose コンポーネント群。タイル表示と Hulu 風表示を切り替え可能。

## 画面 (Screens)
- `LoginScreen`: デバイスコードの表示と認証処理。
- `ModernMediaBrowser`: OneDrive 上のメディア閲覧画面。
- `HighQualityPlayerScreen`: ExoPlayer を用いた動画再生。
- `SettingsScreen`: アプリ設定画面。

## ナビゲーション
- `AppNavigation`: Compose Navigation を利用しホーム、フォルダ、プレイヤー、設定画面へ遷移。

## メインエントリ
- `MainActivity`: 各種リポジトリの初期化と認証状態確認を行い、`AppNavigation` を起動。

## TV 最適化
- `FireTVOptimizations.initializeForFireTV`: Fire TV 固有の最適化を行うプレースホルダー。

## テーマ
- `TVMovieTheme`: Material3 ベースのカラースキームとタイポグラフィを定義。`HuluColors` を使うことで Hulu 風カラーパレットを適用可能。