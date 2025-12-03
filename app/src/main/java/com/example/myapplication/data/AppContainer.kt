package com.example.myapplication.data

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.room.Room
import com.example.myapplication.data.local.LaundryLoopDatabase
import com.example.myapplication.data.local.entity.toEntity
import com.example.myapplication.data.repository.ClosetRepository
import com.example.myapplication.data.repository.InMemoryClosetRepository
import com.example.myapplication.data.repository.InMemoryUserPreferencesRepository
import com.example.myapplication.data.repository.RoomClosetRepository
import com.example.myapplication.data.repository.RoomUserPreferencesRepository
import com.example.myapplication.data.repository.UserPreferencesRepository
import com.example.myapplication.data.sample.SampleData
import com.example.myapplication.data.weather.DebugWeatherRepository
import com.example.myapplication.data.weather.InMemoryWeatherRepository
import com.example.myapplication.data.weather.NoOpWeatherDebugController
import com.example.myapplication.data.weather.OpenMeteoWeatherRepository
import com.example.myapplication.data.weather.WeatherDebugController
import com.example.myapplication.data.weather.WeatherDebugControllerImpl
import com.example.myapplication.data.weather.WeatherRepository
import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.UserPreferences
import com.example.myapplication.domain.model.WeatherSnapshot
import com.example.myapplication.util.time.DebugClockController
import com.example.myapplication.util.time.DebugClockControllerImpl
import com.example.myapplication.util.time.InstantCompat
import com.example.myapplication.util.time.NoOpDebugClockController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface AppContainer {
    val closetRepository: ClosetRepository
    val userPreferencesRepository: UserPreferencesRepository
    val weatherRepository: WeatherRepository
    val weatherDebugController: WeatherDebugController
    val clockDebugController: DebugClockController
}

class DefaultAppContainer(
    context: Context,
    private val appScope: CoroutineScope,
    seedClosetItems: List<ClothingItem> = SampleData.closetItems,
    seedPreferences: UserPreferences = SampleData.defaultUserPreferences,
    seedWeather: WeatherSnapshot = SampleData.weather
) : AppContainer {

    private val isDebugBuild: Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private val database: LaundryLoopDatabase = Room.databaseBuilder(
        context,
        LaundryLoopDatabase::class.java,
        LaundryLoopDatabase.NAME
    ).addMigrations(
        LaundryLoopDatabase.MIGRATION_1_2,
        LaundryLoopDatabase.MIGRATION_2_3
    )
        .build()

    override val closetRepository: ClosetRepository = RoomClosetRepository(database.clothingItemDao())
    override val userPreferencesRepository: UserPreferencesRepository =
        RoomUserPreferencesRepository(database.userPreferencesDao())
    private val openMeteoWeatherRepository = OpenMeteoWeatherRepository(
        coordinates = TOKYO_COORDINATES,
        initialSnapshot = seedWeather
    )
    private val weatherDebugControllerImpl: WeatherDebugController =
        if (isDebugBuild) WeatherDebugControllerImpl() else NoOpWeatherDebugController
    private val clockDebugControllerImpl: DebugClockController =
        if (isDebugBuild) DebugClockControllerImpl() else NoOpDebugClockController
    override val weatherRepository: WeatherRepository = DebugWeatherRepository(
        delegate = openMeteoWeatherRepository,
        debugController = weatherDebugControllerImpl
    )
    override val weatherDebugController: WeatherDebugController = weatherDebugControllerImpl
    override val clockDebugController: DebugClockController = clockDebugControllerImpl

    init {
        InstantCompat.registerDebugOffsetProvider(clockDebugControllerImpl::currentOffsetMillis)
        appScope.launch(Dispatchers.IO) {
            seedDatabaseIfNeeded(seedClosetItems, seedPreferences)
            openMeteoWeatherRepository.refresh()
        }
    }

    private suspend fun seedDatabaseIfNeeded(
        closetSeed: List<ClothingItem>,
        preferencesSeed: UserPreferences
    ) {
        val clothingDao = database.clothingItemDao()
        if (clothingDao.countItems() == 0) {
            clothingDao.upsertItems(closetSeed.map { it.toEntity() })
        }

        val preferencesDao = database.userPreferencesDao()
        if (preferencesDao.get() == null) {
            preferencesDao.upsert(preferencesSeed.toEntity())
        }
    }
}

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
    private val inMemoryWeatherRepository = InMemoryWeatherRepository(seedWeather)
    private val weatherDebugControllerImpl: WeatherDebugController =
        if (isDebugBuild) WeatherDebugControllerImpl() else NoOpWeatherDebugController
    private val clockDebugControllerImpl: DebugClockController =
        if (isDebugBuild) DebugClockControllerImpl() else NoOpDebugClockController
    override val weatherRepository: WeatherRepository = DebugWeatherRepository(
        delegate = inMemoryWeatherRepository,
        debugController = weatherDebugControllerImpl
    )
    override val weatherDebugController: WeatherDebugController = weatherDebugControllerImpl
    override val clockDebugController: DebugClockController = clockDebugControllerImpl

    init {
        InstantCompat.registerDebugOffsetProvider(clockDebugControllerImpl::currentOffsetMillis)
    }
}
