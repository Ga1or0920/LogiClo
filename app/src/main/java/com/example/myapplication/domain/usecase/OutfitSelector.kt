package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.model.*
import com.example.myapplication.ui.logiclo.AppMode
import com.example.myapplication.ui.logiclo.EnvMode

/**
 * TPO対応型ハイブリッドアルゴリズムによる服の選定ロジック
 */
class OutfitSelector {

    /**
     * Formality enumをTPOスコア(0-100)に変換
     */
    private fun Formality?.toTpoScore(): Int = when(this) {
        Formality.FORMAL -> 90
        Formality.SEMI_FORMAL -> 70
        Formality.SOMEWHAT_CASUAL -> 50
        Formality.CASUAL -> 30
        Formality.STANDARD -> 40
        else -> 50  // UNKNOWN or null
    }

    data class SuggestionResult(
        val top: ClothingItem?,
        val bottom: ClothingItem?,
        val outer: ClothingItem?,
        val formalityLabel: String,
        val topTempDiff: Double?,
        val bottomTempDiff: Double?,
        val outerTempDiff: Double?
    )

    /**
     * メインの選定メソッド
     * 
     * @param allClothing 全ての服データ
     * @param weatherSnapshot 天気データ
     * @param tpoMode アプリモード（CASUAL / OFFICE）
     * @param envMode 環境モード（INDOOR / OUTDOOR / SHORT_TRIP）
     * @param timeId 時間帯ID (LogiCloViewModelから渡される)
     * @param effectiveTempForTimeSlot 時間帯別の体感温度算出関数（ViewModelから注入）
     */
    fun selectOutfit(
        allClothing: List<ClothingItem>,
        weatherSnapshot: WeatherSnapshot?,
        tpoMode: AppMode,
        envMode: EnvMode,
        timeId: String,
        effectiveTempForTimeSlot: (WeatherSnapshot?, Boolean, String) -> Double,
        isTomorrow: Boolean
    ): SuggestionResult {

        if (weatherSnapshot == null) {
            return SuggestionResult(null, null, null, "データ不足", null, null, null)
        }

        // =====================================================================
        // Step 1: 足切りフィルタリング (Filtering)
        // =====================================================================
        val cleanItems = allClothing.filter { it.status == LaundryStatus.CLOSET }

        val tpoFilteredItems = cleanItems.filter { item ->
            val score = item.formality.toTpoScore()
            when (tpoMode) {
                AppMode.CASUAL -> score <= 60  // 休日: Relax, Casual, Smart Casual
                AppMode.OFFICE -> score >= 50  // 仕事: Smart Casual, Formal
            }
        }

        // 雨対策: 降水確率50%以上の場合、白・ベージュ系のボトムスを除外
        val precipChance = weatherSnapshot.precipitationProbability ?: 0
        val rainFilteredItems = tpoFilteredItems.filter { item ->
            if (precipChance >= 50 && item.type == ClothingType.BOTTOM) {
                val isLightColor = item.colorGroup == ColorGroup.MONOTONE && item.colorHex.uppercase() == "#FFFFFF" || // 白 (簡易判定)
                                   item.colorGroup == ColorGroup.EARTH_TONE // ベージュ含む
                !isLightColor
            } else {
                true
            }
        }

        // タイプ別に分類
        val tops = rainFilteredItems.filter { it.type == ClothingType.TOP }
        val bottoms = rainFilteredItems.filter { it.type == ClothingType.BOTTOM }
        val outers = rainFilteredItems.filter { it.type == ClothingType.OUTER }

        // =====================================================================
        // Step 2: 二層温度基準の決定 (Dual-Layer Temperature)
        // =====================================================================
        val outerBaseTemp = effectiveTempForTimeSlot(weatherSnapshot, isTomorrow, timeId)
        
        // インナー用温度: 屋内の場合は空調を考慮、屋外の場合は外気温
        val targetTempInner = if (envMode == EnvMode.INDOOR) {
             if (outerBaseTemp < 20.0) 22.0 else 26.0
        } else {
            outerBaseTemp
        }

        // アウター用温度: 常に外気温
        val targetTempOuter = outerBaseTemp

        // =====================================================================
        // Step 3: 温度適合度スコアリング (Scoring)
        // =====================================================================
        
        fun calculateTempDiff(item: ClothingItem, targetTemp: Double): Double {
            val min = item.comfortMinCelsius ?: return Double.MAX_VALUE // 温度範囲未設定は最低優先度
            val max = item.comfortMaxCelsius ?: return Double.MAX_VALUE // 温度範囲未設定は最低優先度
            // 範囲内なら0、範囲外なら絶対差分
            return when {
                targetTemp < min -> min - targetTemp // 寒すぎる (絶対値)
                targetTemp > max -> targetTemp - max // 暑すぎる (絶対値)
                else -> 0.0 // 範囲内 - 最高優先度
            }
        }

        // 温度差が小さい順にソートして上位候補を取得
        val topCandidates = tops.sortedBy { calculateTempDiff(it, targetTempInner) }.take(5)
        val bottomCandidates = bottoms.sortedBy { calculateTempDiff(it, targetTempInner) }.take(5)

        // =====================================================================
        // Step 4: アウターの選定 (Outer Selection)
        // =====================================================================
        
        // 特例: 短時間外出かつ外気温15℃以上ならアウターなし
        // 注: EnvModeにSHORT_TRIPがないため、ViewModelのロジック(timeId="spot")を模倣する必要がありますが、
        // ここではEnvMode引数がINDOOR/OUTDOORのみと仮定し、呼び出し側で制御するか、
        // ViewModelと同じ条件 (timeId == "spot") を使用します。
        val isShortTripWarm = (timeId == "spot") && (targetTempOuter >= 15.0)
        
        val selectedOuter = if (isShortTripWarm) {
            null
        } else {
            outers.sortedBy { calculateTempDiff(it, targetTempOuter) }.firstOrNull()
        }

        // =====================================================================
        // Step 5: 組み合わせと最終決定 (Final Selection)
        // =====================================================================
        
        var bestCombination: Pair<ClothingItem, ClothingItem>? = null

        // 候補の組み合わせを総当たり
        val validCombinations = mutableListOf<Pair<ClothingItem, ClothingItem>>()

        for (top in topCandidates) {
            for (bottom in bottomCandidates) {
                // NG組み合わせ排除
                // 1. 同色NG (セットアップ以外)
                // Note: Domain ModelのColorGroupはEnumなので直接比較
                val isSameColor = top.colorGroup == bottom.colorGroup
                val isSetupAllowed = top.colorGroup == ColorGroup.MONOTONE || top.colorGroup == ColorGroup.NAVY_BLUE // 黒・紺はOK
                
                if (isSameColor && !isSetupAllowed) continue

                // 2. 派手×派手NG
                if (top.colorGroup == ColorGroup.VIVID && bottom.colorGroup == ColorGroup.VIVID) continue

                validCombinations.add(top to bottom)
            }
        }

        // 在庫回転率（着用頻度）の考慮: Topsの最終着用日が古い順にソート
        // lastWornDateがnull（未着用）のものを最優先
        validCombinations.sortWith(compareBy<Pair<ClothingItem, ClothingItem>> { (top, _) ->
            top.lastWornDate ?: java.time.Instant.MIN // nullなら最古扱い
        }.thenBy { (_, bottom) ->
            bottom.lastWornDate ?: java.time.Instant.MIN
        })

        val (finalTop, finalBottom) = validCombinations.firstOrNull() ?: (topCandidates.firstOrNull() to bottomCandidates.firstOrNull())

        // =====================================================================
        // Result Construction
        // =====================================================================
        
        val topDiff = finalTop?.let { calculateTempDiff(it, targetTempInner) }
        val bottomDiff = finalBottom?.let { calculateTempDiff(it, targetTempInner) }
        val outerDiff = selectedOuter?.let { calculateTempDiff(it, targetTempOuter) }

        val formality = calculateFormality(listOfNotNull(finalTop, finalBottom, selectedOuter))

        return SuggestionResult(
            top = finalTop,
            bottom = finalBottom,
            outer = selectedOuter,
            formalityLabel = formality,
            topTempDiff = topDiff,
            bottomTempDiff = bottomDiff,
            outerTempDiff = outerDiff
        )
    }

    /**
     * コーデ全体の平均TPOスコアからフォーマル度を言語化
     */
    private fun calculateFormality(items: List<ClothingItem>): String {
        if (items.isEmpty()) return "---"

        val avgScore = items.map { it.formality.toTpoScore() }.average()

        return when {
            avgScore >= 80 -> "フォーマル (仕事・会食)"
            avgScore >= 60 -> "オフィスカジュアル (通勤・デート)"
            avgScore >= 40 -> "スマートカジュアル (街歩き)"
            avgScore >= 20 -> "カジュアル (リラックス)"
            else -> "ラフ (部屋着・近所)"
        }
    }
}
