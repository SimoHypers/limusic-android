package com.maxrave.kotlinytmusicscraper.extractor

import io.ktor.http.decodeURLQueryComponent
import io.ktor.http.encodeURLParameter

/**
 * A format's `signatureCipher`: the scrambled signature, the query key the solved one goes under,
 * and the URL it belongs to. Port of desktop Limusic's `cipher::parse_cipher`.
 */
internal data class SignatureCipher(
    val s: String,
    val sp: String,
    val url: String,
)

internal fun parseSignatureCipher(cipher: String): SignatureCipher? {
    var s: String? = null
    var sp: String? = null
    var url: String? = null
    for (pair in cipher.split('&')) {
        val value = pair.substringAfter('=', "").decodeURLQueryComponent()
        when (pair.substringBefore('=')) {
            "s" -> s = value
            "sp" -> sp = value
            "url" -> url = value
        }
    }
    return SignatureCipher(s ?: return null, sp ?: "signature", url ?: return null)
}

/** The URL with its solved [signature] appended. */
internal fun SignatureCipher.signedUrl(signature: String): String =
    url + (if ('?' in url) '&' else '?') + sp + "=" + signature.encodeURLParameter()

private val N_PARAM = Regex("[?&]n=([^&]+)")

/** The decoded `n` challenge in [url], or null when it carries none. */
internal fun nChallengeOf(url: String): String? = N_PARAM.find(url)?.groupValues?.get(1)?.decodeURLQueryComponent()

/** [url] with its `n` replaced by the [solved] value, everything else left byte for byte. */
internal fun withN(
    url: String,
    solved: String,
): String {
    val match = N_PARAM.find(url) ?: return url
    return url.replaceRange(match.range, match.value.first() + "n=" + solved.encodeURLParameter())
}
