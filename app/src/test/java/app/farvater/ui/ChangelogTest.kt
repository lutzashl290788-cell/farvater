package app.farvater.ui

import app.farvater.ui.screens.parseChangelog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ChangelogTest {
    private val releases = parseChangelog(File("../CHANGELOG.md").readText())

    @Test
    fun allReleasesFromFirstToLast() {
        assertEquals("1.0.0", releases.last().version)
        assertTrue(releases.none { it.version == "Не выпущено" })
        assertTrue(releases.all { it.groups.isNotEmpty() })
    }

    @Test
    fun dateInRussian() {
        assertEquals("5 октября 2026", releases.last().date)
    }

    @Test
    fun markdownRemoved() {
        assertTrue(releases.flatMap { r -> r.groups.flatMap { it.items } }.none { '`' in it })
    }

    @Test
    fun parsesSample() {
        val text = """
            # Список изменений

            ## [Не выпущено]

            ## [2.0.0] — 2026-12-31

            Большой выпуск.

            ### Добавлено

            - Режим `БС`

            [2.0.0]: https://example.org
        """.trimIndent()
        val r = parseChangelog(text).single()
        assertEquals("2.0.0", r.version)
        assertEquals("31 декабря 2026", r.date)
        assertEquals(listOf("Большой выпуск."), r.intro)
        assertEquals(listOf("Режим БС"), r.groups.single().items)
    }
}
