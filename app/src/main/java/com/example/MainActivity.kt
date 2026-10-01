package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.navigation.AppNavHost
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var deepLinkedInvoiceId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLinkedInvoiceId = extractInvoiceId(intent)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(initialInvoiceId = deepLinkedInvoiceId)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        extractInvoiceId(intent)?.let { id ->
            deepLinkedInvoiceId = id
        }
    }

    private fun extractInvoiceId(intent: Intent?): Long? {
        val uri = intent?.data ?: return null
        return try {
            uri.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }
}
