package com.example

import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.DeepakAppRoot
import com.example.ui.theme.DeepakVideoPlayerTheme
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.PlayerViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val libraryViewModel: LibraryViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()

    private var externalVideoUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as DeepakPlayerApp
        app.adManager.gatherConsentAndInitialize(this)

        handleIncomingIntent(intent)

        setContent {
            val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
            val prefs = libraryState.preferences
            val localizedContext = remember(prefs.appLanguage.code) {
                createLocalizedContext(this, prefs.appLanguage.code)
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalActivityResultRegistryOwner provides this,
                LocalOnBackPressedDispatcherOwner provides this
            ) {
                DeepakVideoPlayerTheme(
                    darkTheme = prefs.isDarkTheme,
                    accentOption = prefs.accentOption
                ) {
                    DeepakAppRoot(
                        libraryViewModel = libraryViewModel,
                        playerViewModel = playerViewModel,
                        externalOpenUri = externalVideoUri,
                        onConsumedExternalUri = { externalVideoUri = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                externalVideoUri = uri
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.O until Build.VERSION_CODES.S) {
            val state = playerViewModel.uiState.value
            if (state.isPlaying && state.currentVideo != null) {
                val w = state.videoWidth
                val h = state.videoHeight
                val ratio = if (w <= 0 || h <= 0) {
                    Rational(16, 9)
                } else {
                    val f = w.toFloat() / h.toFloat()
                    when {
                        f > 2.35f -> Rational(235, 100)
                        f < (1f / 2.35f) -> Rational(100, 235)
                        else -> Rational(w, h)
                    }
                }
                runCatching {
                    enterPictureInPictureMode(
                        PictureInPictureParams.Builder()
                            .setAspectRatio(ratio)
                            .build()
                    )
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        playerViewModel.setPiPActive(isInPictureInPictureMode)
    }

    private fun createLocalizedContext(base: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        val localizedResources = base.createConfigurationContext(config).resources
        return object : ContextWrapper(base) {
            override fun getResources(): Resources = localizedResources
            override fun getAssets(): AssetManager = localizedResources.assets
        }
    }
}
