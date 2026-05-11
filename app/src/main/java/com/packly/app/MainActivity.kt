package com.packly.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.packly.app.ui.navigation.PacklyNavGraph
import com.packly.app.ui.theme.PacklyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PacklyTheme {
                val navController = rememberNavController()
                val app = application as PacklyApplication
                PacklyNavGraph(
                    navController = navController,
                    repository = app.appModule.repository
                )
            }
        }
    }
}
