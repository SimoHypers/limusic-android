package com.simohypers.limusic.updater

import kotlin.test.Test
import kotlin.test.assertEquals

class ReleaseNotesMarkdownTest {
    @Test
    fun parsesTheReleaseNoteFormat() {
        val body =
            """
            The overview line.

            ### New features
            - **Updater**: one tap updates
            - Beta versions

            <p align="center"><img src="https://example.com/a.png" width="300"></p>
            ```
            code line
            ```
            """.trimIndent()
        assertEquals(
            listOf(
                NoteBlock.Paragraph("The overview line."),
                NoteBlock.Heading("New features"),
                NoteBlock.Bullets(listOf("**Updater**: one tap updates", "Beta versions")),
                NoteBlock.Image("https://example.com/a.png"),
                NoteBlock.Code("code line"),
            ),
            parseReleaseNotes(body),
        )
    }
}
