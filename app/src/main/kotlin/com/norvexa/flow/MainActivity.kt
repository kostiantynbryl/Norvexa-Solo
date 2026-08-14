package com.norvexa.flow

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.norvexa.flow.ui.MainViewModel
import com.norvexa.flow.ui.NorvexaFlowApp
import com.norvexa.flow.ui.theme.NorvexaFlowTheme

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    private val viewModel: MainViewModel by viewModels {
        val app = application as NorvexaFlowApplication
        MainViewModel.Factory(app.container.repository, app.container.settingsStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            LaunchedEffect(settings.onboardingCompleted) {
                if (settings.onboardingCompleted && Build.VERSION.SDK_INT >= 33) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            LaunchedEffect(settings.privacyMode) {
                if (settings.privacyMode) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            NorvexaFlowTheme(settings.darkMode) {
                NorvexaFlowApp(viewModel)
            }
        }
    }
}
