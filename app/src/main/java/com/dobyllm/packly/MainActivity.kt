package com.dobyllm.packly

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobyllm.packly.navigation.PacklyNavHost
import com.dobyllm.packly.notification.EXTRA_TRIP_ID
import com.dobyllm.packly.notification.createDeadlineReminderChannel
import com.dobyllm.packly.ui.i18n.PacklyLocalizedContent
import com.dobyllm.packly.ui.theme.PacklyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createDeadlineReminderChannel(this)
        val notificationTripId = intent?.getStringExtra(EXTRA_TRIP_ID)
        setContent {
            val vm: PacklyAppViewModel = viewModel()
            val doc = vm.document.collectAsStateWithLifecycle().value

            PacklyLocalizedContent(languagePreference = doc.settings.languagePreference) {
                PacklyTheme(
                    darkTheme = false,
                    dynamicColor = false,
                ) {
                    SyncSystemBars()
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
}

@Composable
private fun SyncSystemBars() {
    val view = LocalView.current
    val background = MaterialTheme.colorScheme.background.toArgb()
    val navigation = MaterialTheme.colorScheme.surface.toArgb()

    if (!view.isInEditMode) {
        DisposableEffect(background, navigation, view) {
            val window = (view.context as Activity).window
            window.statusBarColor = background
            window.navigationBarColor = navigation
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                window.isStatusBarContrastEnforced = false
            }
            window.syncLightSystemBarIconColors(view = view)
            onDispose { }
        }
    }
}

private fun Window.syncLightSystemBarIconColors(view: android.view.View) {
    val controller = WindowCompat.getInsetsController(this, view)
    controller.isAppearanceLightStatusBars = true
    controller.isAppearanceLightNavigationBars = true
}
