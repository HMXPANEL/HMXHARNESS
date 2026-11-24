package com.hmx.harness.network

import java.io.IOException
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor

/**
 * Single policy for URLs that carry credentials (custom LLM providers and remote MCP servers).
 *
 * HTTPS is accepted for any host so legitimate custom providers and development tunnels keep
 * working. Plain HTTP is accepted only for loopback development servers; it is never accepted
 * for a remote host. The OS-level network security config enforces the same rule at the socket
 * layer, and [secureTransportInterceptor] repeats it at the request layer so a redirect cannot
 * downgrade a credential-bearing request without an explicit error.
 */
object RemoteUrlPolicy {

    private const val MESSAGE =
        "Remote URLs must use https://. Plain http:// is allowed only for localhost or 127.0.0.1 development servers."
    private val LOOPBACK_HOSTS = setOf("localhost", "127.0.0.1", "::1", "[::1]")

    fun isLoopback(host: String): Boolean {
        val lower = host.trim().lowercase().removePrefix("[").removeSuffix("]")
        return lower in LOOPBACK_HOSTS || lower.endsWith(".localhost")
    }

    /** Returns the original string unchanged. Never rewrites user-provided URLs. */
    fun validate(raw: String, kind: String): String {
        val trimmed = raw.trim()
        val url = trimmed.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Enter a valid $kind URL, including https:// or http://.")
        return when {
            url.scheme.equals("https", ignoreCase = true) -> trimmed
            url.scheme.equals("http", ignoreCase = true) && isLoopback(url.host) -> trimmed
            url.scheme.equals("http", ignoreCase = true) -> throw IllegalArgumentException("$kind: $MESSAGE")
            else -> throw IllegalArgumentException("$kind URLs must use https:// (or http:// for local development).")
        }
    }

    /** Null when the URL is acceptable, otherwise a message suitable for a validation UI. */
    fun validationError(raw: String, kind: String): String? = try {
        validate(raw, kind)
        null
    } catch (e: IllegalArgumentException) {
        e.message
    }

    fun allows(url: HttpUrl): Boolean =
        url.scheme.equals("https", ignoreCase = true) ||
            (url.scheme.equals("http", ignoreCase = true) && isLoopback(url.host))

    /**
     * Network interceptor: application interceptors only see the first request, while this one
     * sees every redirect follow-up as well. Throwing prevents a downgraded request from being
     * sent with the original credentials. Only scheme and host are included in the error.
     */
    val secureTransportInterceptor = Interceptor { chain ->
        val url = chain.request().url
        if (!allows(url)) {
            throw IOException("Blocked insecure request redirect to ${url.scheme}://${url.host}.")
        }
        chain.proceed(chain.request())
    }
}
