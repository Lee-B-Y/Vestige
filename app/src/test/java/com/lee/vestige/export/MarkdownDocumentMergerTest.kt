package com.lee.vestige.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownDocumentMergerTest {

    @Test
    fun `refreshes legacy blocks and preserves notes`() {
        val existing = """---
date: 2026-07-01
---

# 2026-07-01

## 天气

- 旧天气

## 健康

- 步数 1,000

## 事件

- 旧事件

## 笔记

这是我的日记。
第二行保持不变。
"""
        val fresh = markedDocument(
            weather = "- 新天气",
            calendar = "- 新事件",
        )

        val result = MarkdownDocumentMerger.merge(existing, fresh)

        assertTrue(result.contains("- 新天气"))
        assertTrue(result.contains("- 步数 1,000"))
        assertTrue(result.contains("- 新事件"))
        assertFalse(result.contains("旧天气"))
        assertFalse(result.contains("旧事件"))
        assertEquals(existing.substringAfter("## 笔记"), result.substringAfter("## 笔记"))
        assertTrue(result.contains("<!-- vestige:health:start -->"))
    }

    @Test
    fun `replaces marked section without duplicating it`() {
        val existing = markedDocument(weather = "- 旧天气", calendar = "- 旧事件")
            .replace("## 笔记\n", "## 笔记\n\n正文\n")
        val fresh = markedDocument(weather = "- 新天气", calendar = "- 新事件")

        val result = MarkdownDocumentMerger.merge(existing, fresh)

        assertEquals(1, "<!-- vestige:weather:start -->".toRegex().findAll(result).count())
        assertTrue(result.contains("- 新天气"))
        assertFalse(result.contains("- 旧天气"))
        assertTrue(result.endsWith("## 笔记\n\n正文\n"))
    }

    @Test
    fun `keeps existing block when fresh data is unavailable`() {
        val existing = markedDocument(weather = "- 旧天气", calendar = "- 旧事件")
        val fresh = markedDocument(weather = null, calendar = "- 新事件")

        val result = MarkdownDocumentMerger.merge(existing, fresh)

        assertTrue(result.contains("- 旧天气"))
        assertTrue(result.contains("- 新事件"))
    }

    @Test
    fun `does not modify a document without notes boundary`() {
        val existing = "# 自定义文档\n\n正文"

        val result = MarkdownDocumentMerger.merge(existing, markedDocument(weather = "- 晴"))

        assertEquals(existing, result)
    }

    private fun markedDocument(
        weather: String?,
        calendar: String? = null,
    ): String = buildString {
        appendLine("---")
        appendLine("date: 2026-07-01")
        appendLine("generated_by: Vestige/1.0")
        appendLine("---")
        appendLine()
        appendLine("# 2026-07-01")
        appendLine()
        weather?.let {
            appendLine("<!-- vestige:weather:start -->")
            appendLine("## 天气")
            appendLine()
            appendLine(it)
            appendLine("<!-- vestige:weather:end -->")
            appendLine()
        }
        calendar?.let {
            appendLine("<!-- vestige:calendar:start -->")
            appendLine("## 事件")
            appendLine()
            appendLine(it)
            appendLine("<!-- vestige:calendar:end -->")
            appendLine()
        }
        appendLine("## 笔记")
    }
}
