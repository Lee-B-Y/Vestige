package com.lee.vestige.data.source

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/** Converts coordinates to a concise, editable place label for a daily note. */
class LocationNameResolver(private val context: Context) {

    suspend fun resolve(latitude: Double, longitude: Double): String {
        val address = try {
            firstAddress(latitude, longitude)
        } catch (_: Exception) {
            null
        }
        val parts = address?.let {
            listOf(it.adminArea, it.locality, it.subLocality)
                .filterNotNull()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinct()
        }.orEmpty()
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" ")
            ?: String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
    }

    private suspend fun firstAddress(latitude: Double, longitude: Double): Address? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                runCatching {
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                            }

                            override fun onError(errorMessage: String?) {
                                if (continuation.isActive) continuation.resume(null)
                            }
                        },
                    )
                }.onFailure {
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        } else {
            @Suppress("DEPRECATION")
            withContext(Dispatchers.IO) {
                runCatching { geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull() }
                    .getOrNull()
            }
        }
    }
}
