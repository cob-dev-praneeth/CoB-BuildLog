package org.childrenofbharat.buildlog

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import org.childrenofbharat.buildlog.overlay.CaptureOverlayService
import org.childrenofbharat.buildlog.ui.BuildLogApp
import org.childrenofbharat.buildlog.ui.BuildLogViewModel
import org.childrenofbharat.buildlog.ui.theme.BuildLogTheme

class MainActivity : ComponentActivity() {
    private val app get() = application as BuildLogApplication
    private val viewModel by viewModels<BuildLogViewModel> { BuildLogViewModel.Factory(app.repository) }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val quickCaptureRequests = MutableStateFlow(0)
    private val overlayPermission = MutableStateFlow(false)
    private var overlayStartPending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        overlayPermission.value = Settings.canDrawOverlays(this)
        if (intent.getBooleanExtra(EXTRA_QUICK_CAPTURE, false)) quickCaptureRequests.value++
        setContent {
            val quickCaptureRequest by quickCaptureRequests.collectAsStateWithLifecycle()
            val overlayAllowed by overlayPermission.collectAsStateWithLifecycle()
            BuildLogTheme {
                BuildLogApp(
                    viewModel = viewModel,
                    quickCaptureRequest = quickCaptureRequest,
                    overlayAllowed = overlayAllowed,
                    onEnableOverlay = ::enableOverlay
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_QUICK_CAPTURE, false)) quickCaptureRequests.value++
    }

    override fun onResume() {
        super.onResume()
        val allowed = Settings.canDrawOverlays(this)
        overlayPermission.value = allowed
        if (overlayStartPending && allowed) {
            overlayStartPending = false
            startOverlay()
        }
    }

    private fun enableOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            overlayStartPending = true
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        startOverlay()
    }

    private fun startOverlay() {
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        CaptureOverlayService.start(this)
    }

    companion object { const val EXTRA_QUICK_CAPTURE = "quick_capture" }
}
