package com.simohypers.limusic.kotlinytmusicscraper.parser

import com.simohypers.limusic.kotlinytmusicscraper.models.MusicResponsiveListItemRenderer
import com.simohypers.limusic.kotlinytmusicscraper.models.MusicTwoRowItemRenderer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** One page of a library browse: every card and row it holds, and the token for the next page. */
data class LibraryPage(
    val cards: List<MusicTwoRowItemRenderer>,
    val rows: List<MusicResponsiveListItemRenderer>,
    val continuation: String?,
)

/**
 * Desktop's `parse_library` and `continuation_token`: the account's library browses come as
 * grids or shelves nested differently from page to page, so the cards, rows and token are taken
 * from wherever they sit rather than from one path. A renderer that fails to decode is skipped.
 */
fun parseLibraryPage(
    root: JsonElement,
    json: Json,
): LibraryPage {
    val cards =
        root.findAll("musicTwoRowItemRenderer").mapNotNull {
            runCatching { json.decodeFromJsonElement(MusicTwoRowItemRenderer.serializer(), it) }.getOrNull()
        }
    val rows =
        root.findAll("musicResponsiveListItemRenderer").mapNotNull {
            runCatching { json.decodeFromJsonElement(MusicResponsiveListItemRenderer.serializer(), it) }.getOrNull()
        }
    val token =
        root.findAll("continuationCommand").firstNotNullOfOrNull { it.stringAt("token") }
            ?: root.findAll("nextContinuationData").firstNotNullOfOrNull { it.stringAt("continuation") }
    return LibraryPage(cards, rows, token)
}

/** One day bucket of `FEmusic_history`: YouTube's own name for it ("Today", "Last week") and its rows. */
data class HistoryShelf(
    val title: String,
    val rows: List<MusicResponsiveListItemRenderer>,
)

/**
 * Desktop's `parse_history`: one `musicShelfRenderer` per bucket, rows newest first. Only a
 * shelf's own `contents` are read, so nothing nested inside a row counts twice; a bucket with no
 * row that decodes is dropped.
 */
fun parseHistory(
    root: JsonElement,
    json: Json,
): List<HistoryShelf> =
    root.findAll("musicShelfRenderer").mapNotNull { shelf ->
        val obj = shelf as? JsonObject ?: return@mapNotNull null
        val title = obj["title"]?.findAll("text")?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }?.joinToString("")
        val rows =
            obj["contents"]?.findAll("musicResponsiveListItemRenderer").orEmpty().mapNotNull {
                runCatching { json.decodeFromJsonElement(MusicResponsiveListItemRenderer.serializer(), it) }.getOrNull()
            }
        if (title.isNullOrBlank() || rows.isEmpty()) null else HistoryShelf(title, rows)
    }

internal fun JsonElement.stringAt(key: String) = ((this as? JsonObject)?.get(key) as? JsonPrimitive)?.contentOrNull

/** Every value under [key], at any depth; a match is not searched further. */
internal fun JsonElement.findAll(
    key: String,
    out: MutableList<JsonElement> = mutableListOf(),
): List<JsonElement> {
    when (this) {
        is JsonObject -> forEach { (k, v) -> if (k == key) out.add(v) else v.findAll(key, out) }
        is JsonArray -> forEach { it.findAll(key, out) }
        else -> Unit
    }
    return out
}
