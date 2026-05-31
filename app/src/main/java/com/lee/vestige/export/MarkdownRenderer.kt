package com.lee.vestige.export

import com.lee.vestige.data.model.DayEntry

/**
 * Renders a [DayEntry] into the final Markdown string.
 *
 * Layout:
 * ```
 * ---
 * date: 2026-05-31
 * generated_by: Vestige/1.0
 * ---
 *
 * # 2026-05-31
 *
 * ## Calendar
 *
 * - 09:00-11:00 ...
 *
 * ## Notes
 *
 * ```
 * The trailing empty `## Notes` is the user's free area, filled in later in Obsidian.
 * `generated_by` is a reserved format-version marker for future migrations; Obsidian
 * ignores it.
 */
class MarkdownRenderer {

    fun render(entry: DayEntry): String = buildString {
        val date = entry.date.toString() // ISO-8601, e.g. 2026-05-31

        appendLine("---")
        appendLine("date: $date")
        appendLine("generated_by: $GENERATED_BY")
        appendLine("---")
        appendLine()

        appendLine("# $date")
        appendLine()

        entry.sections.forEach { section ->
            appendLine("## ${section.title}")
            appendLine()
            section.lines.forEach { appendLine(it) }
            appendLine()
        }

        appendLine("## Notes")
        appendLine()
    }

    companion object {
        const val GENERATED_BY = "Vestige/1.0"
    }
}
