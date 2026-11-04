package com.simohypers.limusic.updater

/**
 * The Markdown that release notes use, and no more: headings, bullets, paragraphs, code fences and
 * images (GitHub's editor writes a pasted screenshot as raw `<img>`). Ported from desktop's
 * Changelog.svelte; bold, code, links and @mentions inside a line are drawn by the page.
 */
sealed interface NoteBlock {
    data class Heading(
        val text: String,
    ) : NoteBlock

    data class Paragraph(
        val text: String,
    ) : NoteBlock

    data class Bullets(
        val items: List<String>,
    ) : NoteBlock

    data class Code(
        val text: String,
    ) : NoteBlock

    data class Image(
        val url: String,
    ) : NoteBlock
}

private val IMAGE = Regex("""!\[[^\]]*]\(([^)]+)\)|<img\b[^>]*?\bsrc=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
private val TAG = Regex("""</?[a-z][^>]*>""", RegexOption.IGNORE_CASE)
private val HEADING = Regex("""^#{1,6}\s+(.*)""")
private val BULLET = Regex("""^[-*]\s+(.*)""")

/** Line by line on purpose: release notes are headings, bullets and one-line paragraphs. */
fun parseReleaseNotes(body: String): List<NoteBlock> {
    val blocks = mutableListOf<NoteBlock>()
    var fenced: MutableList<String>? = null
    for (raw in body.replace("\r", "").split('\n')) {
        val trimmed = raw.trim()
        if (trimmed.startsWith("```")) {
            fenced?.let { blocks += NoteBlock.Code(it.joinToString("\n")) }
            fenced = if (fenced == null) mutableListOf() else null
            continue
        }
        fenced?.let {
            it += raw
            continue
        }
        IMAGE.findAll(trimmed).forEach { m -> blocks += NoteBlock.Image(m.groupValues[1].ifEmpty { m.groupValues[2] }) }
        // Whatever HTML is left (<p align="center">, <br>, <details>) is layout, not content.
        val line = trimmed.replace(IMAGE, "").replace(TAG, "").trim()
        if (line.isEmpty()) continue
        val heading = HEADING.find(line)
        val bullet = BULLET.find(line)
        val last = blocks.lastOrNull()
        when {
            heading != null -> blocks += NoteBlock.Heading(heading.groupValues[1])
            bullet != null && last is NoteBlock.Bullets -> blocks[blocks.lastIndex] = NoteBlock.Bullets(last.items + bullet.groupValues[1])
            bullet != null -> blocks += NoteBlock.Bullets(listOf(bullet.groupValues[1]))
            else -> blocks += NoteBlock.Paragraph(line)
        }
    }
    return blocks
}
