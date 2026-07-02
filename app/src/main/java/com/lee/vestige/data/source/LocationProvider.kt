package com.lee.vestige.data.source

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Returns a recent coarse location for weather, or `null` if permission is missing or
 * no sufficiently fresh fix exists. Android 11+ requests a current fix first; older
 * devices and timeouts fall back to recent cached locations.
 */
class LocationProvider(private val context: Context) {

    private val locationMutex = Mutex()
    private var lastCurrentLocation: Location? = null
    private var lastCurrentRequestAt: Long = 0L

    @SuppressLint("MissingPermission") // permission is checked in hasPermission()
    suspend fun locationForWeather(): Pair<Double, Double>? =
        resolveLocation(MAX_WEATHER_CACHE_AGE_MS)

    /** A stricter current-location lookup for the location written into a new note. */
    suspend fun locationForNote(): Pair<Double, Double>? =
        resolveLocation(MAX_NOTE_CACHE_AGE_MS)

    @SuppressLint("MissingPermission")
    private suspend fun resolveLocation(maxCacheAgeMs: Long): Pair<Double, Double>? =
        withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext null

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext null

        val location = locationMutex.withLock {
            val now = System.currentTimeMillis()
            val sharedCurrent = lastCurrentLocation
                ?.takeIf { now - lastCurrentRequestAt <= SHARED_LOCATION_AGE_MS }
            if (sharedCurrent != null) return@withLock sharedCurrent

            val current = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requestCurrentLocation(lm)
            } else {
                null
            }
            if (current != null) {
                lastCurrentLocation = current
                lastCurrentRequestAt = System.currentTimeMillis()
                current
            } else {
                newestRecentLocation(lm, maxCacheAgeMs)
            }
        }
        location?.let { it.latitude to it.longitude }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestCurrentLocation(lm: LocationManager): Location? {
        val enabledProviders = lm.getProviders(true)
        val provider = PREFERRED_PROVIDERS.firstOrNull { it in enabledProviders }
            ?: enabledProviders.firstOrNull()
            ?: return null

        return withTimeoutOrNull(CURRENT_LOCATION_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val cancellationSignal = CancellationSignal()
                continuation.invokeOnCancellation { cancellationSignal.cancel() }
                runCatching {
                    lm.getCurrentLocation(
                        provider,
                        cancellationSignal,
                        context.mainExecutor,
                    ) { location ->
                        if (continuation.isActive) continuation.resume(location)
                    }
                }.onFailure {
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun newestRecentLocation(lm: LocationManager, maxAgeMs: Long): Location? {
        val cutoff = System.currentTimeMillis() - maxAgeMs
        return lm.getProviders(true)
            .mapNotNull { provider ->
                runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
            }
            .filter { it.time >= cutoff }
            .maxByOrNull { it.time }
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val CURRENT_LOCATION_TIMEOUT_MS = 8_000L
        private const val SHARED_LOCATION_AGE_MS = 60_000L
        private const val MAX_NOTE_CACHE_AGE_MS = 10 * 60 * 1_000L
        private const val MAX_WEATHER_CACHE_AGE_MS = 12 * 60 * 60 * 1_000L
        private val PREFERRED_PROVIDERS = listOf(
            "fused",
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
        )
    }
}
