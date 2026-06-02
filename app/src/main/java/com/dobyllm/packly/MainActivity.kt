package com.dobyllm.packly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.dobyllm.packly.navigation.PacklyNavHost
import com.dobyllm.packly.ui.theme.PacklyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PacklyTheme { PacklyNavHost() } }
    }
}
