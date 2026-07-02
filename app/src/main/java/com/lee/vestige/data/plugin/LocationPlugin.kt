package com.lee.vestige.data.plugin

import android.content.Context
import com.lee.vestige.R
import com.lee.vestige.data.model.DaySection
import com.lee.vestige.data.source.LocationNameResolver
import com.lee.vestige.data.source.LocationProvider
import java.time.LocalDate

/** Adds the current place to a new note. Existing location blocks are never refreshed. */
class LocationPlugin(
    private val context: Context,
    private val locationProvider: LocationProvider,
    private val locationNameResolver: LocationNameResolver,
) : DataPlugin {

    override val key: String = "location"
    override val sectionTitle: String get() = context.getString(R.string.section_location)
    override val order: Int = 12

    override suspend fun fetch(date: LocalDate): DaySection? {
        val (latitude, longitude) = locationProvider.locationForNote() ?: return null
        val label = locationNameResolver.resolve(latitude, longitude)
        return DaySection(
            key = key,
            title = sectionTitle,
            order = order,
            lines = listOf("- $label"),
        )
    }
}
