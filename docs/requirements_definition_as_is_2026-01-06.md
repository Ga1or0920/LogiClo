# LaundryLoopCloset 要件定義書（as-is / 現状実装ベース）

作成日: 2026-01-06

## 0. 本書の位置づけ

- 本書は **現状のコード実装（as-is）** を根拠に、機能要件・非機能要件・制約・外部連携を整理した要件定義書である。
- 既存の要件資料（[docs/LaundryLoopCloset_requirements_v3.2.txt](LaundryLoopCloset_requirements_v3.2.txt)）には Flutter/Firebase 前提の記述が含まれるが、現状実装は **Android ネイティブ（Kotlin / Jetpack Compose）+ Room** を中心としている。
- 現状の起動経路は旧UI（`ui/logiclo`）に接続されており、新UI（`ui/` 配下の新構成）は実装済み領域があるものの **未接続** の箇所がある。

## 1. 対象システム概要

### 1.1 システム名称
- LaundryLoopCloset（LogiClo）

### 1.2 目的（as-is）
- クローゼット（衣類）を登録し、天気・状況に応じた服装提案を行う。
- 着用による「汚れ/洗濯」管理（着用回数による dirty 化、洗濯/クリーニングで closet へ戻す）を支援する。
- 着用後のフィードバック（暑い/寒いなど）を蓄積し、快適温度帯（comfortMin/Max）を調整して提案精度を改善する。

### 1.3 利用者
- 単一端末の単一ユーザーを想定（ログイン/アカウント管理は現状なし）

### 1.4 対象プラットフォーム
- Android
- minSdk: 24
- targetSdk: 36
- 実装言語: Kotlin
- UI: Jetpack Compose

根拠: [app/build.gradle.kts](../app/build.gradle.kts)

## 2. スコープ

### 2.1 スコープ内（現状動作＝起動経路に接続済み）
- 旧UI（`ui/logiclo`）の 4 画面（Dashboard / Closet / Laundry / Settings）
- ローカル永続化（Room）
- 天気取得（Open-Meteo）
- 位置検索（YOLP_APP_ID がある場合は YOLP、ない場合は Geocoder）
- 21:00 リマインド通知（WorkManager）

### 2.2 スコープ外（as-is 要件には含めるが、稼働経路に未接続）
- 新UI（`ui/` 配下）の分割 ViewModel/Screen 構成
  - 例: 新 Laundry 画面のクリーニング遷移、ClosetEditor、PendingFeedback など

※「未接続」＝アプリ起動（MainActivity）から遷移できない、または実行フローに載っていない状態。

## 3. 前提・制約

### 3.1 ネットワーク
- 天気取得のためにインターネット接続が必要。
- ネットワークが無い場合、天気取得は失敗しうる（提案が限定的/既存値のまま等）。

権限根拠: [app/src/main/AndroidManifest.xml](../app/src/main/AndroidManifest.xml)

### 3.2 通知
- Android 13+ では `POST_NOTIFICATIONS` が必要。
- 通知は「着用フィードバックの回答」を促すリマインド。

権限根拠: [app/src/main/AndroidManifest.xml](../app/src/main/AndroidManifest.xml)

### 3.3 アラーム
- `SCHEDULE_EXACT_ALARM`（maxSdkVersion=32）および `USE_EXACT_ALARM` を宣言。
- 実装としては Alarm → Receiver → WorkManager の起動経路が存在。

## 4. 用語

- **クローゼット**: 登録した衣類の集合。
- **衣類アイテム**: `ClothingItem`。
- **状態（LaundryStatus）**: `closet / dirty / cleaning`。
- **着用フィードバック**: 着用後の評価情報（トップ/ボトム個別評価、メモ、提出時刻）。
- **モード（TPO）**: `casual / office`。
- **環境**: `indoor / outdoor`。

根拠: domain model
- [app/src/main/java/com/example/myapplication/domain/model/ClosetModels.kt](../app/src/main/java/com/example/myapplication/domain/model/ClosetModels.kt)
- [app/src/main/java/com/example/myapplication/domain/model/UserPreferences.kt](../app/src/main/java/com/example/myapplication/domain/model/UserPreferences.kt)

## 5. 機能要件

本章は「ユーザー視点の要求」を記述し、実装根拠は付録/別資料にまとめる。

### 5.1 クローゼット管理

#### 5.1.1 衣類登録
- 衣類アイテムを登録できる。
- 登録できる主な属性（as-is）
  - 名称
  - カテゴリ（例: t_shirt, slacks 等）
  - 種別（top/bottom/outer/inner）
  - 袖丈、厚み
  - 快適温度帯（min/max, 任意）
  - 色（hex）と色グループ
  - 柄
  - 最大着用回数（maxWears）
  - 現在着用回数（currentWears）
  - 「常に洗濯」フラグ
  - クリーニング種別（home/dry）
  - 状態（closet/dirty/cleaning）
  - ブランド（任意）
  - 画像URL（任意）
  - 最終着用日（任意）

#### 5.1.2 衣類編集/削除
- 登録済みアイテムを編集できる。
- アイテムを削除できる。

#### 5.1.3 一覧/検索/絞り込み
- クローゼットの一覧を表示できる。
- 一覧上で検索や絞り込みができる（旧UIに実装）。

### 5.2 服装提案（Dashboard）

#### 5.2.1 天気取得
- 指定位置の天気（最低/最高/現在など）を取得する。

#### 5.2.2 位置指定
- 天気取得に使う位置を検索/指定できる。
- `YOLP_APP_ID` が設定されている場合は YOLP を用い、未設定の場合は Geocoder を用いる。

#### 5.2.3 提案生成
- クローゼットのアイテムから、トップ/ボトム等の組み合わせを提案として提示する。
- 提案は以下の影響を受ける（as-is）
  - TPO モード（casual/office）
  - 環境（indoor/outdoor）
  - 天気（気温）
  - 色ルール（黒紺許容、派手同士非許容）
  - アイテムの快適温度帯（存在する場合）

#### 5.2.4 「着る」アクション
- 提案された組み合わせを「着用した」として記録できる。
- 着用回数を加算し、条件により dirty 化できる。
- 直前の着用操作を Undo できる。

### 5.3 洗濯/クリーニング（Laundry）

#### 5.3.1 汚れアイテムの確認
- dirty 状態のアイテムを確認できる。

#### 5.3.2 自宅洗い
- 選択したアイテムを「洗濯した」として closet に戻す。

#### 5.3.3 クリーニング
- cleaning 状態へ遷移する操作は旧UIでは未実装（TODO）である。
- 新UI側にはステータス遷移が実装されている。

### 5.4 フィードバック（学習）

#### 5.4.1 着用履歴の記録
- 「着る」操作時に着用履歴（pending feedback）を作成できる。

#### 5.4.2 リマインド通知
- 21:00 に、未提出のフィードバックが存在する場合、通知を表示できる。
- 通知タップでアプリを起動し、フィードバック画面へ誘導できる。

#### 5.4.3 フィードバック提出
- トップ/ボトムの評価を提出できる。
- 提出結果に応じて、各アイテムの快適温度帯を更新できる（旧UI実装）。

### 5.5 設定（Settings）
- テーマ切替等の設定を変更できる。
- データリセット（全削除）を実行できる。
- デベロッパー向け操作（デバッグ）を利用できる（実装あり）。

## 6. データ要件（永続化）

### 6.1 保存方式
- ローカルDB（Room）を使用する。
- DB 名: `laundry_loop.db`

根拠: [app/src/main/java/com/example/myapplication/data/local/LaundryLoopDatabase.kt](../app/src/main/java/com/example/myapplication/data/local/LaundryLoopDatabase.kt)

### 6.2 テーブル（概要）
- `clothing_items`
- `user_preferences`（シングルトン: id=0）
- `wear_feedback_entries`

## 7. 外部連携要件

### 7.1 天気（Open-Meteo）
- Open-Meteo API を利用して天気情報を取得する。

### 7.2 位置検索
- `YOLP_APP_ID` があれば YOLP を用いる。
- それ以外は Android の Geocoder を用いる。

根拠（BuildConfig）: [app/build.gradle.kts](../app/build.gradle.kts)

### 7.3 Google Maps
- 依存関係として Google Maps が含まれる（新UIの Map Picker で利用想定）。
- as-is の起動経路では必須とは限らない（未接続領域があるため）。

## 8. 非機能要件（as-is で明文化）

### 8.1 性能
- クローゼット一覧はローカルDBから読み出し、UI は Flow/Compose で更新される。
- 大量データ時の性能要件（上限件数など）は現状未定義。

### 8.2 可用性/オフライン
- クローゼット・設定・履歴はオフラインでも利用可能（ローカルDB）。
- 天気・位置検索はオフラインでは制限される。

### 8.3 セキュリティ/プライバシー
- サーバ送信やアカウント機構は現状なし。
- 位置情報は「天気取得のための座標」として保存される（ユーザー設定）。

### 8.4 バックアップ
- `android:allowBackup=true` で OS バックアップ対象になりうる（端末設定に依存）。

根拠: [app/src/main/AndroidManifest.xml](../app/src/main/AndroidManifest.xml)

## 9. 既存要件(v3.2)との差分（要点）

- as-is 実装は Firebase/Firestore ではなく Room を使用している。
- 旧UIが稼働経路で、新UIは未接続の領域がある。
- それでも v3.2 の主要ドメイン（Closet/Dashboard/Laundry/通知/天気）は概ね実装されているが、粒度や実現方式が異なる。

## 10. 関連資料

- 実装機能棚卸し: [docs/implemented_features_audit_2026-01-06.md](implemented_features_audit_2026-01-06.md)
- 詳細設計（as-is）: [docs/detail_design_as_is_2026-01-06.md](detail_design_as_is_2026-01-06.md)
- 要件資料（参考）: [docs/LaundryLoopCloset_requirements_v3.2.txt](LaundryLoopCloset_requirements_v3.2.txt)
