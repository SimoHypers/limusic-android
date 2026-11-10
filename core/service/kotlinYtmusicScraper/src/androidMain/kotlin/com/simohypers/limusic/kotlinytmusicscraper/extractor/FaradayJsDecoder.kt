package com.simohypers.limusic.kotlinytmusicscraper.extractor

import android.content.Context
import com.simohypers.limusic.kotlinytmusicscraper.cipher.FaradayCipherEngine
import com.simohypers.limusic.kotlinytmusicscraper.cipher.InnerTubeLogLevel
import com.simohypers.limusic.kotlinytmusicscraper.cipher.InnerTubeLogger
import com.simohypers.limusic.ktorext.getEngine
import com.simohypers.limusic.logger.Logger
import dev.maxrave.pipepipe.extractor.exceptions.ParsingException
import dev.maxrave.pipepipe.extractor.services.youtube.YoutubeApiDecoder
import dev.maxrave.pipepipe.extractor.services.youtube.YoutubeJavaScriptDecoder
import io.ktor.client.HttpClient
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.koin.mp.KoinPlatform.getKoin
import java.io.File
import java.util.concurrent.Executors

private const val TAG = "FaradayJsDecoder"
private const val JS_THREAD_STACK_BYTES = 32L * 1024L * 1024L

/**
 * Plugs [FaradayCipherEngine] into PipePipe as its local decoder, so signature and `n` challenges
 * are solved on the device instead of at `api.pipepipe.dev`.
 *
 * PipePipe keeps its own server call as the fallback: [YoutubeApiDecoder.decodeBatch] tries this
 * decoder first and drops through to the API when it throws. **Throwing is therefore the way to
 * hand a request back** — returning a half-filled result would be accepted as final and the missing
 * URLs would simply fail later, skipping the fallback entirely.
 */
internal class FaradayJsDecoder : YoutubeJavaScriptDecoder {
    /**
     * One thread, 32 MB of stack.
     *
     * YouTube's n-transform recurses far past what a default ~1 MB JVM thread holds, and QuickJS
     * runs it on the native stack. When it overruns, QuickJS does not reliably raise a JS error —
     * the process takes SIGSEGV with no hs_err file, because the crash is below the JVM.
     */
    private val jsThread =
        Executors
            .newSingleThreadExecutor { runnable -> Thread(null, runnable, "QuickJs", JS_THREAD_STACK_BYTES) }
            .asCoroutineDispatcher()

    /** Shared with the stream chain in YouTube.player through [Extractor.cipher]. */
    val engine by lazy {
        val saved = File(getKoin().get<Context>().cacheDir, "player_id.txt")
        FaradayCipherEngine(
            HttpClient(getEngine()),
            jsThread,
            bridgedLogger,
            savedPlayerId = { runCatching { saved.readText().trim() }.getOrNull() },
            savePlayerId = { runCatching { saved.writeText(it) } },
        )
    }

    override fun getPlayerData(videoId: String): YoutubeJavaScriptDecoder.PlayerData {
        val info =
            runBlocking { engine.playerInfo() }
                ?: throw ParsingException("Faraday: no player metadata")
        return YoutubeJavaScriptDecoder.PlayerData(info.playerId, info.signatureTimestamp)
    }

    override fun decodeBatch(
        playerId: String,
        signatures: List<String>?,
        throttlingParameters: List<String>?,
    ): YoutubeApiDecoder.BatchDecodeResult {
        val wantedSignatures = signatures.orEmpty().distinct()
        val wantedNParameters = throttlingParameters.orEmpty().distinct()
        val result = runBlocking { engine.decode(playerId, wantedSignatures, wantedNParameters) }

        // All or nothing: one unsolved challenge means the player table is stale or the script
        // changed shape, and both are reasons to let the whole batch go to the API rather than
        // ship a URL that will come back 403.
        val missing =
            wantedSignatures.count { it !in result.signatures } +
                wantedNParameters.count { it !in result.nParameters }
        val wanted = wantedSignatures.size + wantedNParameters.size
        if (missing > 0) throw ParsingException("Faraday: $missing of $wanted unsolved")
        Logger.d(TAG, "solved $wanted locally (sig=${wantedSignatures.size} n=${wantedNParameters.size})")
        return YoutubeApiDecoder.BatchDecodeResult(result.signatures, result.nParameters)
    }

    private val bridgedLogger =
        InnerTubeLogger { event ->
            val message = event.message + event.details.entries.joinToString(prefix = " [", postfix = "]") { "${it.key}=${it.value}" }
            when (event.level) {
                InnerTubeLogLevel.DEBUG -> Logger.d(TAG, message)
                InnerTubeLogLevel.INFO -> Logger.i(TAG, message)
                InnerTubeLogLevel.WARN -> Logger.w(TAG, message)
                InnerTubeLogLevel.ERROR -> Logger.e(TAG, message)
            }
        }
}
