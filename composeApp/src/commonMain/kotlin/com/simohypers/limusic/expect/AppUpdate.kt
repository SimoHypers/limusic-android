package com.simohypers.limusic.expect

/** The ABI this phone prefers, spelled as release APK names spell it ("arm64-v8a"). */
expect fun preferredAbi(): String

/**
 * Downloads the APK at [url], checks it against [sha256] (lowercase hex) and hands it to the
 * system installer, reporting download progress 0..1 through [onProgress].
 *
 * Returns only when the update didn't happen: null when the user cancelled the install, otherwise
 * why it failed. A successful update replaces the app, which ends this process.
 */
expect suspend fun downloadAndInstallApk(
    url: String,
    sha256: String,
    userAgent: String,
    onProgress: (Float) -> Unit,
): String?
