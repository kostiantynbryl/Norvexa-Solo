package com.norvexa.flow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.norvexa.flow.ui.NorvexaFlowApp
import com.norvexa.flow.ui.theme.NorvexaFlowTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NorvexaFlowTheme {
                NorvexaFlowApp()
            }
        }
    }
}
