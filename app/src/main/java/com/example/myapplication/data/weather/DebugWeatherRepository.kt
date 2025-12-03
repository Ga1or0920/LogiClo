package com.example.myapplication.data.weather

import com.example.myapplication.domain.model.WeatherSnapshot
import com.example.myapplication.util.time.InstantCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Wraps a [WeatherRepository] and applies debug overrides when available.
 */
class DebugWeatherRepository(
    private val delegate: WeatherRepository,
    private val debugController: WeatherDebugController
) : WeatherRepository {

    override fun observeCurrentWeather(): Flow<WeatherSnapshot> {
        return combine(
            delegate.observeCurrentWeather(),
            debugController.override
        ) { snapshot, override ->
            if (override != null) {
                snapshot.copy(
                    minTemperatureCelsius = override.minTemperatureCelsius,
                    maxTemperatureCelsius = override.maxTemperatureCelsius,
                    humidityPercent = override.humidityPercent,
                    updatedAt = InstantCompat.nowOrNull()
                )
            } else {
                snapshot
            }
        }.distinctUntilChanged()
    }

    override suspend fun refresh() {
        delegate.refresh()
    }
}
