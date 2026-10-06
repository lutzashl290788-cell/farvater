package app.farvater

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import app.farvater.core.xray.LocalProxyAuth
import app.farvater.data.Prefs
import app.farvater.data.SubscriptionRepository
import app.farvater.data.UpdateRepository
import app.farvater.data.UpdateWorker
import app.farvater.engine.XrayEngine
import app.farvater.vpn.FarvaterVpnService
import java.net.Authenticator
import java.net.PasswordAuthentication

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = Prefs(this)
        repo = SubscriptionRepository(this)
        XrayEngine.init(this)
        Authenticator.setDefault(object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication? =
                if (requestingProtocol?.startsWith("SOCKS") == true &&
                    (requestingHost == "127.0.0.1" || requestingSite?.isLoopbackAddress == true)
                ) {
                    PasswordAuthentication(LocalProxyAuth.user, LocalProxyAuth.pass.toCharArray())
                } else {
                    null
                }
        })
        updates = UpdateRepository(this)
        getSystemService(NotificationManager::class.java).apply {
            createNotificationChannel(NotificationChannel(FarvaterVpnService.CHANNEL, "Подключение", NotificationManager.IMPORTANCE_LOW))
            createNotificationChannel(NotificationChannel(UpdateWorker.CHANNEL, "Обновления", NotificationManager.IMPORTANCE_HIGH))
            deleteNotificationChannel("updates")
        }
        UpdateWorker.schedule(this)
    }

    companion object {
        lateinit var instance: App
            private set
        lateinit var prefs: Prefs
            private set
        lateinit var repo: SubscriptionRepository
            private set
        lateinit var updates: UpdateRepository
            private set
    }
}
