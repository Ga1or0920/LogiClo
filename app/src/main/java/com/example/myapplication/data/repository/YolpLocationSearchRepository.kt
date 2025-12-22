package com.example.myapplication.data.repository

import com.example.myapplication.domain.model.LocationSearchResult
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Yahoo! Open Local Platform (YOLP) を使った位置名称検索リポジトリの実装（XML解析を簡易実装）。
 *
 * 注意: YOLP のレスポンス形式の変化に備え、シンプルなテキスト/正規表現ベースで座標や名前を抽出します。
 */
class YolpLocationSearchRepository(
    private val appId: String
) : LocationSearchRepository {

    override suspend fun searchByName(query: String, maxResults: Int): List<LocationSearchResult> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (appId.isBlank()) return emptyList()

        return withContext(Dispatchers.IO) {
            val encodedQuery = URLEncoder.encode(trimmed, StandardCharsets.UTF_8.name())
            val encodedAppId = URLEncoder.encode(appId, StandardCharsets.UTF_8.name())
            val url = "https://map.yahooapis.jp/search/local/V1/localSearch" +
                "?appid=$encodedAppId" +
                "&query=$encodedQuery" +
                "&output=xml" +
                "&results=$maxResults"

            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5_000
                readTimeout = 5_000
            }

            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    emptyList()
                } else {
                    val payload = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                    parseXmlResults(payload, trimmed, maxResults)
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun parseXmlResults(payload: String, fallbackQuery: String, maxResults: Int): List<LocationSearchResult> {
        val featureRegex = Regex("<Feature>(.*?)</Feature>", RegexOption.DOT_MATCHES_ALL)
        val nameRegex = Regex("<Name>(.*?)</Name>", RegexOption.DOT_MATCHES_ALL)
        val addressRegex = Regex("<Address>(.*?)</Address>", RegexOption.DOT_MATCHES_ALL)
        val coordsRegex = Regex("<Coordinates>(.*?)</Coordinates>", RegexOption.DOT_MATCHES_ALL)

        val matches = featureRegex.findAll(payload)
        val results = ArrayList<LocationSearchResult>()
        for (m in matches) {
            if (results.size >= maxResults) break
            val block = m.groupValues[1]
            val name = nameRegex.find(block)?.groupValues?.get(1)?.trim() ?: fallbackQuery
            val address = addressRegex.find(block)?.groupValues?.get(1)?.trim()
            val coordsText = coordsRegex.find(block)?.groupValues?.get(1)?.trim()
            val (lat, lon) = parseCoordinates(coordsText) ?: continue
            results += LocationSearchResult(
                title = name,
                subtitle = address,
                latitude = lat,
                longitude = lon
            )
        }
        return results
    }

    private fun parseCoordinates(text: String?): Pair<Double, Double>? {
        if (text.isNullOrBlank()) return null
        // YOLP の <Coordinates> は "lon,lat" 形式で返ることが多い
        val parts = text.split(',').mapNotNull { it.trim().toDoubleOrNull() }
        if (parts.size >= 2) {
            val lon = parts[0]
            val lat = parts[1]
            if (lat.isFinite() && lon.isFinite()) return lat to lon
        }
        return null
    }
}
