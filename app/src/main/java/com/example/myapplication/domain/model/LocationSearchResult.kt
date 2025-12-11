package com.example.myapplication.domain.model

/**
 * 検索サービスから取得した位置情報の結果。
 */
data class LocationSearchResult(
    val title: String,
    val subtitle: String?,
    val latitude: Double,
    val longitude: Double
)
