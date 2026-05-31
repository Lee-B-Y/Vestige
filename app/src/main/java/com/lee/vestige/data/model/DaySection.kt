package com.lee.vestige.data.model

/**
 * One Markdown section contributed by a [com.lee.vestige.data.plugin.DataPlugin].
 *
 * [lines] are already-formatted Markdown lines — the plugin owns its own formatting
 * (bullet, plain sentence, table row, ...). The renderer never rewrites them; it only
 * prepends the heading and orders sections. This keeps the renderer dumb and stable as
 * new data sources (weather, steps, ...) are added.
 */
data class DaySection(
    val title: String,
    val order: Int,
    val lines: List<String>,
)
