package com.example.farmnavigator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.farmnavigator.ui.FarmNavigatorApp
import com.example.farmnavigator.ui.theme.FarmNavigatorTheme
import com.example.farmnavigator.viewmodel.FarmViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Use a consistent theme for the application
            FarmNavigatorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FarmNavigatorScreen()
                }
            }
        }
    }
}

@Composable
fun FarmNavigatorScreen(viewModel: FarmViewModel = viewModel()) {
    FarmNavigatorApp(viewModel)
}
