package com.lee.vestige.di

import android.content.Context
import android.net.Uri
import com.lee.vestige.data.plugin.CalendarPlugin
import com.lee.vestige.data.plugin.DataPlugin
import com.lee.vestige.data.plugin.WeatherPlugin
import com.lee.vestige.data.settings.SettingsStore
import com.lee.vestige.data.source.CalendarDataSource
import com.lee.vestige.data.source.LocationProvider
import com.lee.vestige.data.source.WeatherDataSource
import com.lee.vestige.domain.DayAggregator
import com.lee.vestige.export.ExportTarget
import com.lee.vestige.export.MarkdownRenderer
import com.lee.vestige.export.SafExportTarget

/**
 * Tiny hand-rolled dependency container — intentionally not a DI framework for the MVP.
 *
 * To add a future data source: construct its plugin and add it to [plugins]. Nothing
 * else in the app changes.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val settingsStore = SettingsStore(appContext)

    private val calendarDataSource = CalendarDataSource(appContext)
    private val locationProvider = LocationProvider(appContext)
    private val weatherDataSource = WeatherDataSource()

    // Order within a day note is controlled by each plugin's `order`
    // (weather = 10, events = 20), not by list position.
    private val plugins: List<DataPlugin> = listOf(
        WeatherPlugin(appContext, locationProvider, weatherDataSource),
        CalendarPlugin(appContext, calendarDataSource),
        // Future: StepsPlugin(...), SleepPlugin(...)
    )

    val aggregator = DayAggregator(plugins)
    val renderer = MarkdownRenderer(appContext)

    /**
     * Resolves the export destination. V1 always returns a local SAF target built from
     * the chosen directory. Future: read a "backend" setting and return a cloud target
     * (OneDrive / Baidu Netdisk) instead — callers stay unchanged.
     */
    fun exportTargetFor(treeUri: Uri): ExportTarget = SafExportTarget(appContext, treeUri)
}
