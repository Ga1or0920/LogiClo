package com.example.myapplication.data.repository

import com.example.myapplication.domain.model.LocationSearchResult

/**
 * 位置名称検索のリポジトリ。
 */
interface LocationSearchRepository {
    suspend fun searchByName(query: String, maxResults: Int = DEFAULT_MAX_RESULTS): List<LocationSearchResult>

    companion object {
        const val DEFAULT_MAX_RESULTS: Int = 8
    }
}
