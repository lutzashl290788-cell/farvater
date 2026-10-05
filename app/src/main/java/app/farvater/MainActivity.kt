package app.farvater

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.farvater.data.UpdateWorker
import app.farvater.ui.MainViewModel
import app.farvater.ui.FarvaterRoot
import app.farvater.ui.UiEvent
import app.farvater.ui.theme.FarvaterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        vm.onVpnPermissionResult(it.resultCode == RESULT_OK)
    }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.events.collect { event ->
                    when (event) {
                        is UiEvent.RequestVpnPermission -> vpnPermission.launch(event.intent)
                        is UiEvent.StartActivity -> runCatching { startActivity(event.intent) }
                    }
                }
            }
        }

        if (savedInstanceState == null) handleDeepLink(intent)
        setContent { FarvaterTheme { FarvaterRoot(vm) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    // импорт по ссылке farvater://import
    private fun handleDeepLink(intent: Intent?) {
        if (intent?.getBooleanExtra(UpdateWorker.EXTRA_OPEN_UPDATE, false) == true) vm.openUpdate()
        val uri = intent?.data ?: return
        if (uri.scheme == "farvater" && uri.host == "import") {
            uri.getQueryParameter("url")?.takeIf { it.isNotBlank() }?.let(vm::requestImport)
        }
    }
}
