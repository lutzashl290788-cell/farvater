package app.farvater.engine

import android.content.Context
import app.farvater.core.xray.LocalProxyAuth
import app.farvater.core.xray.XrayConfigBuilder
import hev.sockstun.TProxyService
import java.io.File

// мост TUN в SOCKS5 через hev-socks5-tunnel
object TunBridge {
    val isAvailable: Boolean by lazy {
        runCatching { System.loadLibrary("hev-socks5-tunnel") }.isSuccess
    }

    private var running = false

    @Synchronized
    fun start(context: Context, fd: Int, mtu: Int): Result<Unit> = runCatching {
        check(isAvailable) { "в сборке нет libhev-socks5-tunnel.so" }
        val config = File(context.filesDir, "hev.yml")
        config.writeText(
            """
            tunnel:
              mtu: $mtu
            socks5:
              port: ${XrayConfigBuilder.SOCKS_PORT}
              address: 127.0.0.1
              udp: 'udp'
              username: '${LocalProxyAuth.user}'
              password: '${LocalProxyAuth.pass}'
            misc:
              task-stack-size: 81920
              log-level: warn
            """.trimIndent(),
        )
        check(TProxyService.TProxyStartService(config.absolutePath, fd)) { "hev-socks5-tunnel не принял конфиг" }
        running = true
    }

    @Synchronized
    fun stop() {
        if (!running) return
        runCatching { TProxyService.TProxyStopService() }
        running = false
    }

    // tx_packets, tx_bytes, rx_packets, rx_bytes или null
    fun stats(): LongArray? =
        if (running) runCatching { TProxyService.TProxyGetStats() }.getOrNull() else null
}
