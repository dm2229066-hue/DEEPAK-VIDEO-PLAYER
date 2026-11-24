package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.R
import com.example.data.local.PlaylistWithItems
import com.example.data.model.FormatUtils
import com.example.data.model.SortOption
import com.example.data.model.VideoItem
import com.example.data.model.VideoMetadataInfo
import kotlinx.coroutines.delay

@Composable
fun GlassCardContainer(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 18,
    accentBorder: Boolean = false,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val borderBrush = if (accentBorder) {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.11f),
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            )
        )
    }
    Surface(
        modifier = modifier.border(1.dp, borderBrush, shape),
        color = MaterialTheme.colorScheme.surface,
        shape = shape,
        tonalElevation = 2.dp,
        content = content
    )
}

@Composable
fun SectionTitleBar(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun StudioIconWell(
    icon: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    sizeDp: Int = 44,
    iconSizeDp: Int = 22,
    cornerRadius: Int = 14,
    highlighted: Boolean = true,
    tint: Color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val bgBrush = if (highlighted) {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                MaterialTheme.colorScheme.surfaceContainerHigh
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                MaterialTheme.colorScheme.surfaceContainer
            )
        )
    }
    val borderBrush = if (highlighted) {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.12f),
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            )
        )
    }
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(shape)
            .background(bgBrush)
            .border(1.dp, borderBrush, shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSizeDp.dp)
        )
    }
}

@Composable
fun DeepakLogoMark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF0D0C12))
                .border(
                    1.25.dp,
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        )
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_deepak_icon_fg),
                contentDescription = "Deepak Video Player Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.18f)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.width(11.dp))
        Column {
            Text(
                text = "DEEPAK",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "CINEMA PLAYER",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SplashScreenOverlay(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(true) }
    val scale = remember { Animatable(0.84f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(320))
        scale.animateTo(1f, animationSpec = tween(420, easing = FastOutSlowInEasing))
        delay(520)
        visible = false
        delay(250)
        onFinished()
    }

    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(animationSpec = tween(240))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF060608)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_brand_hero),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.36f),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .scale(scale.value)
                    .alpha(alpha.value)
                    .padding(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                        .border(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_deepak_icon_fg),
                        contentDescription = "Deepak Video Player",
                        modifier = Modifier
                            .size(108.dp)
                            .scale(1.12f)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(22.dp))
                Text(
                    text = "Deepak Video Player",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    shape = CircleShape,
                    modifier = Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        CircleShape
                    )
                ) {
                    Text(
                        text = "PRO MEDIA3 STUDIO",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VideoThumbnailBox(
    video: VideoItem,
    modifier: Modifier = Modifier,
    showPlayBadge: Boolean = false
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Movie,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(30.dp)
        )

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(video.uri)
                .videoFrameMillis(1500L)
                .crossfade(true)
                .build(),
            contentDescription = video.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.12f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.68f)
                        )
                    )
                )
        )

        if (showPlayBadge) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x9908080B))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        Surface(
            color = Color.Black.copy(alpha = 0.72f),
            shape = RoundedCornerShape(7.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .border(0.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(7.dp))
        ) {
            Text(
                text = video.resolutionLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        if (video.durationMs > 0L) {
            Surface(
                color = Color.Black.copy(alpha = 0.78f),
                shape = RoundedCornerShape(7.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            ) {
                Text(
                    text = FormatUtils.formatDuration(video.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        if (video.progressFraction > 0.01f) {
            LinearProgressIndicator(
                progress = { video.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.25f)
            )
        }
    }
}

@Composable
fun VideoListRow(
    video: VideoItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCardContainer(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("video_row_${video.id}"),
        cornerRadius = 18
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VideoThumbnailBox(
                video = video,
                modifier = Modifier
                    .width(136.dp)
                    .aspectRatio(16f / 9.5f),
                showPlayBadge = video.lastPositionMs > 2000L
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = FormatUtils.formatFileSize(video.sizeBytes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = video.folderName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (video.lastPositionMs > 2000L && video.durationMs > 0L) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Resume • ${FormatUtils.formatDuration(video.lastPositionMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (video.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (video.isFavorite) "Remove favorite" else "Add favorite",
                    tint = if (video.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(21.dp)
                )
            }
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("video_more_${video.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Video options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
fun VideoGridCard(
    video: VideoItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCardContainer(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("video_grid_${video.id}"),
        cornerRadius = 18
    ) {
        Column {
            VideoThumbnailBox(
                video = video,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f),
                showPlayBadge = true
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 2.dp, top = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${FormatUtils.formatFileSize(video.sizeBytes)} • ${video.folderName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Video options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionAndSafCard(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onBrowseSaf: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCardContainer(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 20,
        accentBorder = !hasPermission
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StudioIconWell(
                    icon = Icons.Rounded.VideoLibrary,
                    sizeDp = 46,
                    iconSizeDp = 24,
                    cornerRadius = 14
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasPermission) "Add External / SAF Videos" else "Access Device Videos",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (hasPermission) {
                            "Pick individual videos or mount external SD/USB folders securely."
                        } else {
                            "Grant media access to automatically discover device videos, or pick files safely via SAF."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!hasPermission) {
                    Button(
                        onClick = onRequestPermission,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("grant_media_permission_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Scan Storage")
                    }
                }
                OutlinedButton(
                    onClick = onBrowseSaf,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("browse_saf_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Browse Files")
                }
            }
        }
    }
}

@Composable
fun EmptyLibraryState(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_videos),
            contentDescription = title,
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(22.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(actionLabel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoContextBottomSheet(
    video: VideoItem,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onInspectInfo: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VideoThumbnailBox(
                    video = video,
                    modifier = Modifier
                        .width(92.dp)
                        .aspectRatio(16f / 10f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${FormatUtils.formatDuration(video.durationMs)} • ${FormatUtils.formatFileSize(video.sizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SheetActionItem(Icons.Rounded.PlayArrow, "Play Now", onClick = { onDismiss(); onPlay() })
            SheetActionItem(
                if (video.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                if (video.isFavorite) "Remove from Favorites" else "Add to Favorites",
                onClick = { onDismiss(); onToggleFavorite() }
            )
            SheetActionItem(Icons.AutoMirrored.Rounded.PlaylistAdd, "Add to Playlist", onClick = { onDismiss(); onAddToPlaylist() })
            SheetActionItem(Icons.Rounded.Info, "Video Properties & Metadata", onClick = { onDismiss(); onInspectInfo() })
            SheetActionItem(Icons.Rounded.Share, "Share Video", onClick = { onDismiss(); onShare() })
            SheetActionItem(Icons.Rounded.DriveFileRenameOutline, "Rename File", onClick = { onDismiss(); onRename() })
            SheetActionItem(
                Icons.Rounded.Delete,
                "Delete Video",
                tint = MaterialTheme.colorScheme.error,
                onClick = { onDismiss(); onDelete() }
            )
        }
    }
}

@Composable
private fun SheetActionItem(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}

@Composable
fun VideoInfoDialog(
    info: VideoMetadataInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Video Metadata", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MetadataLine("Title", info.title)
                MetadataLine("File Name", info.fileName)
                MetadataLine("Folder", info.folderPath)
                MetadataLine("Format / MIME", "${info.codecSummary} (${info.mimeType})")
                MetadataLine("Resolution", info.resolution)
                MetadataLine("Duration", info.durationFormatted)
                MetadataLine("File Size", info.sizeFormatted)
                MetadataLine("Bitrate", info.bitrateFormatted)
                MetadataLine("Frame Rate", info.frameRate)
                MetadataLine("Modified", info.dateModifiedFormatted)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun MetadataLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun RenameVideoDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember(currentName) { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Video") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("File name (include extension)") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text.trim()) },
                enabled = text.isNotBlank()
            ) { Text("Rename") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(
    videoTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Video?") },
        text = {
            Text("Are you sure you want to permanently delete \"$videoTitle\" from device storage?")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistPickerSheet(
    playlists: List<PlaylistWithItems>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (Long) -> Unit,
    onCreatePlaylist: (String) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text("Add to Playlist", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("New playlist name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onCreatePlaylist(newName.trim())
                            newName = ""
                        }
                    },
                    enabled = newName.isNotBlank()
                ) { Text("Create") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(playlists, key = { it.playlist.id }) { item ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelectPlaylist(item.playlist.id)
                                onDismiss()
                            },
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.playlist.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${item.items.size} videos",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortAndViewSheet(
    currentSort: SortOption,
    ascending: Boolean,
    onSelectSort: (SortOption, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Sort Videos By", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            SortOption.entries.forEach { option ->
                val selected = option == currentSort
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val nextAsc = if (selected) !ascending else (option == SortOption.NAME)
                            onSelectSort(option, nextAsc)
                            onDismiss()
                        }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected) {
                        Icon(
                            imageVector = if (ascending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                            contentDescription = if (ascending) "Ascending" else "Descending",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
