package com.maxrave.domain.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.maxrave.domain.data.model.metadata.Line

/**
 * A song's lyrics as cached. A hit is kept for good; [error] marks a "no lyrics anywhere" verdict,
 * which only holds for a day from [fetchedAt] (epoch seconds). Rows from before the Limusic chain
 * have no [provider] and are fetched again.
 */
@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey(autoGenerate = false) val videoId: String,
    val error: Boolean,
    val lines: List<Line>?,
    val syncType: String?,
    /** The LyricsSource id that answered. */
    val provider: String? = null,
    @ColumnInfo(defaultValue = "0") val fetchedAt: Long = 0,
    /** Timing nudge for this song, positive = lyrics later. */
    @ColumnInfo(defaultValue = "0") val offsetMs: Int = 0,
    /** Picked by hand or nudged: kept when the provider order changes. */
    @ColumnInfo(defaultValue = "0") val pinned: Boolean = false,
)
