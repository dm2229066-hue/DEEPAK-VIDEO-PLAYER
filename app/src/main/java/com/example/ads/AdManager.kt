package com.example.ads

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class AdManager(private val appContext: Context) {

    companion object {
        private const val TAG = "DeepakAdManager"
        private const val INTERSTITIAL_COOLDOWN_MS = 180_000L // 3 minutes frequency cap
        private const val MIN_PLAYER_EXITS_BEFORE_INTERSTITIAL = 2
    }

    private val isMobileAdsInitialized = AtomicBoolean(false)

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false
    private var lastInterstitialShownAt = 0L
    private var playerExitCount = 0

    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading = false

    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _isPrivacyOptionsRequired = MutableStateFlow(false)
    val isPrivacyOptionsRequired: StateFlow<Boolean> = _isPrivacyOptionsRequired.asStateFlow()

    private val _isRewardedAdReady = MutableStateFlow(false)
    val isRewardedAdReady: StateFlow<Boolean> = _isRewardedAdReady.asStateFlow()

    fun gatherConsentAndInitialize(activity: Activity) {
        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()

        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { loadAndShowError ->
                    if (loadAndShowError != null) {
                        Log.w(TAG, "UMP form error: ${loadAndShowError.message}")
                    }
                    updateConsentState(consentInfo)
                }
            },
            { requestError ->
                Log.w(TAG, "UMP info update error: ${requestError.message}")
                updateConsentState(consentInfo)
            }
        )

        if (consentInfo.canRequestAds()) {
            initializeMobileAdsIfNeeded()
        }
    }

    private fun updateConsentState(consentInfo: ConsentInformation) {
        _isPrivacyOptionsRequired.value =
            consentInfo.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (consentInfo.canRequestAds()) {
            _canRequestAds.value = true
            initializeMobileAdsIfNeeded()
        }
    }

    fun showPrivacyOptionsForm(activity: Activity, onDismissed: (String?) -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
            updateConsentState(consentInfo)
            onDismissed(formError?.message)
        }
    }

    private val adScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun initializeMobileAdsIfNeeded() {
        if (isMobileAdsInitialized.getAndSet(true)) return
        adScope.launch {
            runCatching {
                MobileAds.initialize(appContext) {
                    adScope.launch {
                        withContext(Dispatchers.Main) {
                            _canRequestAds.value = true
                            preloadInterstitial()
                            preloadRewarded()
                        }
                    }
                }
            }.onFailure {
                isMobileAdsInitialized.set(false)
                Log.e(TAG, "MobileAds init failed: ${it.message}")
            }
        }
    }

    fun preloadInterstitial() {
        if (!_canRequestAds.value || interstitialAd != null || isInterstitialLoading || !isNetworkAvailable()) {
            return
        }
        isInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            appContext,
            BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isInterstitialLoading = false
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isInterstitialLoading = false
                    interstitialAd = null
                    Log.d(TAG, "Interstitial failed to load: ${error.message}")
                }
            }
        )
    }

    /**
     * Called ONLY after returning from the video player back to the library.
     * Enforces frequency capping: at least 2 completed/exited player sessions AND >= 3 min cooldown.
     * Never interrupts active playback and never triggers on every video open.
     */
    fun onReturnFromPlayer(activity: Activity, onComplete: () -> Unit) {
        playerExitCount++
        val now = System.currentTimeMillis()
        val cooldownPassed = (now - lastInterstitialShownAt) >= INTERSTITIAL_COOLDOWN_MS
        val ad = interstitialAd
        if (_canRequestAds.value &&
            ad != null &&
            playerExitCount >= MIN_PLAYER_EXITS_BEFORE_INTERSTITIAL &&
            cooldownPassed
        ) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    lastInterstitialShownAt = System.currentTimeMillis()
                    playerExitCount = 0
                    preloadInterstitial()
                    onComplete()
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    interstitialAd = null
                    preloadInterstitial()
                    onComplete()
                }
            }
            ad.show(activity)
        } else {
            if (interstitialAd == null) preloadInterstitial()
            onComplete()
        }
    }

    fun preloadRewarded() {
        if (!_canRequestAds.value || rewardedAd != null || isRewardedLoading || !isNetworkAvailable()) {
            return
        }
        isRewardedLoading = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            appContext,
            BuildConfig.ADMOB_REWARDED_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isRewardedLoading = false
                    rewardedAd = ad
                    _isRewardedAdReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isRewardedLoading = false
                    rewardedAd = null
                    _isRewardedAdReady.value = false
                    Log.d(TAG, "Rewarded failed to load: ${error.message}")
                }
            }
        )
    }

    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onUnavailable: (String) -> Unit
    ) {
        if (!isNetworkAvailable()) {
            onUnavailable("Connect to the internet to load a rewarded ad.")
            return
        }
        val ad = rewardedAd
        if (ad == null) {
            preloadRewarded()
            onUnavailable("Rewarded ad is still loading. Please try again in a moment.")
            return
        }

        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _isRewardedAdReady.value = false
                preloadRewarded()
                if (earned) {
                    onRewardEarned()
                }
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                rewardedAd = null
                _isRewardedAdReady.value = false
                preloadRewarded()
                onUnavailable("Unable to display ad right now.")
            }
        }
        ad.show(activity) {
            earned = true
        }
    }

    fun isNetworkAvailable(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

@Composable
fun AdBannerBar(
    canRequestAds: Boolean,
    modifier: Modifier = Modifier
) {
    if (!canRequestAds) return

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val adWidthDp = configuration.screenWidthDp.coerceAtLeast(320)
    var isAdLoaded by remember { mutableStateOf(false) }

    val adView = remember(adWidthDp) {
        AdView(context).apply {
            adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
            setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp)
            )
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    isAdLoaded = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isAdLoaded = false
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(adView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adView.resume()
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    if (isAdLoaded) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { adView },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
