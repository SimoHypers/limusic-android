package com.simohypers.limusic.utils

import com.simohypers.limusic.BuildKonfig

object VersionManager {
    // Read straight from the build: a background job (the update check) runs without MainActivity.
    fun getVersionName(): String = removeDevSuffix(BuildKonfig.versionName)

    private fun removeDevSuffix(versionName: String): String {
        return if (versionName.endsWith("-dev")) {
            versionName.replace("-dev", "")
        } else {
            versionName
        }
    }
}

/** A release candidate or other prerelease: anything with a `-` suffix (`1.1.0-rc.2`). */
fun isPrerelease(version: String): Boolean = version.contains('-')

/** `x.y.z` with an optional prerelease suffix, as release tags are named (the leading "v" removed). */
fun isVersion(version: String): Boolean = Regex("""\d+\.\d+\.\d+(-[0-9A-Za-z.]+)?""").matches(version)

/**
 * Orders versions as desktop's version.ts does: `x.y.z` numerically, a release above its own
 * prereleases (1.1.0 > 1.1.0-rc.2), and two prereleases of one version by suffix, numbers compared
 * as numbers (rc.10 > rc.9).
 */
fun compareVersions(
    a: String,
    b: String,
): Int {
    val (ac, sa) = a.split('-', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
    val (bc, sb) = b.split('-', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
    val pa = ac.split('.').map { it.toIntOrNull() ?: 0 }
    val pb = bc.split('.').map { it.toIntOrNull() ?: 0 }
    for (i in 0 until 3) {
        val diff = pa.getOrElse(i) { 0 }.compareTo(pb.getOrElse(i) { 0 })
        if (diff != 0) return diff
    }
    if (sa == sb) return 0
    if (sa.isEmpty()) return 1
    if (sb.isEmpty()) return -1
    // ponytail: numeric collation, not semver's full identifier rules; right for rc.N / beta.N
    val ta = Regex("""\d+|\D+""").findAll(sa).map { it.value }.toList()
    val tb = Regex("""\d+|\D+""").findAll(sb).map { it.value }.toList()
    for (i in 0 until minOf(ta.size, tb.size)) {
        val x = ta[i].toLongOrNull()
        val y = tb[i].toLongOrNull()
        val diff = if (x != null && y != null) x.compareTo(y) else ta[i].compareTo(tb[i])
        if (diff != 0) return diff
    }
    return ta.size.compareTo(tb.size)
}
