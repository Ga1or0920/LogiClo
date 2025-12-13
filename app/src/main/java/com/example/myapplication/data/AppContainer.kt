package com.example.myapplication.data

import android.content.Context
import android.content.pm.ApplicationInfo
import com.example.myapplication.data.local.LaundryLoopDatabase
import com.example.myapplication.data.repository.ClosetRepository
import com.example.myapplication.data.repository.InMemoryClosetRepository
import com.example.myapplication.data.repository.InMemoryUserPreferencesRepository
import com.example.myapplication.data.repository.InMemoryWearFeedbackRepository
import com.example.myapplication.data.repository.InMemoryLocationSearchRepository
import com.example.myapplication.data.repository.RoomClosetRepository
import com.example.myapplication.data.repository.RoomUserPreferencesRepository
import com.example.myapplication.data.repository.RoomWearFeedbackRepository
import com.example.myapplication.data.repository.UserPreferencesRepository
import com.example.myapplication.data.repository.WearFeedbackRepository
import com.example.myapplication.data.repository.LocationSearchRepository
import com.example.myapplication.data.repository.GeocoderLocationSearchRepository
import com.example.myapplication.data.sample.SampleData
import com.example.myapplication.data.weather.DebugWeatherRepository
import com.example.myapplication.data.weather.InMemoryWeatherRepository
import com.example.myapplication.data.weather.NoOpWeatherDebugController
import com.example.myapplication.data.weather.OpenMeteoWeatherRepository
import com.example.myapplication.data.weather.PersistentWeatherDebugController
import com.example.myapplication.data.weather.WeatherDebugController
import com.example.myapplication.data.weather.WeatherDebugControllerImpl
import com.example.myapplication.data.weather.WeatherRepository
import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.UserPreferences
import com.example.myapplication.domain.model.WeatherLocationOverride
import com.example.myapplication.domain.model.WeatherSnapshot
import com.example.myapplication.domain.usecase.WearFeedbackReminderScheduler
import com.example.myapplication.util.time.DebugClockController
import com.example.myapplication.util.time.DebugClockControllerImpl
import com.example.myapplication.util.time.InstantCompat
import com.example.myapplication.util.time.NoOpDebugClockController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

interface AppContainer {
    val closetRepository: ClosetRepository
    val userPreferencesRepository: UserPreferencesRepository
    val weatherRepository: WeatherRepository
    val wearFeedbackRepository: WearFeedbackRepository
    val weatherDebugController: WeatherDebugController
    val clockDebugController: DebugClockController
    val locationSearchRepository: LocationSearchRepository
    val syncManager: SyncManager
}

class DefaultAppContainer(
    context: Context,
    private val appScope: CoroutineScope,
    seedWeather: WeatherSnapshot
) : AppContainer {

    private val isDebugBuild: Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private val database: LaundryLoopDatabase = LaundryLoopDatabase.getInstance(context)

    override val closetRepository: ClosetRepository = RoomClosetRepository(database.clothingItemDao())
    override val userPreferencesRepository: UserPreferencesRepository =
        RoomUserPreferencesRepository(database.userPreferencesDao())
    override val wearFeedbackRepository: WearFeedbackRepository =
        RoomWearFeedbackRepository(database.wearFeedbackDao())
    private val openMeteoWeatherRepository = OpenMeteoWeatherRepository(
        coordinates = TOKYO_COORDINATES,
        initialSnapshot = seedWeather
    )
    private val weatherDebugControllerImpl: WeatherDebugController =
        if (isDebugBuild) PersistentWeatherDebugController(context) else NoOpWeatherDebugController
    private val clockDebugControllerImpl: DebugClockController =
        if (isDebugBuild) DebugClockControllerImpl() else NoOpDebugClockController
    override val locationSearchRepository: LocationSearchRepository =
        GeocoderLocationSearchRepository(context)
    override val weatherRepository: WeatherRepository = DebugWeatherRepository(
        delegate = openMeteoWeatherRepository,
        debugController = weatherDebugControllerImpl
    )
    override val weatherDebugController: WeatherDebugController = weatherDebugControllerImpl
    override val clockDebugController: DebugClockController = clockDebugControllerImpl
    private val wearFeedbackReminderScheduler = WearFeedbackReminderScheduler(context)
    override val syncManager: SyncManager = SyncManager(appScope, closetRepository)

    init {
        InstantCompat.registerDebugOffsetProvider(clockDebugControllerImpl::currentOffsetMillis)
        appScope.launch(Dispatchers.IO) {
            closetRepository.seedSampleData()
        }
        appScope.launch(Dispatchers.IO) {
            wearFeedbackRepository.observeLatestPending().collectLatest { entry ->
                wearFeedbackReminderScheduler.updateSchedule(entry)
            }
        }
        appScope.launch(Dispatchers.IO) {
            var lastCoordinates: OpenMeteoWeatherRepository.Coordinates? = null
            userPreferencesRepository.observe().collectLatest { preferences ->
                val override = preferences.weatherLocationOverride
                val targetCoordinates = override?.toCoordinates() ?: TOKYO_COORDINATES
                if (lastCoordinates != targetCoordinates) {
                    openMeteoWeatherRepository.updateCoordinates(targetCoordinates)
                    openMeteoWeatherRepository.refresh()
                    lastCoordinates = targetCoordinates
                }
            }
        }
        appScope.launch(Dispatchers.IO) {
            val thirtyDaysAgo = System.currentTimeMillis() - THIRTY_DAYS_IN_MILLIS
            wearFeedbackRepository.pruneHistory(thirtyDaysAgo)
        }
    }
private fun WeatherLocationOverride.toCoordinates(): OpenMeteoWeatherRepository.Coordinates =
    OpenMeteoWeatherRepository.Coordinates(
        latitude = latitude,
        longitude = longitude
    )
}

private const val THIRTY_DAYS_IN_MILLIS: Long = 30L * 24L * 60L * 60L * 1_000L

private val TOKYO_COORDINATES = OpenMeteoWeatherRepository.Coordinates(
    latitude = 35.6764,
    longitude = 139.6500
)

class InMemoryAppContainer(
    seedClosetItems: List<ClothingItem> = SampleData.closetItems,
    seedPreferences: UserPreferences = SampleData.defaultUserPreferences,
    seedWeather: WeatherSnapshot = SampleData.weather
) : AppContainer {

    private val isDebugBuild: Boolean = true

    override val closetRepository: ClosetRepository = InMemoryClosetRepository(seedClosetItems)
    override val userPreferencesRepository: UserPreferencesRepository =
        InMemoryUserPreferencesRepository(seedPreferences)
    override val wearFeedbackRepository: WearFeedbackRepository = InMemoryWearFeedbackRepository()
    private val inMemoryWeatherRepository = InMemoryWeatherRepository(seedWeather)
    private val weatherDebugControllerImpl: WeatherDebugController =
        if (isDebugBuild) WeatherDebugControllerImpl() else NoOpWeatherDebugController
    private val clockDebugControllerImpl: DebugClockController =
        if (isDebugBuild) DebugClockControllerImpl() else NoOpDebugClockController
    override val locationSearchRepository: LocationSearchRepository = InMemoryLocationSearchRepository()
    override val weatherRepository: WeatherRepository = DebugWeatherRepository(
        delegate = inMemoryWeatherRepository,
        debugController = weatherDebugControllerImpl
    )
    override val weatherDebugController: WeatherDebugController = weatherDebugControllerImpl
    override val clockDebugController: DebugClockController = clockDebugControllerImpl
    override val syncManager: SyncManager = SyncManager(CoroutineScope(SupervisorJob()), closetRepository)

    init {
        InstantCompat.registerDebugOffsetProvider(clockDebugControllerImpl::currentOffsetMillis)
    }
}
