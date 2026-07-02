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
        val fresh = document(
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
        assertFalse(result.contains("<!-- vestige:"))
    }

    @Test
    fun `replaces marked section without duplicating it`() {
        val existing = document(weather = "- 旧天气", calendar = "- 旧事件", marked = true)
            .replace("## 笔记\n", "## 笔记\n\n正文\n")
        val fresh = document(weather = "- 新天气", calendar = "- 新事件")

        val result = MarkdownDocumentMerger.merge(existing, fresh)

        assertFalse(result.contains("<!-- vestige:"))
        assertTrue(result.contains("- 新天气"))
        assertFalse(result.contains("- 旧天气"))
        assertTrue(result.endsWith("## 笔记\n\n正文\n"))
    }

    @Test
    fun `keeps existing block when fresh data is unavailable`() {
        val existing = document(weather = "- 旧天气", calendar = "- 旧事件")
        val fresh = document(weather = null, calendar = "- 新事件")

        val result = MarkdownDocumentMerger.merge(existing, fresh)

        assertTrue(result.contains("- 旧天气"))
        assertTrue(result.contains("- 新事件"))
    }

    @Test
    fun `never refreshes a user edited location block`() {
        val existing = document(
            weather = "- 旧天气",
            location = "- 用户手动修改的位置",
        )
        val fresh = document(
            weather = "- 新天气",
            location = "- 后台取得的新位置",
        )

        val result = MarkdownDocumentMerger.merge(existing, fresh)

        assertTrue(result.contains("- 用户手动修改的位置"))
        assertFalse(result.contains("- 后台取得的新位置"))
        assertTrue(result.contains("- 新天气"))
    }

    @Test
    fun `does not restore a location removed by the user`() {
        val existing = document(weather = "- 旧天气")
        val fresh = document(weather = "- 新天气", location = "- 新取得的位置")

        val result = MarkdownDocumentMerger.merge(existing, fresh)

        assertFalse(result.contains("vestige:location"))
        assertFalse(result.contains("- 新取得的位置"))
    }

    @Test
    fun `does not modify a document without notes boundary`() {
        val existing = "# 自定义文档\n\n正文"

        val result = MarkdownDocumentMerger.merge(existing, document(weather = "- 晴"))

        assertEquals(existing, result)
    }

    private fun document(
        weather: String?,
        location: String? = null,
        calendar: String? = null,
        marked: Boolean = false,
    ): String = buildString {
        appendLine("---")
        appendLine("date: 2026-07-01")
        appendLine("generated_by: Vestige/1.0")
        appendLine("---")
        appendLine()
        appendLine("# 2026-07-01")
        appendLine()
        weather?.let {
            appendSection("weather", "天气", it, marked)
        }
        location?.let {
            appendSection("location", "位置", it, marked)
        }
        calendar?.let {
            appendSection("calendar", "事件", it, marked)
        }
        appendLine("## 笔记")
    }

    private fun StringBuilder.appendSection(
        key: String,
        title: String,
        body: String,
        marked: Boolean,
    ) {
        if (marked) appendLine("<!-- vestige:$key:start -->")
        appendLine("## $title")
        appendLine()
        appendLine(body)
        if (marked) appendLine("<!-- vestige:$key:end -->")
        appendLine()
    }
}
