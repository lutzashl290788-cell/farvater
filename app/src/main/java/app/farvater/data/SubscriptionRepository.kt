package app.farvater.data

import android.content.Context
import app.farvater.BuildConfig
import app.farvater.core.model.ProxyNode
import app.farvater.core.parser.SubscriptionParser
import app.farvater.core.xray.XrayConfigBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

data class SourceSnapshot(
    val sourceId: String,
    val title: String?,
    val announce: String?,
    val notices: List<String>,
    val nodes: List<ProxyNode>,
    val updatedAt: Long,
    val error: String? = null,
    val skipped: Map<String, Int> = emptyMap(),
    val intervalHours: Int? = null,
)

class SubscriptionRepository(context: Context) {
    private val dir = File(context.filesDir, "subs").apply { mkdirs() }
    private val manualFile = File(dir, "${ProxyNode.MANUAL_SOURCE}.txt")

    private val baseClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    private val tunnelClient by lazy {
        baseClient.newBuilder()
            .proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", XrayConfigBuilder.SOCKS_PORT)))
            .build()
    }

    suspend fun fetch(
        sourceId: String,
        urls: List<String>,
        viaTunnel: Boolean,
        extraHeaders: Map<String, String> = emptyMap(),
    ): SourceSnapshot =
        withContext(Dispatchers.IO) {
            val client = if (viaTunnel) tunnelClient else baseClient
            var lastError: String? = null
            for (url in urls) {
                try {
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "Farvater/${BuildConfig.VERSION_NAME}")
                        .apply { extraHeaders.forEach { (k, v) -> header(k, v) } }
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            lastError = "HTTP ${response.code}"
                            return@use
                        }
                        val body = response.body?.string().orEmpty()
                        val headers = response.headers.toMultimap().mapValues { it.value.firstOrNull().orEmpty() }
                        val parsed = SubscriptionParser.parse(body, sourceId, headers)
                        if (parsed.nodes.isEmpty() && parsed.notices.isEmpty()) {
                            lastError = if (body.trimStart().startsWith("<")) {
                                "вместо подписки пришла веб-страница"
                            } else {
                                "в подписке сейчас нет серверов"
                            }
                            return@use
                        }
                        File(dir, "$sourceId.txt").writeText(body)
                        val interval = File(dir, "$sourceId.interval")
                        parsed.updateIntervalHours?.let { interval.writeText(it.toString()) } ?: interval.delete()
                        return@withContext SourceSnapshot(
                            sourceId, parsed.title, parsed.announce, parsed.notices, parsed.nodes,
                            updatedAt = System.currentTimeMillis(),
                            skipped = parsed.skipped,
                            intervalHours = parsed.updateIntervalHours,
                        )
                    }
                } catch (e: Exception) {
                    lastError = e.message ?: e.javaClass.simpleName
                }
            }
            val cached = loadCached(sourceId)
            cached?.copy(error = lastError)
                ?: SourceSnapshot(sourceId, null, null, emptyList(), emptyList(), 0, error = lastError)
        }

    fun loadCached(sourceId: String): SourceSnapshot? {
        val file = File(dir, "$sourceId.txt")
        if (!file.exists()) return null
        val parsed = SubscriptionParser.parse(file.readText(), sourceId)
        val interval = File(dir, "$sourceId.interval").takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull()
        return SourceSnapshot(
            sourceId, parsed.title, parsed.announce, parsed.notices, parsed.nodes, file.lastModified(),
            skipped = parsed.skipped,
            intervalHours = interval ?: parsed.updateIntervalHours,
        )
    }

    fun loadManual(): SourceSnapshot =
        loadCached(ProxyNode.MANUAL_SOURCE)
            ?: SourceSnapshot(ProxyNode.MANUAL_SOURCE, null, null, emptyList(), emptyList(), 0)

    @Synchronized
    fun appendManual(nodes: List<ProxyNode>): Int {
        val existing = loadManual().nodes.map { it.id }.toSet()
        val fresh = nodes.filter { it.id !in existing }
        if (fresh.isNotEmpty()) manualFile.appendText(fresh.joinToString("\n", postfix = "\n") { it.raw })
        return fresh.size
    }

    fun removeManual(nodeId: String) {
        val keep = loadManual().nodes.filter { it.id != nodeId }
        manualFile.writeText(keep.joinToString("\n", postfix = "\n") { it.raw })
    }

    fun deleteCache(sourceId: String) {
        File(dir, "$sourceId.txt").delete()
        File(dir, "$sourceId.interval").delete()
    }
}
