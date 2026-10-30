package com.simohypers.limusic.media3.service.download

import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException
import kotlin.math.min

/**
 * Reads [upstream] in bounded ranges of [CHUNK] bytes, one after another, as one stream.
 *
 * googlevideo throttles a whole-file request (open-ended `Range: bytes=X-`, a plain GET, or a URL
 * carrying `&range=0-<length>`) to about twice real time, and serves a bounded 4 MiB range at full
 * speed: Limusic measured a 60 MB mix at 32 KB/s open-ended and 9.4 MB/s in chunks (audioproxy.rs).
 * A download is one CacheWriter read from start to end, so without this every song took about half
 * its length to fetch. The `range` query parameter is dropped so the Range header is the only one.
 *
 * Written out rather than delegated with `by`: [DataSource] has Java default methods.
 */
@UnstableApi
internal class ChunkedDataSource(
    private val upstream: DataSource,
) : DataSource {
    private var spec: DataSpec? = null
    private var position = 0L

    // Absolute end of what was asked for, exclusive; LENGTH_UNSET until the first response says.
    private var end = C.LENGTH_UNSET.toLong()
    private var chunkLeft = 0L

    override fun addTransferListener(transferListener: TransferListener) = upstream.addTransferListener(transferListener)

    override fun open(dataSpec: DataSpec): Long {
        val clean = dataSpec.withUri(withoutRangeParam(dataSpec.uri.toString()).toUri())
        spec = clean
        position = clean.position
        end = if (clean.length != C.LENGTH_UNSET.toLong()) clean.position + clean.length else C.LENGTH_UNSET.toLong()
        val opened = openChunk()
        if (end == C.LENGTH_UNSET.toLong()) {
            val total = upstream.responseHeaders.entries.firstOrNull { it.key.equals("Content-Range", ignoreCase = true) }?.value?.firstOrNull()
            end =
                totalFromContentRange(total)
                    // No Content-Range: the server ignored the range and sent the whole rest. Read it as is.
                    ?: run {
                        chunkLeft = Long.MAX_VALUE
                        return opened
                    }
        }
        return end - clean.position
    }

    private fun openChunk(): Long {
        val length = if (end == C.LENGTH_UNSET.toLong()) CHUNK else min(CHUNK, end - position)
        val opened = upstream.open(spec!!.buildUpon().setPosition(position).setLength(length).build())
        chunkLeft = if (opened == C.LENGTH_UNSET.toLong()) length else opened
        return opened
    }

    override fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Int {
        if (length == 0) return 0
        if (end != C.LENGTH_UNSET.toLong() && position >= end) return C.RESULT_END_OF_INPUT
        if (chunkLeft == 0L) {
            upstream.close()
            openChunk()
        }
        val read = upstream.read(buffer, offset, min(length.toLong(), chunkLeft).toInt())
        if (read == C.RESULT_END_OF_INPUT) {
            // Ending short of a known end would have the download saved truncated: fail it, so the
            // manager retries from what is on disk.
            if (end != C.LENGTH_UNSET.toLong() && position < end) throw IOException("Range ended at $position of $end")
            return C.RESULT_END_OF_INPUT
        }
        position += read
        chunkLeft -= read
        return read
    }

    override fun getUri() = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() {
        spec = null
        upstream.close()
    }

    class Factory(
        private val upstream: DataSource.Factory,
    ) : DataSource.Factory {
        override fun createDataSource() = ChunkedDataSource(upstream.createDataSource())
    }

    companion object {
        const val CHUNK = 4L * 1024 * 1024
    }
}

private val RANGE_PARAM = Regex("([?&])range=\\d+-\\d*(&?)")

/** The URL without its `range=a-b` query parameter, left otherwise byte for byte as it was (the signature is in there). */
internal fun withoutRangeParam(url: String): String =
    RANGE_PARAM.replace(url) { m -> if (m.groupValues[2].isNotEmpty()) m.groupValues[1] else "" }

/** The total in `bytes 0-4194303/7654321`, or null when absent or `*`. */
internal fun totalFromContentRange(value: String?): Long? = value?.substringAfterLast('/', "")?.toLongOrNull()
