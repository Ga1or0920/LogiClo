package com.example.myapplication.data.repository

import com.example.myapplication.domain.model.LocationSearchResult

class InMemoryLocationSearchRepository : LocationSearchRepository {
    private val sample = listOf(
        LocationSearchResult(
            title = "東京駅",
            subtitle = "東京都 千代田区",
            latitude = 35.681236,
            longitude = 139.767125
        ),
        LocationSearchResult(
            title = "大阪駅",
            subtitle = "大阪府 大阪市北区",
            latitude = 34.702485,
            longitude = 135.495951
        ),
        LocationSearchResult(
            title = "ユニバーサル・スタジオ・ジャパン",
            subtitle = "大阪府 大阪市此花区",
            latitude = 34.665442,
            longitude = 135.432338
        )
    )

    override suspend fun searchByName(query: String, maxResults: Int): List<LocationSearchResult> {
        if (query.isBlank()) return emptyList()
        val normalized = query.trim().lowercase()
        return sample.filter { result ->
            result.title.lowercase().contains(normalized) ||
                result.subtitle?.lowercase()?.contains(normalized) == true
        }.take(maxResults)
    }
}
