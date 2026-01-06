# LaundryLoop Closet / LogiClo 実装機能調査（2026-01-06）

## 前提：現状の起動経路（重要）
- アプリ起動時の入口は [app/src/main/java/com/example/myapplication/MainActivity.kt](app/src/main/java/com/example/myapplication/MainActivity.kt) で、現時点では **旧UI**（`ui/logiclo/LogiCloApp`）を `setContent` しています。
- 一方で、新UI（`ui/LaundryLoopApp` など）も実装されていますが、**現状の起動経路では未接続**です。

以降は「(A) 旧UI: 現行起動経路で利用できる機能」「(B) 新UI: 実装済みだが未接続の機能」「(C) 共通基盤」に分けて棚卸しします。

関連資料:
- as-is要件定義書: [docs/requirements_definition_as_is_2026-01-06.md](requirements_definition_as_is_2026-01-06.md)
- as-is詳細設計書: [docs/detail_design_as_is_2026-01-06.md](detail_design_as_is_2026-01-06.md)

---

## (A) 旧UI（現行起動経路）で利用できる機能

### A-1. 画面構成（Bottom Navigation）
- ホーム（Dashboard） / クローゼット / 洗濯 / 設定 の4タブ
  - 実装: [app/src/main/java/com/example/myapplication/ui/logiclo/LogiCloApp.kt](app/src/main/java/com/example/myapplication/ui/logiclo/LogiCloApp.kt)

### A-2. ホーム（Dashboard）
- 天気表示（体感温度・湿度・天気コード）
- 今日/明日切り替え
- TPOモード切替（カジュアル/オフィス）
- 時間帯（短時間/半日/終日、日勤/夕勤/夜勤、など）選択
- 屋外/屋内（屋内の場合は目標室温を設定）
- 場所表示・場所検索（オーバーライドの設定/解除）
- コーデ提案：トップス/ボトムス/アウターを提示し、各パーツ「別候補に変更」
- 「これを着る」操作
  - 着用カウント増加（暑い日は +2、それ以外は +1）
  - 閾値到達で `dirty` 扱いへ移行（UI上は洗濯待ち扱い）
  - SnackBarで結果表示
  - Undo（元に戻す）
  - フィードバック通知用に着用記録（後述）
- フィードバックダイアログ（寒い/普通/暑いの評価、温度帯の調整）
  - 学習：評価に応じて適正温度帯（min/max）を更新

根拠:
- 画面: [app/src/main/java/com/example/myapplication/ui/logiclo/DashboardScreen.kt](app/src/main/java/com/example/myapplication/ui/logiclo/DashboardScreen.kt)
- ロジック: [app/src/main/java/com/example/myapplication/ui/logiclo/LogiCloViewModel.kt](app/src/main/java/com/example/myapplication/ui/logiclo/LogiCloViewModel.kt)

### A-3. クローゼット（アイテム管理）
- クローゼット内アイテム一覧（`dirty` を除外）
- 検索（名前/ブランド/カテゴリキー）
- 絞り込み（複数条件）
  - 種別（TOP/BOTTOM/OUTER）
  - カテゴリ
  - 袖丈
  - 厚さ
  - 色
  - 適正温度帯（min/maxの範囲フィルタ）
- 服の追加（FAB「服を追加」）
- 服の編集（詳細は旧UIのシートUI）
- 服の削除
- 着用カウントを増やす/洗濯へ移動/ステータス切替

根拠:
- 画面: [app/src/main/java/com/example/myapplication/ui/logiclo/ClosetScreen.kt](app/src/main/java/com/example/myapplication/ui/logiclo/ClosetScreen.kt)
- ロジック: [app/src/main/java/com/example/myapplication/ui/logiclo/LogiCloViewModel.kt](app/src/main/java/com/example/myapplication/ui/logiclo/LogiCloViewModel.kt)

### A-4. 洗濯
- タブ：自宅洗い / クリーニング
- 自宅洗い：洗濯待ちアイテム一覧、チェック選択、選択分を「洗濯完了」(status=closet, currentWears=0)
- クリーニング：一覧UIはあるが、旧UI画面ではアクションが `TODO`（コメントで未実装）

根拠:
- 画面: [app/src/main/java/com/example/myapplication/ui/logiclo/LaundryScreen.kt](app/src/main/java/com/example/myapplication/ui/logiclo/LaundryScreen.kt)
- ロジック: [app/src/main/java/com/example/myapplication/ui/logiclo/LogiCloViewModel.kt](app/src/main/java/com/example/myapplication/ui/logiclo/LogiCloViewModel.kt)

### A-5. 設定
- テーマ（ライト/ダーク/システム）
- 「全データをリセット」（実態は“服を全て closet に戻して currentWears を0”）
- デバッグ機能
  - 体感気温オーバーライド
  - 天気コードオーバーライド
  - 通知/フィードバックのデバッグトリガー
  - アラーム/WorkManager スケジュールのデバッグ

根拠:
- 画面: [app/src/main/java/com/example/myapplication/ui/logiclo/SettingsScreen.kt](app/src/main/java/com/example/myapplication/ui/logiclo/SettingsScreen.kt)
- ロジック: [app/src/main/java/com/example/myapplication/ui/logiclo/LogiCloViewModel.kt](app/src/main/java/com/example/myapplication/ui/logiclo/LogiCloViewModel.kt)

---

## (B) 新UI（実装済みだが現状未接続）の機能

### B-1. 新UIのルート/ナビゲーション
- Bottom Navigation + Compose Navigation による画面遷移（Dashboard/Closet/Laundry/Settings）
- クローゼット編集: add/edit のルート（引数付き）
- フィードバック画面へのルート（通知からのdeep link想定）

根拠:
- ルート: [app/src/main/java/com/example/myapplication/ui/LaundryLoopApp.kt](app/src/main/java/com/example/myapplication/ui/LaundryLoopApp.kt)
- 主要Destination: [app/src/main/java/com/example/myapplication/ui/navigation/AppDestination.kt](app/src/main/java/com/example/myapplication/ui/navigation/AppDestination.kt)

### B-2. 新Dashboard（よりリッチな提案/通知/場所選択）
- 在庫・提案・天気更新・Undoなどのイベント設計（SnackBar/通知）
- 位置指定UI（検索 + 手入力 + Map Picker：Google Maps APIが利用可能なら）
- 「着る」アクションの通知（POST_NOTIFICATIONS許可前提）
- デバッグ（天気/時計/フィードバックのdebug UI state）

根拠:
- 画面: [app/src/main/java/com/example/myapplication/ui/dashboard/DashboardScreen.kt](app/src/main/java/com/example/myapplication/ui/dashboard/DashboardScreen.kt)
- ViewModel: [app/src/main/java/com/example/myapplication/ui/dashboard/DashboardViewModel.kt](app/src/main/java/com/example/myapplication/ui/dashboard/DashboardViewModel.kt)

### B-3. 新クローゼット編集
- 名前/ブランド
- 初期ステータス選択（追加時）
- カテゴリ選択、色選択（カラーオプション）
- Always Wash、最大着用回数
- 適正温度帯（RangeSlider）・デフォルトへリセット
- 洗濯タイプ（自宅/クリーニング）
- 袖丈/厚さ
- 画像選択（端末からの取り込み）

根拠:
- 画面: [app/src/main/java/com/example/myapplication/ui/closet/ClosetEditorScreen.kt](app/src/main/java/com/example/myapplication/ui/closet/ClosetEditorScreen.kt)

### B-4. 新洗濯（クリーニング含む）
- 自宅洗い：選択分 or 全部を「洗濯完了」
- クリーニング：
  - `dirty` → 「店に出す」(status=cleaning)
  - `cleaning` → 「受け取る」(status=closet, currentWears=0)

根拠:
- 画面: [app/src/main/java/com/example/myapplication/ui/laundry/LaundryScreen.kt](app/src/main/java/com/example/myapplication/ui/laundry/LaundryScreen.kt)
- ViewModel: [app/src/main/java/com/example/myapplication/ui/laundry/LaundryViewModel.kt](app/src/main/java/com/example/myapplication/ui/laundry/LaundryViewModel.kt)

### B-5. フィードバック（保留フィードバック画面）
- 通知などから `feedback/pending` に遷移
- 対象アイテムの現在の適正温度帯を取得して表示
- トップ/ボトム別の「暑い/普通/寒い」評価を送信

根拠:
- 画面: [app/src/main/java/com/example/myapplication/ui/feedback/PendingFeedbackScreen.kt](app/src/main/java/com/example/myapplication/ui/feedback/PendingFeedbackScreen.kt)

---

## (C) 共通基盤（データ/外部連携/通知）

### C-1. 永続化（Room）
- 服アイテム、ユーザー設定、着用フィードバック履歴をRoomに保存
- 初回はサンプルデータをseed

根拠:
- DI/初期化: [app/src/main/java/com/example/myapplication/data/AppContainer.kt](app/src/main/java/com/example/myapplication/data/AppContainer.kt)
- Entity（服）: [app/src/main/java/com/example/myapplication/data/local/entity/ClothingItemEntity.kt](app/src/main/java/com/example/myapplication/data/local/entity/ClothingItemEntity.kt)
- ドメインモデル（服）: [app/src/main/java/com/example/myapplication/domain/model/ClosetModels.kt](app/src/main/java/com/example/myapplication/domain/model/ClosetModels.kt)

### C-2. 天気（Open-Meteo API）
- Open-Meteoから current/daily/hourly を取得し、WeatherSnapshotへ変換
- 位置オーバーライド（緯度経度）に応じて取得座標を更新し、再fetch

根拠:
- Repo: [app/src/main/java/com/example/myapplication/data/weather/OpenMeteoWeatherRepository.kt](app/src/main/java/com/example/myapplication/data/weather/OpenMeteoWeatherRepository.kt)
- 座標更新/refresh: [app/src/main/java/com/example/myapplication/data/AppContainer.kt](app/src/main/java/com/example/myapplication/data/AppContainer.kt)

### C-3. 位置検索
- BuildConfigにYOLP app idが設定されていればYOLP、なければAndroidのGeocoderを利用

根拠:
- 切替: [app/src/main/java/com/example/myapplication/data/AppContainer.kt](app/src/main/java/com/example/myapplication/data/AppContainer.kt)
- Interface: [app/src/main/java/com/example/myapplication/data/repository/LocationSearchRepository.kt](app/src/main/java/com/example/myapplication/data/repository/LocationSearchRepository.kt)

### C-4. 通知（着用フィードバックのリマインド）
- 21:00に通知を飛ばす（WorkManagerでOneTimeWorkをスケジュール）
- 未回答（pending）のフィードバックがある場合のみ通知
- 通知タップでアプリ起動＋フィードバック画面へ遷移するインテントを生成

根拠:
- スケジューラ: [app/src/main/java/com/example/myapplication/domain/usecase/WearFeedbackReminderScheduler.kt](app/src/main/java/com/example/myapplication/domain/usecase/WearFeedbackReminderScheduler.kt)
- Worker: [app/src/main/java/com/example/myapplication/domain/worker/WearFeedbackReminderWorker.kt](app/src/main/java/com/example/myapplication/domain/worker/WearFeedbackReminderWorker.kt)
- BroadcastReceiver（Alarm→WorkManager）: [app/src/main/java/com/example/myapplication/domain/worker/FeedbackAlarmReceiver.kt](app/src/main/java/com/example/myapplication/domain/worker/FeedbackAlarmReceiver.kt)
- Manifest権限/Receiver: [app/src/main/AndroidManifest.xml](app/src/main/AndroidManifest.xml)

---

## 仕様書とのギャップ（発表で触れると良いポイント）
- 要件書では「Flutter/Firebase」想定ですが、現リポジトリは **ネイティブAndroid（Jetpack Compose + Room + WorkManager）** 実装です。
- 新旧UIが併存しており、現状の起動経路は旧UI。新UIは実装が進んでいるが未接続。

根拠:
- 起動経路: [app/src/main/java/com/example/myapplication/MainActivity.kt](app/src/main/java/com/example/myapplication/MainActivity.kt)
- 要件: [docs/LaundryLoopCloset_requirements_v3.2.txt](docs/LaundryLoopCloset_requirements_v3.2.txt)

---

## 発表用スライド構成案（たたき台）
1. プロダクト概要（狙い：服選びの判断コスト削減/在庫管理）
2. システム構成（端末内：Room、外部：Open-Meteo、通知：WorkManager）
3. 主要画面フロー（Dashboard→Wear→Laundry、Closet CRUD）
4. コーデ提案ロジックの要点（温度適合・TPO・在庫）
5. 着用/洗濯のステートマシン（closet/dirty/cleaning）
6. フィードバック学習（適正温度帯の更新、21時リマインド）
7. 実装状況（旧UI稼働/新UI移行中）と今後の課題
