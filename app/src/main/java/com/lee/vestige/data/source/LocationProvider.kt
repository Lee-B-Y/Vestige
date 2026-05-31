package com.lee.vestige.data.source

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Returns the device's last-known coarse location, or `null` if permission is missing,
 * location is disabled, or no cached fix exists. Intentionally uses [LocationManager]
 * (no Google Play Services dependency) — last-known is good enough for daily weather.
 */
class LocationProvider(private val context: Context) {

    @SuppressLint("MissingPermission") // permission is checked in hasPermission()
    suspend fun lastKnownLocation(): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext null

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext null

        var best: Location? = null
        for (provider in lm.getProviders(true)) {
            val loc = runCatching { lm.getLastKnownLocation(provider) }.getOrNull() ?: continue
            // Lower accuracy value == more precise; prefer the most precise fix.
            if (best == null || loc.accuracy < best!!.accuracy) best = loc
        }
        best?.let { it.latitude to it.longitude }
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
}
