package com.mychangli.app.core.network.campustools

import android.util.Log
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class FeatureAvailability(
    val status: String = "OPEN",
    val message: String = "",
    val expectedRestoreAt: Long? = null,
) {
    val isOpen: Boolean get() = status == "OPEN"
}

fun resolveFeatureAvailability(flag: RemoteFeatureFlag?): FeatureAvailability {
    if (flag == null) return FeatureAvailability()
    val normalized = flag.payload.status.uppercase()
    val status = when {
        !flag.enabled -> "CLOSED"
        normalized in setOf("OPEN", "CLOSED", "MAINTENANCE") -> normalized
        else -> "OPEN"
    }
    return FeatureAvailability(
        status = status,
        message = flag.payload.maintenanceNote,
        expectedRestoreAt = flag.payload.expectedRestoreAt,
    )
}

object RemoteConfigurationStore {
    private const val TAG = "RemoteConfiguration"
    private const val STORE_ID = "campus_tools_public_config_v1"
    private const val KEY_BOOTSTRAP = "bootstrap"
    private const val KEY_SERVICE_UPDATES = "service_updates"
    private val gson = Gson()
    private val storage by lazy { MMKV.mmkvWithID(STORE_ID) }
    private val refreshMutex = Mutex()
    @Volatile private var lastSuccessfulRefreshAt = 0L
    private val mutableBootstrap = MutableStateFlow(load())
    private val mutableServiceUpdates = MutableStateFlow(loadServiceUpdates())
    val bootstrap: StateFlow<PublicBootstrap?> = mutableBootstrap.asStateFlow()
    val serviceUpdates: StateFlow<List<RemoteServiceUpdate>> = mutableServiceUpdates.asStateFlow()

    suspend fun refresh(): PublicBootstrap? = refreshIfStale(0L)

    suspend fun refreshIfStale(minIntervalMs: Long = 30_000L): PublicBootstrap? {
        val now = System.currentTimeMillis()
        if (now - lastSuccessfulRefreshAt < minIntervalMs) return mutableBootstrap.value
        return refreshMutex.withLock {
            val lockedNow = System.currentTimeMillis()
            if (lockedNow - lastSuccessfulRefreshAt < minIntervalMs) {
                return@withLock mutableBootstrap.value
            }
            val response = try {
                CampusToolsApiClient.unauthenticated.publicBootstrap()
            } catch (error: Throwable) {
                // The public configuration contains no account or academic data. Keep
                // the last valid cache for users, while leaving a non-sensitive signal
                // that makes local USB/network mapping failures diagnosable.
                Log.w(TAG, "Feature configuration refresh failed (${error.javaClass.simpleName})")
                throw error
            }
            val value = response.data.takeIf { response.ok } ?: return@withLock mutableBootstrap.value
            check(storage.encode(KEY_BOOTSTRAP, gson.toJson(value)))
            mutableBootstrap.value = value
            lastSuccessfulRefreshAt = lockedNow
            Log.d(
                TAG,
                "Feature states refreshed: " + value.flags.joinToString { flag ->
                    "${flag.key}=${resolveFeatureAvailability(flag).status}"
                },
            )
            value
        }
    }

    fun feature(key: String): FeatureAvailability {
        val flag = mutableBootstrap.value?.flags?.firstOrNull { it.key == key }
        return resolveFeatureAvailability(flag)
    }

    suspend fun refreshServiceUpdates(): List<RemoteServiceUpdate> {
        val response = try {
            CampusToolsApiClient.unauthenticated.serviceUpdates()
        } catch (error: Throwable) {
            Log.w(TAG, "Service updates refresh failed (${error.javaClass.simpleName})")
            throw error
        }
        val value = response.data.takeIf { response.ok } ?: return mutableServiceUpdates.value
        check(storage.encode(KEY_SERVICE_UPDATES, gson.toJson(value)))
        mutableServiceUpdates.value = value
        return value
    }

    private fun load(): PublicBootstrap? = runCatching {
        gson.fromJson(storage.decodeString(KEY_BOOTSTRAP), PublicBootstrap::class.java)
    }.getOrNull()

    private fun loadServiceUpdates(): List<RemoteServiceUpdate> = runCatching {
        val encoded = storage.decodeString(KEY_SERVICE_UPDATES)
        if (encoded.isNullOrBlank()) {
            emptyList()
        } else {
            gson.fromJson(
                encoded,
                Array<RemoteServiceUpdate>::class.java,
            ).toList()
        }
    }.getOrDefault(emptyList())
}
