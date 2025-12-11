package com.example.myapplication.data.repository

import android.content.Context
import android.location.Geocoder
import com.example.myapplication.domain.model.LocationSearchResult
import java.io.IOException
import java.util.Locale
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.min

class GeocoderLocationSearchRepository(
    context: Context,
    private val locale: Locale = Locale.getDefault()
) : LocationSearchRepository {

    private val geocoder: Geocoder? =
        if (Geocoder.isPresent()) Geocoder(context, locale) else null

    override suspend fun searchByName(query: String, maxResults: Int): List<LocationSearchResult> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        return withContext(Dispatchers.IO) {
            val localResults = searchWithGeocoder(trimmed, maxResults)
            when {
                localResults == null -> searchWithRemote(trimmed, maxResults)
                localResults.isNotEmpty() -> localResults
                else -> searchWithRemote(trimmed, maxResults)
            }
        }
    }

    private fun searchWithGeocoder(query: String, maxResults: Int): List<LocationSearchResult>? {
        val gc = geocoder ?: return null
        return runCatching {
            @Suppress("DEPRECATION")
            gc.getFromLocationName(query, maxResults) ?: emptyList()
        }.fold(
            onSuccess = { addresses ->
                addresses.mapNotNull { address ->
                    val lat = address.latitude
                    val lon = address.longitude
                    if (!lat.isFinite() || !lon.isFinite()) return@mapNotNull null
                    val title =
                        address.featureName ?: address.locality ?: address.adminArea ?: query
                    val subtitle = buildGeocoderSubtitle(address)
                    LocationSearchResult(
                        title = title,
                        subtitle = subtitle,
                        latitude = lat,
                        longitude = lon
                    )
                }
            },
            onFailure = { throwable ->
                when (throwable) {
                    is IOException -> null
                    else -> throw throwable
                }
            }
        )
    }

    private fun searchWithRemote(query: String, maxResults: Int): List<LocationSearchResult> {
        val language = locale.language.takeIf { it.isNotBlank() } ?: "en"
        val encodedName = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
        val url = "https://geocoding-api.open-meteo.com/v1/search" +
            "?name=$encodedName" +
            "&count=$maxResults" +
            "&language=$language" +
            "&format=json"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 5_000
        }
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                emptyList()
            } else {
                val payload = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    reader.readText()
                }
                parseRemoteResults(payload, query, maxResults)
            }
        } catch (io: IOException) {
            throw io
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRemoteResults(
        payload: String,
        query: String,
        maxResults: Int
    ): List<LocationSearchResult> {
        val json = JSONObject(payload)
        val resultsArray = json.optJSONArray("results") ?: return emptyList()
        val limit = min(resultsArray.length(), maxResults)
        if (limit <= 0) return emptyList()
        val results = ArrayList<LocationSearchResult>(limit)
        for (index in 0 until limit) {
            val item = resultsArray.optJSONObject(index) ?: continue
            val latitude = item.optDouble("latitude", Double.NaN)
            val longitude = item.optDouble("longitude", Double.NaN)
            if (!latitude.isFinite() || !longitude.isFinite()) continue
            val title = item.optString("name").takeIf { it.isNotBlank() } ?: query
            val subtitle = buildRemoteSubtitle(item)
            results += LocationSearchResult(
                title = title,
                subtitle = subtitle,
                latitude = latitude,
                longitude = longitude
            )
        }
        return results
    }

    private fun buildGeocoderSubtitle(address: android.location.Address): String? {
        val builder = mutableListOf<String>()
        val admin = address.adminArea?.takeIf { it.isNotBlank() }
        val locality = address.locality?.takeIf { it.isNotBlank() }
        val subLocality = address.subLocality?.takeIf { it.isNotBlank() }
        val thoroughfare = address.thoroughfare?.takeIf { it.isNotBlank() }
        if (admin != null) builder += admin
        if (locality != null && locality != admin) builder += locality
        if (subLocality != null && subLocality != locality) builder += subLocality
        if (thoroughfare != null) builder += thoroughfare
        return when {
            builder.isEmpty() -> address.getAddressLine(0)
            else -> builder.joinToString(separator = " ")
        }
    }

    private fun buildRemoteSubtitle(json: JSONObject): String? {
        val admin1 = json.optString("admin1").takeIf { it.isNotBlank() }
        val admin2 = json.optString("admin2").takeIf { it.isNotBlank() }
        val country = json.optString("country").takeIf { it.isNotBlank() }
        val parts = listOfNotNull(admin2, admin1, country)
        return if (parts.isEmpty()) null else parts.joinToString(separator = " ")
    }
}

private fun Double.isFinite(): Boolean = !isNaN() && !isInfinite()
