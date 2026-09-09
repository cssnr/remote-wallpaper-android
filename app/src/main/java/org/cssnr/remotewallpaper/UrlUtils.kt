package org.cssnr.remotewallpaper

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

fun normalizeUrl(input: String): String? {
    val url = input.trim().let {
        if (it.contains("://")) it else "https://$it"
    }.toHttpUrlOrNull() ?: return null
    if (url.scheme !in listOf("http", "https")) return null
    if (url.host.isBlank()) return null
    if (url.toString().length > 2048) return null
    return url.toString()
}
