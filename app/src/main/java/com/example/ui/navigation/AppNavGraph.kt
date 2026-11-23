package com.example.ui.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.DeepakPlayerApp
import com.example.R
import com.example.data.media.DeviceVideoScanner
import com.example.data.model.VideoItem
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.PlaylistPickerSheet
import com.example.ui.components.RenameVideoDialog
import com.example.ui.components.SortAndViewSheet
import com.example.ui.components.SplashScreenOverlay
import com.example.ui.components.VideoContextBottomSheet
import com.example.ui.components.VideoInfoDialog
import com.example.ui.player.PlayerScreen
import com.example.ui.player.findActivity
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NetworkStreamScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideosScreen
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object VideosRoute
@Serializable object FoldersRoute
@Serializable object PlaylistsRoute
@Serializable object SettingsRoute
@Serializable object NetworkStreamRoute
@Serializable object PrivacyPolicyRoute
@Serializable object PlayerRoute

private data class TopTab(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topTabs = listOf(
    TopTab(HomeRoute, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    TopTab(VideosRoute, R.string.nav_videos, Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle, "nav_videos"),
    TopTab(FoldersRoute, R.string.nav_folders, Icons.Filled.Folder, Icons.Outlined.Folder, "nav_folders"),
    TopTab(PlaylistsRoute, R.string.nav_playlists, Icons.AutoMirrored.Filled.QueueMusic, Icons.AutoMirrored.Outlined.QueueMusic, "nav_playlists"),
    TopTab(SettingsRoute, R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
)

@Composable
fun DeepakAppRoot(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    externalOpenUri: Uri?,
    onConsumedExternalUri: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as DeepakPlayerApp
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val inspectedMetadata by libraryViewModel.inspectedMetadata.collectAsStateWithLifecycle()
    val canRequestAds by app.adManager.canRequestAds.collectAsStateWithLifecycle()
    val isPrivacyOptionsRequired by app.adManager.isPrivacyOptionsRequired.collectAsStateWithLifecycle()

    var showSplash by rememberSaveable { mutableStateOf(true) }
    var selectedFolderPath by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }

    var showSortSheet by remember { mutableStateOf(false) }
    var contextSheetVideo by remember { mutableStateOf<VideoItem?>(null) }
    var playlistPickerVideo by remember { mutableStateOf<VideoItem?>(null) }
    var renameTargetVideo by remember { mutableStateOf<VideoItem?>(null) }
    var pendingRenameNewName by remember { mutableStateOf<String?>(null) }
    var deleteTargetVideo by remember { mutableStateOf<VideoItem?>(null) }
    var pendingDeleteUri by remember { mutableStateOf<Uri?>(null) }

    fun navigateToPlayer(queue: List<VideoItem>, index: Int) {
        playerViewModel.startPlayback(queue, index)
        navController.navigate(PlayerRoute) {
            launchSingleTop = true
        }
    }

    // Handle external ACTION_VIEW video URI
    LaunchedEffect(externalOpenUri) {
        if (externalOpenUri != null) {
            val item = libraryViewModel.resolveSingleUri(externalOpenUri)
            navigateToPlayer(listOf(item), 0)
            onConsumedExternalUri()
        }
    }

    // Activity Result Launchers
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        libraryViewModel.refreshPermissionAndScan()
        if (!granted) {
            scope.launch {
                snackbarHostState.showSnackbar("Permission denied. You can still browse files via SAF.")
            }
        }
    }

    val safSingleVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            scope.launch {
                val item = libraryViewModel.resolveSingleUri(uri)
                navigateToPlayer(listOf(item), 0)
            }
        }
    }

    val safFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(treeUri, flags)
            }.onFailure {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            val label = DocumentFile.fromTreeUri(context, treeUri)?.name ?: "External Folder"
            libraryViewModel.addSafFolder(treeUri, label)
            scope.launch { snackbarHostState.showSnackbar("Mounted folder: $label") }
        }
    }

    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            playerViewModel.loadExternalSubtitle(uri)
        }
    }

    val deleteSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingDeleteUri?.let { libraryViewModel.refreshVideos() }
            scope.launch { snackbarHostState.showSnackbar("Video deleted") }
        }
        pendingDeleteUri = null
    }

    val writeSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val uri = renameTargetVideo?.let { Uri.parse(it.uri) }
        val newName = pendingRenameNewName
        if (result.resultCode == Activity.RESULT_OK && uri != null && !newName.isNullOrBlank()) {
            libraryViewModel.renameVideoDirect(uri, newName) { ok ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (ok) "Renamed to $newName" else "Could not rename this file on this Android version."
                    )
                }
            }
        }
        renameTargetVideo = null
        pendingRenameNewName = null
    }

    fun requestMediaPermission() {
        permissionLauncher.launch(DeviceVideoScanner.requiredMediaPermission())
    }

    fun browseSafVideos() {
        safSingleVideoLauncher.launch(arrayOf("video/*", "application/x-matroska"))
    }

    fun shareVideo(video: VideoItem) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = video.mimeType.ifBlank { "video/*" }
            putExtra(Intent.EXTRA_STREAM, Uri.parse(video.uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(Intent.createChooser(shareIntent, "Share ${video.title}"))
        }.onFailure {
            scope.launch { snackbarHostState.showSnackbar("Cannot share this media source.") }
        }
    }

    fun performDelete(video: VideoItem) {
        val uri = Uri.parse(video.uri)
        val sender = libraryViewModel.createDeleteSender(uri)
        if (sender != null) {
            pendingDeleteUri = uri
            deleteSenderLauncher.launch(IntentSenderRequest.Builder(sender).build())
        } else {
            libraryViewModel.deleteVideoDirect(uri) { ok ->
                scope.launch {
                    snackbarHostState.showSnackbar(if (ok) "Video deleted" else "Delete not permitted for this file.")
                }
            }
        }
    }

    fun performRename(video: VideoItem, newName: String) {
        val uri = Uri.parse(video.uri)
        val sender = libraryViewModel.createWriteSender(uri)
        if (sender != null) {
            pendingRenameNewName = newName
            writeSenderLauncher.launch(IntentSenderRequest.Builder(sender).build())
        } else {
            libraryViewModel.renameVideoDirect(uri, newName) { ok ->
                scope.launch {
                    snackbarHostState.showSnackbar(if (ok) "Renamed to $newName" else "Rename not supported for this file.")
                }
            }
            renameTargetVideo = null
        }
    }

    val isPlayerRoute = currentDest?.hasRoute(PlayerRoute::class) == true
    val showTopLevelNav = !isPlayerRoute &&
        currentDest?.hasRoute(NetworkStreamRoute::class) != true &&
        currentDest?.hasRoute(PrivacyPolicyRoute::class) != true

    fun navigateTopTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = if (isPlayerRoute) WindowInsets(0, 0, 0, 0) else WindowInsets.safeDrawing,
                bottomBar = {
                    if (showTopLevelNav && !useNavigationRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            topTabs.forEach { tab ->
                                val selected = currentDest?.hasRoute(tab.route::class) == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(tab.labelRes), maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isPlayerRoute) androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
                ) {
                    if (showTopLevelNav && useNavigationRail) {
                        NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                            topTabs.forEach { tab ->
                                val selected = currentDest?.hasRoute(tab.route::class) == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(tab.labelRes), maxLines = 1) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = HomeRoute,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable<HomeRoute> {
                            HomeScreen(
                                uiState = libraryState,
                                canRequestAds = canRequestAds,
                                onPlayVideo = ::navigateToPlayer,
                                onOpenNetworkStream = { navController.navigate(NetworkStreamRoute) },
                                onOpenFolder = { folder ->
                                    selectedFolderPath = folder.path
                                    navigateTopTab(FoldersRoute)
                                },
                                onOpenPlaylist = { id ->
                                    selectedPlaylistId = id
                                    navigateTopTab(PlaylistsRoute)
                                },
                                onSeeAllVideos = { navigateTopTab(VideosRoute) },
                                onSearchQueryChange = libraryViewModel::onSearchQueryChange,
                                onToggleViewMode = libraryViewModel::toggleViewMode,
                                onOpenSortSheet = { showSortSheet = true },
                                onRefresh = libraryViewModel::refreshVideos,
                                onRequestPermission = ::requestMediaPermission,
                                onBrowseSaf = ::browseSafVideos,
                                onToggleFavorite = libraryViewModel::toggleFavorite,
                                onMoreVideoOptions = { contextSheetVideo = it }
                            )
                        }

                        composable<VideosRoute> {
                            VideosScreen(
                                uiState = libraryState,
                                canRequestAds = canRequestAds,
                                onPlayVideo = ::navigateToPlayer,
                                onSearchQueryChange = libraryViewModel::onSearchQueryChange,
                                onFilterSelected = libraryViewModel::onFilterSelected,
                                onToggleViewMode = libraryViewModel::toggleViewMode,
                                onOpenSortSheet = { showSortSheet = true },
                                onRefresh = libraryViewModel::refreshVideos,
                                onRequestPermission = ::requestMediaPermission,
                                onBrowseSaf = ::browseSafVideos,
                                onToggleFavorite = libraryViewModel::toggleFavorite,
                                onMoreVideoOptions = { contextSheetVideo = it }
                            )
                        }

                        composable<FoldersRoute> {
                            FoldersScreen(
                                uiState = libraryState,
                                selectedFolderPath = selectedFolderPath,
                                canRequestAds = canRequestAds,
                                onSelectFolder = { selectedFolderPath = it?.path },
                                onMountSafFolder = { safFolderLauncher.launch(null) },
                                onToggleShowHidden = libraryViewModel::setShowHiddenFolders,
                                onToggleHideFolder = libraryViewModel::toggleHideFolder,
                                onPlayVideo = ::navigateToPlayer,
                                onToggleFavorite = libraryViewModel::toggleFavorite,
                                onMoreVideoOptions = { contextSheetVideo = it }
                            )
                        }

                        composable<PlaylistsRoute> {
                            PlaylistsScreen(
                                uiState = libraryState,
                                selectedPlaylistId = selectedPlaylistId,
                                canRequestAds = canRequestAds,
                                onSelectPlaylist = { selectedPlaylistId = it },
                                onCreatePlaylist = libraryViewModel::createPlaylist,
                                onRenamePlaylist = libraryViewModel::renamePlaylist,
                                onDeletePlaylist = libraryViewModel::deletePlaylist,
                                onRemoveVideoFromPlaylist = libraryViewModel::removeVideoFromPlaylist,
                                onPlayQueue = ::navigateToPlayer,
                                onOpenNetworkStream = { navController.navigate(NetworkStreamRoute) }
                            )
                        }

                        composable<SettingsRoute> {
                            SettingsScreen(
                                prefs = libraryState.preferences,
                                canRequestAds = canRequestAds,
                                isPrivacyOptionsRequired = isPrivacyOptionsRequired,
                                onSetDarkTheme = libraryViewModel::setDarkTheme,
                                onSelectAccent = libraryViewModel::setAccentOption,
                                onUnlockAccentWithRewardedAd = { accent ->
                                    val act = activity ?: return@SettingsScreen
                                    app.adManager.showRewardedAd(
                                        activity = act,
                                        onRewardEarned = {
                                            libraryViewModel.unlockAccentViaReward(accent)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Unlocked ${accent.label} theme!")
                                            }
                                        },
                                        onUnavailable = { msg ->
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        }
                                    )
                                },
                                onSetDefaultSpeed = libraryViewModel::setDefaultSpeed,
                                onSetDefaultOrientation = libraryViewModel::setDefaultOrientation,
                                onSetGesturesEnabled = libraryViewModel::setGesturesEnabled,
                                onSetAutoPlayNext = libraryViewModel::setAutoPlayNext,
                                onSetBackgroundPlayback = libraryViewModel::setBackgroundPlayback,
                                onSetSubtitlePrefs = libraryViewModel::setSubtitlePreferences,
                                onSetShowHiddenFolders = libraryViewModel::setShowHiddenFolders,
                                onSetAppLanguage = libraryViewModel::setAppLanguage,
                                onClearHistory = {
                                    libraryViewModel.clearPlaybackHistory()
                                    scope.launch { snackbarHostState.showSnackbar("Playback history cleared") }
                                },
                                onOpenPrivacyPolicy = { navController.navigate(PrivacyPolicyRoute) },
                                onOpenAdConsentOptions = {
                                    val act = activity ?: return@SettingsScreen
                                    app.adManager.showPrivacyOptionsForm(act) { err ->
                                        if (err != null) {
                                            scope.launch { snackbarHostState.showSnackbar(err) }
                                        }
                                    }
                                }
                            )
                        }

                        composable<NetworkStreamRoute> {
                            NetworkStreamScreen(
                                recentStreams = libraryState.networkStreams,
                                canRequestAds = canRequestAds,
                                onBack = { navController.popBackStack() },
                                onPlayStream = { streamVideo -> navigateToPlayer(listOf(streamVideo), 0) },
                                onSaveStream = libraryViewModel::saveNetworkStream,
                                onDeleteStream = libraryViewModel::deleteNetworkStream
                            )
                        }

                        composable<PrivacyPolicyRoute> {
                            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
                        }

                        composable<PlayerRoute> {
                            PlayerScreen(
                                viewModel = playerViewModel,
                                onPickExternalSubtitle = {
                                    subtitlePickerLauncher.launch(
                                        arrayOf(
                                            "application/x-subrip",
                                            "text/vtt",
                                            "text/plain",
                                            "application/octet-stream",
                                            "*/*"
                                        )
                                    )
                                },
                                onNavigateBack = {
                                    val act = activity
                                    if (act != null) {
                                        app.adManager.onReturnFromPlayer(act) {
                                            navController.popBackStack()
                                        }
                                    } else {
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showSplash) {
            SplashScreenOverlay(onFinished = { showSplash = false })
        }
    }

    // Shared Dialogs & Sheets
    if (showSortSheet) {
        SortAndViewSheet(
            currentSort = libraryState.preferences.sortOption,
            ascending = libraryState.preferences.sortAscending,
            onSelectSort = libraryViewModel::setSort,
            onDismiss = { showSortSheet = false }
        )
    }

    contextSheetVideo?.let { video ->
        VideoContextBottomSheet(
            video = video,
            onDismiss = { contextSheetVideo = null },
            onPlay = {
                val list = libraryState.filteredVideos.ifEmpty { listOf(video) }
                val idx = list.indexOfFirst { it.uri == video.uri }.coerceAtLeast(0)
                navigateToPlayer(list, idx)
            },
            onToggleFavorite = { libraryViewModel.toggleFavorite(video) },
            onAddToPlaylist = { playlistPickerVideo = video },
            onInspectInfo = { libraryViewModel.inspectVideo(video) },
            onShare = { shareVideo(video) },
            onRename = { renameTargetVideo = video },
            onDelete = { deleteTargetVideo = video }
        )
    }

    playlistPickerVideo?.let { video ->
        PlaylistPickerSheet(
            playlists = libraryState.playlists,
            onDismiss = { playlistPickerVideo = null },
            onSelectPlaylist = { plId ->
                libraryViewModel.addVideoToPlaylist(plId, video)
                scope.launch { snackbarHostState.showSnackbar("Added to playlist") }
            },
            onCreatePlaylist = libraryViewModel::createPlaylist
        )
    }

    inspectedMetadata?.let { info ->
        VideoInfoDialog(info = info, onDismiss = libraryViewModel::dismissMetadata)
    }

    renameTargetVideo?.let { video ->
        RenameVideoDialog(
            currentName = video.displayName,
            onDismiss = { renameTargetVideo = null },
            onConfirm = { newName -> performRename(video, newName) }
        )
    }

    deleteTargetVideo?.let { video ->
        DeleteConfirmationDialog(
            videoTitle = video.title,
            onDismiss = { deleteTargetVideo = null },
            onConfirm = {
                val target = video
                deleteTargetVideo = null
                performDelete(target)
            }
        )
    }
}
