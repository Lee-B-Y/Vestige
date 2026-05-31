package com.lee.vestige.export

import android.content.Context
import com.lee.vestige.R
import com.lee.vestige.data.model.DayEntry

/**
 * Renders a [DayEntry] into the final Markdown string.
 *
 * Section headings follow the system language (Events/事件, Weather/天气, Notes/笔记);
 * the frontmatter (`date`, `generated_by`) stays language-neutral. The trailing empty
 * Notes section is the user's free area, filled in later in Obsidian. `generated_by`
 * is a reserved format-version marker for future migrations; Obsidian ignores it.
 */
class MarkdownRenderer(private val context: Context) {

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

        appendLine("## ${context.getString(R.string.section_notes)}")
        appendLine()
    }

    companion object {
        const val GENERATED_BY = "Vestige/1.0"
    }
}
