# LogiClo プロジェクト構造ドキュメント

このドキュメントは、`LogiClo` (旧名 LaundryLoop) アプリケーションの現在のディレクトリ構造と、各コンポーネントの役割を解説します。
現在、アプリケーションは **旧UI実装から新UI実装への移行過渡期** にあります。リファクタリングを行う際は、この構造を理解することが重要です。

---

## 1. アーキテクチャ概要

推奨される「モダンAndroidアーキテクチャ」に基づき、以下の3層で構成されています。

1.  **UI Layer (`ui`)**: 画面描画とユーザー操作のハンドリング。
    *   **現状**: 旧実装 (`ui.logiclo`) と新実装 (`ui.dashboard`, `ui.closet` 等) が混在しています。
2.  **Domain Layer (`domain`)**: アプリのビジネスロジックとドメインモデル。UIやData層に依存しません。
3.  **Data Layer (`data`)**: データの取得と保存（Repositoryパターン）。

---

## 2. ディレクトリ詳細 (`app/src/main/java/com/example/myapplication/`)

### 📱 UI Layer (リファクタリング重点領域)

現在、`MainActivity` は旧実装の `LogiCloApp` を呼び出していますが、開発の主軸は新実装に移っています。

#### 🏚️ 旧実装 (Legacy) - `ui/logiclo/`
現在稼働しているUIの実装です。リファクタリング完了後に削除される予定です。
*   **`LogiCloApp.kt`**: 旧UIのルートコンポーザブル。現在の `MainActivity` から呼び出されています。
*   **`DashboardScreen.kt`**: 旧ダッシュボード画面。
*   **`LogiCloViewModel.kt`**: 旧実装用の巨大なViewModel。多くのロジックがここに集約されています。
*   **`LogiCloData.kt`**: 旧実装専用のデータモデル定義。

#### 🆕 新実装 (Modern) - 機能別パッケージ
各画面ごとにパッケージを分割し、ViewModelの責務を分散させています。
*   **`ui/dashboard/`**: 新しいダッシュボード画面。
    *   `DashboardScreen.kt`: 新しいメイン画面の実装。
    *   `DashboardViewModel.kt`: 天気や在庫状況に基づく提案ロジックなどを管理。
    *   `model/`: UI状態 (`DashboardUiState`) や提案モデル (`OutfitSuggestion`)。
*   **`ui/closet/`**: クローゼット管理機能。
    *   `ClosetEditorScreen.kt`: 服の追加・編集画面。
    *   `ClosetViewModel.kt`: アイテムのCRUD操作やフィルタリング。
    *   `ClosetOptions.kt`: カテゴリや色の選択肢定義。
*   **`ui/settings/`**: 設定画面。
    *   `SettingsScreen.kt`, `SettingsViewModel.kt`

#### 🧩 共通コンポーネント
*   **`ui/common/`**: アプリ全体で共有するロジックや定数。
    *   `LabelResId.kt`: Enum値と文字列リソースIDの変換ロジック。
    *   `UiMessage.kt`: Snackbar等で表示するメッセージのラッパー。
*   **`ui/components/`**: 再利用可能なUI部品。
    *   `ClothingIllustrationSwatch.kt`: 服のイラストアイコン表示。
*   **`ui/theme/`**: Jetpack Composeのテーマ定義 (Color, Type, Theme)。

---

### 🧠 Domain Layer (`domain/`)

アプリの中核となるルールとデータ構造です。

*   **`model/`**: ドメインモデル。
    *   `ClothingItem.kt`: 服のアイテムデータ（ID, カテゴリ, ステータス等）。
    *   `ClosetModels.kt`: `ClothingCategory`, `LaundryStatus` などのEnum定義。
    *   `WeatherSnapshot.kt`: 天気情報のデータクラス。
*   **`usecase/`**: 単一のビジネスアクション。
    *   `ApplyWearUseCase.kt`: 服を着用した際の状態更新（汚れ度合いの計算など）。
    *   `ComfortRangeDefaults.kt`: カテゴリごとの快適気温の定義。

---

### 💾 Data Layer (`data/`)

データの永続化と外部通信を担当します。

*   **`repository/`**: データの抽象化レイヤー。
    *   `ClosetRepository.kt`: 服データのCRUDインターフェース。
    *   `WeatherRepository.kt`: 天気情報の取得。
    *   `UserPreferencesRepository.kt`: ユーザー設定（DataStore）。
    *   `WearFeedbackRepository.kt`: 着用後のフィードバック管理。
*   **`AppContainer.kt`**: DI（依存性注入）コンテナ。リポジトリのインスタンス生成と管理を行います。
*   **`local/`**: ローカルデータベース (Room) 関連。

---

## 3. リファクタリング・ロードマップ

開発をスムーズに進めるための推奨手順です。

1.  **共通部品の整備**:
    *   `ui/logiclo` 内にある再利用可能なコンポーネントを `ui/components` へ移動・汎用化する。
2.  **ロジックの移行**:
    *   `LogiCloViewModel` (旧) にあるロジックを分析し、適切な `domain/usecase` または各画面のViewModel (`DashboardViewModel` 等) に移動する。
3.  **エントリポイントの切り替え**:
    *   `MainActivity.kt` の `setContent` を `LogiCloApp` (旧) から、新実装のルートコンポーザブル（`LaundryLoopApp` 等として新規作成が必要）に切り替える。
4.  **旧コードの削除**:
    *   `ui/logiclo` パッケージ全体を削除する。
