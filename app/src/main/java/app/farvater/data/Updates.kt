package app.farvater.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import app.farvater.BuildConfig
import app.farvater.core.xray.XrayConfigBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

@Serializable
data class UpdateAsset(val url: String, val sha256: String, val size: Long = 0)

// описание релиза из update.json, его собирает CI вместе с APK
@Serializable
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val published: String = "",
    val critical: Boolean = false,
    val notes: List<String> = emptyList(),
    val assets: Map<String, UpdateAsset> = emptyMap(),
) {
    // APK под архитектуру телефона, иначе универсальный
    fun assetForDevice(): UpdateAsset? =
        Build.SUPPORTED_ABIS.firstNotNullOfOrNull { assets[it] } ?: assets["universal"]
}

// проверка, скачивание и установка обновлений с GitHub и зеркал
class UpdateRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val dir = File(context.cacheDir, "updates").apply { mkdirs() }

    private val baseClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val tunnelClient by lazy {
        baseClient.newBuilder()
            .proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", XrayConfigBuilder.SOCKS_PORT)))
            .build()
    }

    // где лежит update.json: релиз на GitHub и копии в ветке updates
    private val manifestUrls: List<String>
        get() {
            val repo = BuildConfig.UPDATE_REPO
            return listOf(
                "https://github.com/$repo/releases/latest/download/update.json",
                "https://raw.githubusercontent.com/$repo/updates/update.json",
                "https://cdn.jsdelivr.net/gh/$repo@updates/update.json",
            )
        }

    // свежий релиз, если он новее установленной версии
    suspend fun check(viaTunnel: Boolean): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        val clients = if (viaTunnel) listOf(tunnelClient, baseClient) else listOf(baseClient)
        var lastError: Throwable? = null
        for (client in clients) for (url in manifestUrls) {
            try {
                val request = Request.Builder().url(url).header("User-Agent", "Farvater/${BuildConfig.VERSION_NAME}").build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("HTTP ${response.code}")
                    val info = json.decodeFromString(UpdateInfo.serializer(), response.body?.string().orEmpty())
                    return@withContext Result.success(info.takeIf { it.versionCode > BuildConfig.VERSION_CODE })
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        Result.failure(lastError ?: IllegalStateException("нет зеркал"))
    }

    // скачивает APK и сверяет SHA-256, onProgress получает долю от 0 до 1
    suspend fun download(info: UpdateInfo, viaTunnel: Boolean, onProgress: (Float) -> Unit): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val asset = info.assetForDevice() ?: error("в релизе нет APK для этого телефона")
                val target = File(dir, "farvater-${info.versionName}.apk")
                dir.listFiles()?.filter { it != target }?.forEach { it.delete() }
                val client = if (viaTunnel) tunnelClient else baseClient
                val request = Request.Builder().url(asset.url).header("User-Agent", "Farvater/${BuildConfig.VERSION_NAME}").build()
                val digest = MessageDigest.getInstance("SHA-256")
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("HTTP ${response.code}")
                    val body = response.body ?: error("пустой ответ")
                    val total = body.contentLength().takeIf { it > 0 } ?: asset.size
                    var read = 0L
                    body.byteStream().use { input ->
                        target.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val n = input.read(buffer)
                                if (n < 0) break
                                output.write(buffer, 0, n)
                                digest.update(buffer, 0, n)
                                read += n
                                if (total > 0) onProgress((read.toFloat() / total).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                if (!actual.equals(asset.sha256, ignoreCase = true)) {
                    target.delete()
                    error("файл повреждён или подменён: контрольная сумма не совпала")
                }
                target
            }
        }

    // может ли Фарватер ставить приложения; если нет, пользователь разрешает это в настройках
    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    fun installPermissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    // системный установщик сам проверит, что подпись совпадает с установленной
    fun installIntent(apk: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
