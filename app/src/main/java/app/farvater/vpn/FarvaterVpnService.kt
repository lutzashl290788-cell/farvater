package app.farvater.vpn

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import app.farvater.App
import app.farvater.MainActivity
import app.farvater.R
import app.farvater.core.model.ProxyNode
import app.farvater.core.xray.XrayConfigBuilder
import app.farvater.engine.TunBridge
import app.farvater.engine.XrayEngine
import app.farvater.ui.formatSpeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

class FarvaterVpnService : VpnService() {

    companion object {
        const val ACTION_START = "app.farvater.action.START"
        const val ACTION_STOP = "app.farvater.action.STOP"
        const val CHANNEL = "vpn"
        private const val NOTIFICATION_ID = 7
        private const val MTU = 8500

        fun start(context: Context, node: ProxyNode) {
            App.prefs.lastNode = node
            context.startForegroundService(Intent(context, FarvaterVpnService::class.java).setAction(ACTION_START))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, FarvaterVpnService::class.java).setAction(ACTION_STOP))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tun: ParcelFileDescriptor? = null
    private var monitor: Job? = null

    private val probeClient by lazy {
        OkHttpClient.Builder()
            .proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", XrayConfigBuilder.SOCKS_PORT)))
            .callTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }
        // запуск из приложения, плитки или Always-on
        val node = App.prefs.lastNode
        startForegroundCompat(notification(node?.name ?: "Подключение", "Поднимаю туннель"))
        if (node == null) {
            fail("Сначала выберите сервер в приложении", null)
            return START_NOT_STICKY
        }
        scope.launch { startVpn(node) }
        return START_STICKY
    }

    private fun startVpn(node: ProxyNode) {
        synchronized(this) { connectLocked(node) }
    }

    private fun connectLocked(node: ProxyNode) {
        teardown()
        VpnBus.state.value = VpnState.Connecting(node)
        val settings = App.prefs.settings

        val config = XrayConfigBuilder.build(node, settings.directRuServices, encryptedDns = settings.encryptedDns)
            ?: return fail("${node.protocol.title} пока не поддерживается — выберите другой узел", node)
        XrayEngine.start(config).onFailure { return fail("Ядро не запустилось: ${it.message}", node) }

        val builder = Builder()
            .setSession(node.name)
            .setMtu(MTU)
            .addAddress("10.10.14.1", 30)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("1.1.1.1")
        // приложение идёт мимо туннеля
        runCatching { builder.addDisallowedApplication(packageName) }
        settings.bypassApps.forEach { pkg -> runCatching { builder.addDisallowedApplication(pkg) } }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) builder.setMetered(false)

        val fd = builder.establish() ?: return fail("Нет разрешения на VPN", node)
        tun = fd
        TunBridge.start(this, fd.fd, MTU).onFailure { return fail("TUN не запустился: ${it.message}", node) }

        VpnBus.state.value = VpnState.Connected(node, System.currentTimeMillis())
        updateNotification(node.name, "Подключено")
        monitor = scope.launch { monitorLoop(node) }
    }

    private suspend fun monitorLoop(node: ProxyNode) {
        var lastTx = -1L
        var lastRx = -1L
        var tick = 0
        var failures = 0
        val history = ArrayDeque<Long>()
        while (scope.isActive) {
            delay(1000)
            tick++
            TunBridge.stats()?.takeIf { it.size >= 4 }?.let { s ->
                if (lastTx >= 0) {
                    val up = (s[1] - lastTx).coerceAtLeast(0)
                    val down = (s[3] - lastRx).coerceAtLeast(0)
                    VpnBus.traffic.value = Traffic(up, down, s[1], s[3])
                    history.addLast(up + down)
                    if (history.size > 60) history.removeFirst()
                    VpnBus.speedHistory.value = history.toList()
                }
                lastTx = s[1]
                lastRx = s[3]
            }
            if (tick % 5 == 0) {
                val t = VpnBus.traffic.value
                updateNotification(node.name, "↓ ${formatSpeed(t.downBps)}   ↑ ${formatSpeed(t.upBps)}")
            }
            if (tick % 45 == 0) {
                failures = if (probe()) 0 else failures + 1
                VpnBus.failedChecks.value = failures
            }
        }
    }

    private fun probe(): Boolean = runCatching {
        val request = Request.Builder().url(App.prefs.settings.testUrl).build()
        probeClient.newCall(request).execute().use { it.code == 204 || it.code == 200 }
    }.getOrDefault(false)

    private fun teardown() {
        monitor?.cancel()
        monitor = null
        TunBridge.stop()
        XrayEngine.stop()
        runCatching { tun?.close() }
        tun = null
        VpnBus.traffic.value = Traffic()
        VpnBus.speedHistory.value = emptyList()
        VpnBus.failedChecks.value = 0
    }

    private fun stopVpn() {
        synchronized(this) { teardown() }
        VpnBus.state.value = VpnState.Idle
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun fail(message: String, node: ProxyNode?) {
        teardown()
        VpnBus.state.value = VpnState.Failed(message, node)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() = stopVpn()

    override fun onDestroy() {
        synchronized(this) { teardown() }
        scope.cancel()
        if (VpnBus.state.value !is VpnState.Failed) VpnBus.state.value = VpnState.Idle
        super.onDestroy()
    }

    private fun notification(title: String, text: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, FarvaterVpnService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_beacon)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null as Icon?, "Отключить", stop).build())
            .build()
    }

    private fun updateNotification(title: String, text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(title, text))
    }

    private fun startForegroundCompat(n: Notification) {
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, n)
        }
    }
}
