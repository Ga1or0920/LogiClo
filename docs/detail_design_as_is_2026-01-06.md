# LaundryLoopCloset 詳細設計書（as-is / 現状実装ベース）

作成日: 2026-01-06

## 0. 本書の位置づけ

- 本書は **現状のコード実装（as-is）** を根拠に、アーキテクチャ、データ設計、主要フロー、外部連携、通知設計をまとめた詳細設計書である。
- 現状の起動経路は旧UI（`ui/logiclo`）に接続されている（新UIは未接続領域あり）。

関連資料:
- 要件定義（as-is）: [docs/requirements_definition_as_is_2026-01-06.md](requirements_definition_as_is_2026-01-06.md)
- 実装機能棚卸し: [docs/implemented_features_audit_2026-01-06.md](implemented_features_audit_2026-01-06.md)
- 参考要件（v3.2）: [docs/LaundryLoopCloset_requirements_v3.2.txt](LaundryLoopCloset_requirements_v3.2.txt)

## 1. アーキテクチャ概要

### 1.1 技術スタック

- Android / Kotlin
- UI: Jetpack Compose, Navigation Compose
- 永続化: Room
- 非同期: Kotlin Coroutines / Flow
- 通知: WorkManager（+ AlarmManager Receiver 経路あり）
- 天気: Open-Meteo API（`HttpURLConnection` + `org.json`）
- 位置検索: YOLP（任意）または Geocoder + Open-Meteo Geocoding

根拠:
- Gradle: [app/build.gradle.kts](../app/build.gradle.kts)
- Manifest: [app/src/main/AndroidManifest.xml](../app/src/main/AndroidManifest.xml)

### 1.2 レイヤ構造（概念）

- UI 層
  - Compose Screen（旧UI: `ui/logiclo/*`）
  - ViewModel（旧UI: `ui/logiclo/LogiCloViewModel`）
- Domain 層
  - ドメインモデル（`domain/model/*`）
  - UseCase/スケジューラ（`domain/usecase/*`）
- Data 層
  - Repository interface + 実装（`data/repository/*`）
  - 外部 API リポジトリ（`data/weather/*`、位置検索 repo）
  - Room（`data/local/*`）

### 1.3 起動・依存性注入（DI相当）

- `LaundryLoopApplication` が `DefaultAppContainer` を生成し、Repository 群や DebugController を保持。
- `MainActivity` の `LogiCloViewModel.Factory` へ Container の依存を注入。

根拠:
- [app/src/main/java/com/example/myapplication/LaundryLoopApplication.kt](../app/src/main/java/com/example/myapplication/LaundryLoopApplication.kt)
- [app/src/main/java/com/example/myapplication/data/AppContainer.kt](../app/src/main/java/com/example/myapplication/data/AppContainer.kt)
- [app/src/main/java/com/example/myapplication/MainActivity.kt](../app/src/main/java/com/example/myapplication/MainActivity.kt)

## 2. UI 設計（as-is 起動経路）

### 2.1 画面構成（旧UI）

- 旧UIのルートは `LogiCloApp`。
- `Scaffold + NavigationBar` により 4 画面をボトムナビで切り替える。

画面（route）:
- Dashboard（`dashboard`）
- Closet（`closet`）
- Laundry（`laundry`）
- Settings（`settings`）

根拠:
- [app/src/main/java/com/example/myapplication/ui/logiclo/LogiCloApp.kt](../app/src/main/java/com/example/myapplication/ui/logiclo/LogiCloApp.kt)

### 2.2 状態管理

- 旧UIは `LogiCloViewModel` が `StateFlow<LogiCloUiState>` を公開し、各 Screen が `collectAsState()` で購読する。
- Repository の Flow を ViewModel 内で購読し、UI状態を更新して提案の再計算（`_refreshSuggestion()`）を行う。

根拠:
- [app/src/main/java/com/example/myapplication/ui/logiclo/LogiCloViewModel.kt](../app/src/main/java/com/example/myapplication/ui/logiclo/LogiCloViewModel.kt)

### 2.3 新UI（未接続領域）

- `ui/` 配下に新UIの Screen/ViewModel 群が存在するが、`MainActivity` からは現在呼び出されていない。
- as-is の詳細設計では「参考実装」として扱い、稼働範囲は旧UIに限定する。

## 3. データ設計（Room）

### 3.1 DB概要

- DB: `LaundryLoopDatabase`
- DB名: `laundry_loop.db`
- version: 10

根拠:
- [app/src/main/java/com/example/myapplication/data/local/LaundryLoopDatabase.kt](../app/src/main/java/com/example/myapplication/data/local/LaundryLoopDatabase.kt)

### 3.2 テーブル定義

#### 3.2.1 clothing_items

主キー:
- `id` (TEXT)

主なカラム（as-is）:
- `name` (TEXT)
- `category` (TEXT) … `ClothingCategory.backendValue`
- `type` (TEXT) … `ClothingType.backendValue`
- `sleeveLength` (TEXT)
- `thickness` (TEXT)
- `comfortMinCelsius` (REAL, nullable)
- `comfortMaxCelsius` (REAL, nullable)
- `colorHex` (TEXT)
- `colorGroup` (TEXT)
- `pattern` (TEXT)
- `maxWears` (INTEGER)
- `currentWears` (INTEGER)
- `isAlwaysWash` (INTEGER/BOOLEAN)
- `cleaningType` (TEXT)
- `status` (TEXT) … `LaundryStatus.backendValue`
- `brand` (TEXT, nullable)
- `imageUrl` (TEXT, nullable)
- `lastWornEpochMillis` (INTEGER, nullable)

根拠:
- Entity: [app/src/main/java/com/example/myapplication/data/local/entity/ClothingItemEntity.kt](../app/src/main/java/com/example/myapplication/data/local/entity/ClothingItemEntity.kt)
- DAO: [app/src/main/java/com/example/myapplication/data/local/dao/ClothingItemDao.kt](../app/src/main/java/com/example/myapplication/data/local/dao/ClothingItemDao.kt)

#### 3.2.2 user_preferences

主キー:
- `id` (INTEGER) … シングルトン（0固定）

主なカラム（as-is）:
- `lastLoginEpochMillis` (INTEGER, nullable)
- `lastSelectedMode` (TEXT)
- `lastSelectedEnvironment` (TEXT, default: outdoor)
- `allowBlackNavy` (INTEGER/BOOLEAN)
- `disallowVividPair` (INTEGER/BOOLEAN)
- `defaultMaxWearsJson` (TEXT, default: "{}") … `ClothingCategory -> maxWears` を `;` 区切りで格納
- `indoorTemperatureCelsius` (REAL, nullable)
- `weatherLocationLabel` (TEXT, nullable)
- `weatherLocationLatitude` (REAL, nullable)
- `weatherLocationLongitude` (REAL, nullable)

根拠:
- Entity: [app/src/main/java/com/example/myapplication/data/local/entity/UserPreferencesEntity.kt](../app/src/main/java/com/example/myapplication/data/local/entity/UserPreferencesEntity.kt)
- DAO: [app/src/main/java/com/example/myapplication/data/local/dao/UserPreferencesDao.kt](../app/src/main/java/com/example/myapplication/data/local/dao/UserPreferencesDao.kt)

#### 3.2.3 wear_feedback_entries

主キー:
- `id` (TEXT)

主なカラム（as-is）:
- `wornAtEpochMillis` (INTEGER)
- `topItemId` (TEXT, nullable)
- `bottomItemId` (TEXT, nullable)
- `rating` (TEXT, nullable) … 旧仕様互換用（現在は未使用のケースが多い）
- `topRating` (TEXT, nullable)
- `bottomRating` (TEXT, nullable)
- `notes` (TEXT, nullable)
- `submittedAtEpochMillis` (INTEGER, nullable)

Pending判定:
- `rating == null && topRating == null && bottomRating == null`

根拠:
- Entity: [app/src/main/java/com/example/myapplication/data/local/entity/WearFeedbackEntity.kt](../app/src/main/java/com/example/myapplication/data/local/entity/WearFeedbackEntity.kt)
- DAO: [app/src/main/java/com/example/myapplication/data/local/dao/WearFeedbackDao.kt](../app/src/main/java/com/example/myapplication/data/local/dao/WearFeedbackDao.kt)
- Domain model: [app/src/main/java/com/example/myapplication/domain/model/WearFeedbackModels.kt](../app/src/main/java/com/example/myapplication/domain/model/WearFeedbackModels.kt)

### 3.3 マイグレーション方針

- `LaundryLoopDatabase` 内に `MIGRATION_1_2 ... MIGRATION_9_10` が定義される。
- `wear_feedback_entries` は v4→v5 で追加され、v8→v9 で top/bottom の個別評価カラムが追加。
- `clothing_items` は v9→v10 で `imageUrl`, `lastWornEpochMillis` を「存在しない場合のみ」追加。

根拠:
- [app/src/main/java/com/example/myapplication/data/local/LaundryLoopDatabase.kt](../app/src/main/java/com/example/myapplication/data/local/LaundryLoopDatabase.kt)

## 4. Repository 設計

### 4.1 ClosetRepository（衣類）

- `RoomClosetRepository` は `ClothingItemDao` を利用し、`Entity <-> Domain` 変換して返す。
- 主要I/F:
  - `observeAll()`
  - `observeByStatus(status)`
  - `upsert(item)` / `upsert(items)`
  - `delete(id)`
  - `getItem(id)` / `getItems(ids)`

根拠:
- [app/src/main/java/com/example/myapplication/data/repository/RoomClosetRepository.kt](../app/src/main/java/com/example/myapplication/data/repository/RoomClosetRepository.kt)

### 4.2 UserPreferencesRepository（設定）

- `RoomUserPreferencesRepository` は `UserPreferencesDao` を利用。
- `observe()` は entity が無い場合に `UserPreferences()`（デフォルト）を返す。
- `update(transform)` は「現状態取得→変換→upsert」の形。

根拠:
- [app/src/main/java/com/example/myapplication/data/repository/RoomUserPreferencesRepository.kt](../app/src/main/java/com/example/myapplication/data/repository/RoomUserPreferencesRepository.kt)

### 4.3 WearFeedbackRepository（着用履歴/評価）

- `recordWear(topId, bottomId)` は pending エントリを新規作成（UUID、rating/topRating/bottomRating は null）。
- `observeLatestPending()` は DAO の pending 条件クエリ（latest 1件）を Flow で提供。
- `submitFeedback(entryId, topRating, bottomRating, notes)` は top/bottom 個別評価を更新し、submittedAt を設定。

根拠:
- [app/src/main/java/com/example/myapplication/data/repository/RoomWearFeedbackRepository.kt](../app/src/main/java/com/example/myapplication/data/repository/RoomWearFeedbackRepository.kt)

## 5. 外部連携設計

### 5.1 天気（Open-Meteo）

- `OpenMeteoWeatherRepository` が Open-Meteo Forecast API を呼び出し、`WeatherSnapshot` を構築して `Flow` で提供。
- 取得項目（URL query）:
  - daily: `temperature_2m_max`, `temperature_2m_min`
  - hourly: `relative_humidity_2m`, `apparent_temperature`
  - current: `apparent_temperature`, `relative_humidity_2m`, `temperature_2m`, `weather_code`
- 例外は握りつぶし（ログ警告）で、直前の Snapshot を維持する。

根拠:
- [app/src/main/java/com/example/myapplication/data/weather/OpenMeteoWeatherRepository.kt](../app/src/main/java/com/example/myapplication/data/weather/OpenMeteoWeatherRepository.kt)

### 5.2 位置検索

選択ロジック:
- `BuildConfig.YOLP_APP_ID` が非空 → `YolpLocationSearchRepository`
- それ以外 → `GeocoderLocationSearchRepository`

根拠:
- Container: [app/src/main/java/com/example/myapplication/data/AppContainer.kt](../app/src/main/java/com/example/myapplication/data/AppContainer.kt)

#### 5.2.1 Geocoder + Open-Meteo Geocoding

- 端末に Geocoder が存在する場合は Geocoder を優先。
- Geocoder が使えない/結果が空の場合に Open-Meteo geocoding API へフォールバック。

根拠:
- [app/src/main/java/com/example/myapplication/data/repository/GeocoderLocationSearchRepository.kt](../app/src/main/java/com/example/myapplication/data/repository/GeocoderLocationSearchRepository.kt)

#### 5.2.2 YOLP

- YOLP Local Search API を XML で取得し、簡易 regex で `<Feature>` ブロックから `Name/Address/Coordinates` を抽出。
- `<Coordinates>` は `lon,lat` 想定のため、lat/lon を入れ替えて格納。

根拠:
- [app/src/main/java/com/example/myapplication/data/repository/YolpLocationSearchRepository.kt](../app/src/main/java/com/example/myapplication/data/repository/YolpLocationSearchRepository.kt)

## 6. 通知設計（着用フィードバック）

### 6.1 概要

- pending の `WearFeedbackEntry` が存在する場合、**21:00** に通知で回答を促す。
- 通知は `WearFeedbackReminderWorker` が送信する。

根拠:
- Scheduler: [app/src/main/java/com/example/myapplication/domain/usecase/WearFeedbackReminderScheduler.kt](../app/src/main/java/com/example/myapplication/domain/usecase/WearFeedbackReminderScheduler.kt)
- Worker: [app/src/main/java/com/example/myapplication/domain/worker/WearFeedbackReminderWorker.kt](../app/src/main/java/com/example/myapplication/domain/worker/WearFeedbackReminderWorker.kt)

### 6.2 スケジューリング

- `AppContainer` 初期化で `wearFeedbackRepository.observeLatestPending()` を購読し、変化のたびに `WearFeedbackReminderScheduler.updateSchedule(entry)` を実行。
- `entry == null` または `!entry.isPending` の場合は `cancel()`。
- pending の場合は `OneTimeWorkRequest` を `enqueueUniqueWork(ExistingWorkPolicy.REPLACE)` で登録。
- 遅延は「現在時刻→次の21:00」まで。

根拠:
- Container購読: [app/src/main/java/com/example/myapplication/data/AppContainer.kt](../app/src/main/java/com/example/myapplication/data/AppContainer.kt)
- Scheduler実装: [app/src/main/java/com/example/myapplication/domain/usecase/WearFeedbackReminderScheduler.kt](../app/src/main/java/com/example/myapplication/domain/usecase/WearFeedbackReminderScheduler.kt)

### 6.3 通知の内容と遷移

- 通知本文は top/bottom のアイテム名が取れる場合はそれを含める。
- 通知タップで `MainActivity` を起動し、extra で「フィードバック画面へ誘導する」情報を渡す設計。

根拠:
- Worker内の `createLaunchIntent()` と `MainActivity.EXTRA_*`: [app/src/main/java/com/example/myapplication/domain/worker/WearFeedbackReminderWorker.kt](../app/src/main/java/com/example/myapplication/domain/worker/WearFeedbackReminderWorker.kt)
- Activity extras: [app/src/main/java/com/example/myapplication/MainActivity.kt](../app/src/main/java/com/example/myapplication/MainActivity.kt)

### 6.4 Receiver 経路（補助）

- `FeedbackAlarmReceiver` が AlarmManager から呼ばれる想定で、WorkManager を enqueue する。
- as-is では「Scheduler による OneTimeWork」中心で、Receiver 経路は補助的。

根拠:
- [app/src/main/java/com/example/myapplication/domain/worker/FeedbackAlarmReceiver.kt](../app/src/main/java/com/example/myapplication/domain/worker/FeedbackAlarmReceiver.kt)
- Manifest: [app/src/main/AndroidManifest.xml](../app/src/main/AndroidManifest.xml)

## 7. 主要シーケンス（as-is）

### 7.1 天気更新（位置変更→再取得）

1. 旧UIで位置を検索して選択
2. ViewModel が `UserPreferencesRepository.update { weatherLocationOverride = ... }`
3. `DefaultAppContainer` が `userPreferencesRepository.observe()` を購読しており、座標変更を検知
4. `OpenMeteoWeatherRepository.updateCoordinates()`
5. `OpenMeteoWeatherRepository.refresh()` が Open-Meteo API を呼び出し `WeatherSnapshot` を更新
6. ViewModel が `weatherRepository.observeCurrentWeather()` を購読し、UI状態を更新・再提案

根拠:
- ViewModel位置選択: [app/src/main/java/com/example/myapplication/ui/logiclo/LogiCloViewModel.kt](../app/src/main/java/com/example/myapplication/ui/logiclo/LogiCloViewModel.kt)
- Container天気再取得: [app/src/main/java/com/example/myapplication/data/AppContainer.kt](../app/src/main/java/com/example/myapplication/data/AppContainer.kt)

### 7.2 「着る」→ pending 作成 → 21時通知

1. ユーザーが提案で「着る」を実行
2. ViewModel が衣類の `currentWears` を更新し、条件により `status` を dirty へ遷移
3. 併せて `WearFeedbackRepository.recordWear(topId, bottomId)` を呼び、pending を作成
4. `DefaultAppContainer` が `observeLatestPending()` の変化を検知
5. `WearFeedbackReminderScheduler` が次回 21:00 の Work を REPLACE で登録
6. 21:00 になり Work が実行され、通知が表示される

補足:
- as-is の UI イベント実装は旧UI ViewModel の該当メソッドで行われる。

### 7.3 通知タップ → フィードバック提出

1. 通知タップで `MainActivity` が起動（TaskStackBuilder）
2. extra で「遷移先 destination」「代表アイテムID」が渡される
3. （as-is では）起動後 UI 側で extra を読み取り遷移する実装は、新UI側の destination 設計と整合させる必要がある

※ここは「設計は存在するが、旧UI稼働経路に未統合」の可能性があるため、実機検証が必要。

## 8. 権限・設定

- `INTERNET`
- `POST_NOTIFICATIONS`（Android 13+）
- `SCHEDULE_EXACT_ALARM`（maxSdkVersion=32）
- `USE_EXACT_ALARM`

根拠:
- [app/src/main/AndroidManifest.xml](../app/src/main/AndroidManifest.xml)

## 9. 既知のギャップ / TODO（as-is）

- 旧UI Laundry の「クリーニング」操作は TODO が残っている（新UI側には遷移実装が存在）。
- 通知 extra を旧UIの Nav に反映する統合は未完了の可能性がある。
- v3.2 の Firebase/Firestore 想定は as-is 実装とは異なる。

