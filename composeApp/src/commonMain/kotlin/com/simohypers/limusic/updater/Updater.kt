package com.simohypers.limusic.updater

import com.simohypers.limusic.BuildKonfig
import com.simohypers.limusic.common.AppIdentity
import com.simohypers.limusic.expect.downloadAndInstallApk
import com.simohypers.limusic.expect.preferredAbi
import com.simohypers.limusic.logger.Logger
import com.simohypers.limusic.utils.VersionManager
import com.simohypers.limusic.utils.compareVersions
import com.simohypers.limusic.utils.isPrerelease
import com.simohypers.limusic.utils.isVersion
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.update_failed
import multiplatform.network.cmptoast.ToastDuration
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform.getKoin
import kotlin.time.Duration.Companion.hours

private const val TAG = "Updater"
private const val GITHUB = "https://api.github.com/repos/SimoHypers/limusic-android"

/** A release newer than this build, with the APK picked for this phone. */
data class AvailableUpdate(
    val version: String,
    val apkUrl: String,
    val sha256: String,
)

data class UpdateState(
    val available: AvailableUpdate? = null,
    /** 0..1 while the APK downloads, 1 while the system installs it, null otherwise. */
    val progress: Float? = null,
)

@Serializable
private data class Release(
    @SerialName("tag_name") val tag: String,
    val draft: Boolean = false,
    val assets: List<Asset> = emptyList(),
)

@Serializable
private data class Asset(
    val name: String,
    @SerialName("browser_download_url") val url: String,
    /** "sha256:<hex>", which GitHub computes on upload. */
    val digest: String? = null,
)

/**
 * Desktop's updater on GitHub releases: a quiet check on launch and every [INTERVAL], a banner
 * above the mini player, and an install through the system's package installer. Android can't
 * downgrade, so unlike desktop there is no rollback: only a newer version is offered.
 */
object Updater {
    val INTERVAL = 6.hours

    private val _state = MutableStateFlow(UpdateState())
    val state = _state.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    // ponytail: lives as long as the process, like artworkTintHttpClient
    private val http by lazy { HttpClient(CIO) }

    private val identity get() = getKoin().get<AppIdentity>()

    /** Debug builds can read a local test server instead (`limusic.updateBase` in local.properties). */
    private val base get() = BuildKonfig.updateBase.takeIf { identity.isDevBuild && it.isNotBlank() } ?: GITHUB

    private suspend fun fetch(path: String): String {
        val response =
            http.get("$base$path") {
                header("Accept", "application/vnd.github+json")
                header("User-Agent", identity.userAgent)
            }
        check(response.status.value == 200) { "HTTP ${response.status.value}" }
        return response.bodyAsText()
    }

    /** The newest release above this build that has an APK for this phone, or null. */
    private suspend fun look(): AvailableUpdate? {
        val current = VersionManager.getVersionName()
        // ponytail: RC builds follow the beta channel; Settings > About > Beta versions comes next
        val beta = isPrerelease(current)
        val releases =
            if (beta) {
                json.decodeFromString<List<Release>>(fetch("/releases"))
            } else {
                listOf(json.decodeFromString<Release>(fetch("/releases/latest")))
            }
        val abi = preferredAbi()
        return releases
            .filter { !it.draft }
            .mapNotNull { release ->
                val version = release.tag.removePrefix("v").takeIf(::isVersion) ?: return@mapNotNull null
                val apk =
                    release.assets.firstOrNull { it.name.endsWith("-$abi.apk") }
                        ?: release.assets.firstOrNull { it.name.endsWith("-universal.apk") }
                // No digest, no install: it is the only check on the download besides the signature.
                val sha256 = apk?.digest?.removePrefix("sha256:")?.takeIf { it != apk.digest } ?: return@mapNotNull null
                AvailableUpdate(version, apk.url, sha256.lowercase())
            }.maxWithOrNull { a, b -> compareVersions(a.version, b.version) }
            ?.takeIf { compareVersions(it.version, current) > 0 }
    }

    /** Shows the banner when a newer version exists; says nothing otherwise, failures included. */
    suspend fun checkQuiet() {
        if (_state.value.available != null) return
        runCatching { look() }
            .onSuccess { found -> if (found != null) _state.update { it.copy(available = found) } }
            .onFailure { Logger.w(TAG, "Update check failed: ${it.message}") }
    }

    fun dismiss() = _state.update { if (it.progress == null) UpdateState() else it }

    // Not the banner's scope: leaving the screen it sits on must not cancel a download.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun install() {
        val offered = _state.value.available ?: return
        if (_state.value.progress != null) return
        _state.update { it.copy(progress = 0f) }
        scope.launch { install(offered) }
    }

    private suspend fun install(offered: AvailableUpdate) {
        // The banner can sit for hours; a release whose APK was replaced since has a new digest.
        val update = runCatching { look() }.getOrNull() ?: offered
        _state.value = UpdateState(available = update, progress = 0f)
        val error =
            downloadAndInstallApk(update.apkUrl, update.sha256, identity.userAgent) { p ->
                _state.update { it.copy(progress = p) }
            }
        // Only reached when the update didn't happen: a successful one ends this process.
        _state.update { it.copy(progress = null) }
        if (error != null) {
            Logger.e(TAG, "Update to ${update.version} failed: $error")
            showToast(message = getString(Res.string.update_failed, error), duration = ToastDuration.Long, gravity = ToastGravity.Bottom)
        }
    }
}
