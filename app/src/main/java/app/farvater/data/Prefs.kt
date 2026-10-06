package app.farvater.data

import android.content.Context
import app.farvater.core.catalog.BuiltInCatalog
import app.farvater.core.model.ProxyNode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

// версия условий использования
const val LEGAL_VERSION = 1

@Serializable
data class AppSettings(
    // банки и госсервисы мимо VPN
    val directRuServices: Boolean = true,
    val autoSwitch: Boolean = true,
    val hideDead: Boolean = false,
    val testUrl: String = "https://www.gstatic.com/generate_204",
    val concurrency: Int = 16,
    val onboardingDone: Boolean = false,
    val communityEnabled: Boolean = false,
    val acceptedLegalVersion: Int = 0,
    // прятать узлы без шифрования, со слабым шифрованием или без проверки сертификата
    val safeMode: Boolean = true,
    // DNS-запросы идут через узел по HTTPS, сервер не может их подменить
    val encryptedDns: Boolean = true,
    // отправлять HWID своим подпискам: панели с лимитом устройств без него урезают подписку
    val sendHwid: Boolean = true,
    // проверять обновления в фоне и присылать уведомление
    val autoUpdates: Boolean = true,
    // приложения мимо VPN, выбирает пользователь
    val bypassApps: Set<String> = emptySet(),
)

@Serializable
data class UserSource(val id: String, val url: String, val title: String)

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("farvater", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // значения держим в памяти: их читают на каждом пересчёте списка
    @Volatile private var cachedSettings: AppSettings? = null
    @Volatile private var cachedEnabled: Set<String>? = null
    @Volatile private var cachedUserSources: List<UserSource>? = null

    var settings: AppSettings
        get() = cachedSettings ?: (read<AppSettings>("settings") ?: AppSettings()).also { cachedSettings = it }
        set(value) {
            cachedSettings = value
            write("settings", value)
        }

    // включённые источники каталога
    var enabledSources: Set<String>
        get() = cachedEnabled
            ?: (sp.getStringSet("enabled_sources", null)?.toSet() ?: BuiltInCatalog.sources.map { it.id }.toSet())
                .also { cachedEnabled = it }
        set(value) {
            cachedEnabled = value
            sp.edit().putStringSet("enabled_sources", value).apply()
        }

    var userSources: List<UserSource>
        get() = cachedUserSources ?: (read<List<UserSource>>("user_sources") ?: emptyList()).also { cachedUserSources = it }
        set(value) {
            cachedUserSources = value
            write("user_sources", value)
        }

    // случайный идентификатор установки для подписок с лимитом устройств, не связан с железом телефона
    val hwid: String
        get() = sp.getString("hwid", null) ?: java.util.UUID.randomUUID().toString().replace("-", "")
            .also { sp.edit().putString("hwid", it).apply() }

    var lastUpdateCheck: Long
        get() = sp.getLong("last_update_check", 0)
        set(value) = sp.edit().putLong("last_update_check", value).apply()

    // последняя версия, о которой уже пришло уведомление
    var notifiedVersion: Int
        get() = sp.getInt("notified_version", 0)
        set(value) = sp.edit().putInt("notified_version", value).apply()

    var selectedNodeId: String?
        get() = sp.getString("selected_node", null)
        set(value) = sp.edit().putString("selected_node", value).apply()

    // узел, к которому подключается сервис
    var lastNode: ProxyNode?
        get() = read<ProxyNode>("last_node")
        set(value) = write("last_node", value)

    private inline fun <reified T> read(key: String): T? =
        sp.getString(key, null)?.let { runCatching { json.decodeFromString(serializer<T>(), it) }.getOrNull() }

    private inline fun <reified T> write(key: String, value: T?) {
        sp.edit().putString(key, value?.let { json.encodeToString(serializer<T>(), it) }).apply()
    }
}
