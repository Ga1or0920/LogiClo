package com.example.myapplication.ui.dashboard

import com.example.myapplication.domain.model.CasualForecastDay
import com.example.myapplication.domain.model.CasualForecastSegment
import com.example.myapplication.domain.model.CasualForecastSegmentSummary
import com.example.myapplication.domain.model.WeatherSnapshot

data class CasualForecastSelection(
    val day: CasualForecastDay = CasualForecastDay.TODAY,
    val segment: CasualForecastSegment = CasualForecastSegment.MORNING
)

data class CasualForecastComputation(
    val uiState: com.example.myapplication.ui.dashboard.model.CasualForecastUiState? = null,
    val resolvedSelection: CasualForecastSelection? = null
)

class CasualForecastEngine {
    fun compute(weather: WeatherSnapshot, selection: CasualForecastSelection): CasualForecastComputation {
        val summaries = weather.casualSegmentSummaries
        if (summaries.isEmpty()) return CasualForecastComputation()
        val groupedByDay = summaries.groupBy { it.day }
        val dayOptions = groupedByDay.keys.sortedBy { it.ordinal }
        if (dayOptions.isEmpty()) return CasualForecastComputation()
        val resolvedDay = selection.day.takeIf { dayOptions.contains(it) } ?: dayOptions.first()
        val summariesForDay = groupedByDay[resolvedDay].orEmpty()
        if (summariesForDay.isEmpty()) return CasualForecastComputation()
        val availableSegmentsForDay = summariesForDay.map { it.segment }.distinct().sortedBy { it.ordinal }
        val resolvedSegment = selection.segment.takeIf { availableSegmentsForDay.contains(it) }
            ?: availableSegmentsForDay.firstOrNull()
            ?: return CasualForecastComputation()
        val summaryForSelection = summariesForDay.firstOrNull { it.segment == resolvedSegment } ?: return CasualForecastComputation()
        val availableSegmentsOverall = CasualForecastSegment.values().filter { segment -> summaries.any { it.segment == segment } }
        if (availableSegmentsOverall.isEmpty()) return CasualForecastComputation()
        val segmentOptions = availableSegmentsOverall.map { segment ->
            com.example.myapplication.ui.dashboard.model.CasualForecastSegmentOption(
                segment = segment,
                isEnabled = availableSegmentsForDay.contains(segment)
            )
        }
        val summary = summaryForSelection.toUiSummary()
        val uiState = com.example.myapplication.ui.dashboard.model.CasualForecastUiState(
            dayOptions = dayOptions,
            segmentOptions = segmentOptions,
            selectedDay = resolvedDay,
            selectedSegment = resolvedSegment,
            summary = summary
        )
        return CasualForecastComputation(uiState = uiState, resolvedSelection = CasualForecastSelection(resolvedDay, resolvedSegment))
    }

    private fun CasualForecastSegmentSummary.toUiSummary(): com.example.myapplication.ui.dashboard.model.CasualForecastSummary {
        return com.example.myapplication.ui.dashboard.model.CasualForecastSummary(
            minTemperatureCelsius = minTemperatureCelsius,
            maxTemperatureCelsius = maxTemperatureCelsius,
            averageApparentTemperatureCelsius = averageApparentTemperatureCelsius
        )
    }
}
