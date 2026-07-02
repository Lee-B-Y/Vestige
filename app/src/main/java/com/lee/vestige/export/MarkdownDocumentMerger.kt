package com.lee.vestige.export

/** Safely refreshes Vestige-managed blocks in an existing daily note. */
object MarkdownDocumentMerger {

    private val sectionOrder = listOf("weather", "location", "health", "calendar")
    private val refreshableKeys = setOf("weather", "health", "calendar")
    private val legacyTitles = mapOf(
        "weather" to setOf("天气", "Weather"),
        "location" to setOf("位置", "Location"),
        "health" to setOf("健康", "Health"),
        "calendar" to setOf("事件", "Events", "Calendar"),
    )
    private val notesHeading = Regex("(?m)^## (?:笔记|Notes)\\s*$")

    fun merge(existing: String, fresh: String): String {
        val existingNotesStart = notesHeading.find(existing)?.range?.first ?: return existing
        val freshNotesStart = notesHeading.find(fresh)?.range?.first ?: return existing

        val existingPrefix = existing.substring(0, existingNotesStart)
        val freshPrefix = fresh.substring(0, freshNotesStart)
        val notes = existing.substring(existingNotesStart)

        val existingSections = extractSections(existingPrefix)
        val freshSections = extractMarkedSections(freshPrefix)
        val headerEnd = MARKED_SECTION.find(freshPrefix)?.range?.first ?: freshPrefix.length
        val header = freshPrefix.substring(0, headerEnd).trimEnd()

        return buildString {
            append(header)
            append("\n\n")
            sectionOrder.forEach { key ->
                val block = if (key in refreshableKeys) {
                    freshSections[key] ?: existingSections[key]
                } else {
                    existingSections[key]
                }
                if (block != null) {
                    append(block.trim())
                    append("\n\n")
                }
            }
            append(notes)
        }
    }

    private fun extractSections(prefix: String): Map<String, String> {
        val sections = extractMarkedSections(prefix).toMutableMap()
        legacyTitles.forEach { (key, titles) ->
            if (key !in sections) {
                findLegacySection(prefix, titles)?.let { body ->
                    sections[key] = markedBlock(key, body)
                }
            }
        }
        return sections
    }

    private fun extractMarkedSections(text: String): Map<String, String> =
        MARKED_SECTION.findAll(text).associate { match ->
            match.groupValues[1] to match.value.trimEnd()
        }

    private fun findLegacySection(text: String, titles: Set<String>): String? {
        val headings = SECTION_HEADING.findAll(text).toList()
        val headingIndex = headings.indexOfFirst { it.groupValues[1].trim() in titles }
        if (headingIndex < 0) return null
        val start = headings[headingIndex].range.first
        val end = headings.getOrNull(headingIndex + 1)?.range?.first ?: text.length
        return text.substring(start, end).trimEnd()
    }

    private fun markedBlock(key: String, body: String): String = buildString {
        appendLine("<!-- vestige:$key:start -->")
        appendLine(body)
        append("<!-- vestige:$key:end -->")
    }

    private val MARKED_SECTION = Regex(
        "(?ms)^<!-- vestige:([a-z_]+):start -->\\s*$.*?" +
            "^<!-- vestige:\\1:end -->\\s*$",
    )
    private val SECTION_HEADING = Regex("(?m)^## ([^\\r\\n]+)\\s*$")
}
