package com.dobyllm.packly

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.dobyllm.packly.core.model.PACKLY_DRIVE_SCOPE
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
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
import com.dobyllm.packly.navigation.PacklyNavHost
import com.dobyllm.packly.notification.EXTRA_TRIP_ID
import com.dobyllm.packly.notification.createDeadlineReminderChannel
import com.dobyllm.packly.ui.i18n.PacklyLocalizedContent
import com.dobyllm.packly.ui.theme.PacklyTheme

class MainActivity : ComponentActivity() {
    private lateinit var authorizationClient: AuthorizationClient
    private lateinit var packlyViewModel: PacklyAppViewModel
    private lateinit var connectivityManager: ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            packlyViewModel.syncIfGoogleDriveEnabled()
        }
    }

    private val driveAuthorizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val data = result.data
        if (data == null && result.resultCode == RESULT_CANCELED) {
            packlyViewModel.onGoogleDriveAuthorizationCancelled()
            return@registerForActivityResult
        }
        if (data == null) {
            packlyViewModel.onGoogleDriveAuthorizationDataMissing()
            return@registerForActivityResult
        }
        runCatching { authorizationClient.getAuthorizationResultFromIntent(data) }
            .onSuccess { persistDriveAuthorizationResult(it) }
            .onFailure { packlyViewModel.onGoogleDriveAuthorizationParserFailed() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        packlyViewModel = ViewModelProvider(this)[PacklyAppViewModel::class.java]
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        authorizationClient = Identity.getAuthorizationClient(this)
        createDeadlineReminderChannel(this)
        val notificationTripId = intent?.getStringExtra(EXTRA_TRIP_ID)
        setContent {
            val vm = packlyViewModel
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
                        PacklyNavHost(
                            vm = vm,
                            initialTripId = notificationTripId,
                            onGoogleDriveSyncClick = { requestGoogleDriveAuthorization() },
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder()
                .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build(),
            networkCallback,
        )
        packlyViewModel.syncIfGoogleDriveEnabled()
    }

    override fun onStop() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
        super.onStop()
    }

    private fun requestGoogleDriveAuthorization() {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(PACKLY_DRIVE_SCOPE)))
            .build()
        authorizationClient.authorize(request)
            .addOnSuccessListener { result ->
                val pendingIntent = result.pendingIntent
                if (result.hasResolution() && pendingIntent != null) {
                    driveAuthorizationLauncher.launch(
                        IntentSenderRequest.Builder(pendingIntent.intentSender).build(),
                    )
                } else {
                    persistDriveAuthorizationResult(result)
                }
            }
            .addOnFailureListener { packlyViewModel.onGoogleDriveAuthorizationParserFailed() }
    }

    private fun persistDriveAuthorizationResult(result: AuthorizationResult) {
        val granted = result.grantedScopes.any { it == PACKLY_DRIVE_SCOPE }
        val tokenPresent = !result.accessToken.isNullOrBlank()
        when {
            !granted -> packlyViewModel.onGoogleDriveAuthorizationScopeDenied()
            !tokenPresent -> packlyViewModel.onGoogleDriveAuthorizationBlankToken()
            else -> packlyViewModel.onGoogleDriveAuthorized()
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
