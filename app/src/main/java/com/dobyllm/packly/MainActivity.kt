package com.dobyllm.packly

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dobyllm.packly.core.model.ThemeMode
import com.dobyllm.packly.navigation.PacklyNavHost
import com.dobyllm.packly.notification.EXTRA_TRIP_ID
import com.dobyllm.packly.notification.createDeadlineReminderChannel
import com.dobyllm.packly.ui.theme.PacklyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createDeadlineReminderChannel(this)
        val notificationTripId = intent?.getStringExtra(EXTRA_TRIP_ID)
        setContent {
            val vm: PacklyAppViewModel = viewModel()
            val doc = vm.document.collectAsStateWithLifecycle().value
            val darkTheme = doc.settings.themeMode.resolveDarkTheme(isSystemInDarkTheme())

            PacklyTheme(
                darkTheme = darkTheme,
                dynamicColor = doc.settings.dynamicColorEnabled,
            ) {
                SyncSystemBars(darkTheme = darkTheme)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    PacklyNavHost(vm = vm, initialTripId = notificationTripId)
                }
            }
        }
    }
}

private fun ThemeMode.resolveDarkTheme(systemDarkTheme: Boolean): Boolean = when (this) {
    ThemeMode.System -> systemDarkTheme
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

@Composable
private fun SyncSystemBars(darkTheme: Boolean) {
    val view = LocalView.current
    val background = MaterialTheme.colorScheme.background.toArgb()
    val navigation = MaterialTheme.colorScheme.surface.toArgb()

    if (!view.isInEditMode) {
        DisposableEffect(darkTheme, background, navigation, view) {
            val window = (view.context as Activity).window
            window.statusBarColor = background
            window.navigationBarColor = navigation
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                window.isStatusBarContrastEnforced = false
            }
            window.syncSystemBarIconColors(view = view, darkTheme = darkTheme)
            onDispose { }
        }
    }
}

private fun Window.syncSystemBarIconColors(view: android.view.View, darkTheme: Boolean) {
    val controller = WindowCompat.getInsetsController(this, view)
    controller.isAppearanceLightStatusBars = !darkTheme
    controller.isAppearanceLightNavigationBars = !darkTheme
}
