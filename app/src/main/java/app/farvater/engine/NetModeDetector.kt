package app.farvater.engine

import app.farvater.core.model.NetMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object NetModeDetector {
    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(6, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .followRedirects(false)
        .build()

    private val foreign = listOf(
        "https://www.gstatic.com/generate_204",
        "https://cp.cloudflare.com/generate_204",
        "https://github.com/",
    )
    private val domestic = listOf(
        "https://ya.ru/",
        "https://vk.com/",
        "https://www.gosuslugi.ru/",
    )

    suspend fun detect(): NetMode? = coroutineScope {
        val abroad = foreign.map { async(Dispatchers.IO) { reachable(it) } }
        val home = domestic.map { async(Dispatchers.IO) { reachable(it) } }
        when {
            abroad.any { it.await() } -> NetMode.BLACK
            home.any { it.await() } -> NetMode.WHITE
            else -> null
        }
    }

    private suspend fun reachable(url: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            client.newCall(Request.Builder().url(url).head().build()).execute().use { true }
        }.getOrDefault(false)
    }
}
