package com.simohypers.limusic.expect

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform.getKoin
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

actual fun preferredAbi(): String = Build.SUPPORTED_ABIS.firstOrNull().orEmpty()

actual suspend fun downloadAndInstallApk(
    url: String,
    sha256: String,
    userAgent: String,
    onProgress: (Float) -> Unit,
): String? {
    val context: Context = getKoin().get()
    return try {
        val apk = download(context, url, sha256, userAgent, onProgress)
        onProgress(1f)
        install(context, apk)
    } catch (e: Exception) {
        e.message ?: e.toString()
    }
}

private suspend fun download(
    context: Context,
    url: String,
    sha256: String,
    userAgent: String,
    onProgress: (Float) -> Unit,
): File =
    withContext(Dispatchers.IO) {
        // One APK at a time; whatever an earlier try left goes first.
        val dir = File(context.cacheDir, "update").apply { deleteRecursively() }
        dir.mkdirs()
        val apk = File(dir, "update.apk")
        val digest = MessageDigest.getInstance("SHA-256")
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.setRequestProperty("User-Agent", userAgent)
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            check(connection.responseCode == 200) { "HTTP ${connection.responseCode}" }
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                apk.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    var percent = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        done += read
                        val now = if (total > 0) (done * 100 / total).toInt() else 0
                        if (now != percent) {
                            percent = now
                            onProgress(now / 100f)
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        val hex = digest.digest().joinToString("") { "%02x".format(it) }
        check(hex == sha256) { "the download doesn't match the release" }
        apk
    }

/**
 * Hands [apk] to the system installer and waits for its verdict. The first update asks the user
 * (and, before that, for permission to install apps from Limusic); once Limusic has installed
 * itself, Android 12+ updates it without asking.
 */
private suspend fun install(
    context: Context,
    apk: File,
): String? {
    val action = "${context.packageName}.UPDATE_STATUS"
    val verdict = CompletableDeferred<String?>()
    val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                c: Context,
                intent: Intent,
            ) {
                when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
                    PackageInstaller.STATUS_PENDING_USER_ACTION ->
                        IntentCompat
                            .getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                            ?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    PackageInstaller.STATUS_SUCCESS, PackageInstaller.STATUS_FAILURE_ABORTED -> verdict.complete(null)
                    else -> verdict.complete(intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "status $status")
                }
            }
        }
    ContextCompat.registerReceiver(context, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED)
    try {
        withContext(Dispatchers.IO) {
            val installer = context.packageManager.packageInstaller
            val params =
                PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                    setAppPackageName(context.packageName)
                    setSize(apk.length())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                    }
                }
            val id = installer.createSession(params)
            installer.openSession(id).use { session ->
                apk.inputStream().use { input ->
                    session.openWrite("base.apk", 0, apk.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
                val status =
                    PendingIntent.getBroadcast(
                        context,
                        id,
                        Intent(action).setPackage(context.packageName),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                    )
                session.commit(status.intentSender)
            }
        }
        return verdict.await()
    } finally {
        context.unregisterReceiver(receiver)
    }
}
