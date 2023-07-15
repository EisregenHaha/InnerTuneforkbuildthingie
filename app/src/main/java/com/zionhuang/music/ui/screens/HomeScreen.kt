package com.zionhuang.music.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.zionhuang.innertube.models.WatchEndpoint
import com.zionhuang.innertube.utils.parseCookieString
import com.zionhuang.music.LocalDatabase
import com.zionhuang.music.LocalPlayerAwareWindowInsets
import com.zionhuang.music.LocalPlayerConnection
import com.zionhuang.music.R
import com.zionhuang.music.constants.InnerTubeCookieKey
import com.zionhuang.music.constants.ListItemHeight
import com.zionhuang.music.extensions.togglePlayPause
import com.zionhuang.music.models.toMediaMetadata
import com.zionhuang.music.playback.queues.YouTubeAlbumRadio
import com.zionhuang.music.playback.queues.YouTubeQueue
import com.zionhuang.music.ui.component.LocalMenuState
import com.zionhuang.music.ui.component.SongListItem
import com.zionhuang.music.ui.component.YouTubeGridItem
import com.zionhuang.music.ui.menu.SongMenu
import com.zionhuang.music.ui.menu.YouTubeAlbumMenu
import com.zionhuang.music.ui.utils.SnapLayoutInfoProvider
import com.zionhuang.music.utils.rememberPreference
import com.zionhuang.music.viewmodels.HomeViewModel
import kotlin.random.Random

@Suppress("DEPRECATION")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val quickPicks by viewModel.quickPicks.collectAsState()
    val newReleaseAlbums by viewModel.newReleaseAlbums.collectAsState()

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val mostPlayedLazyGridState = rememberLazyGridState()

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = viewModel::refresh,
        indicatorPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
            val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor
            val snapLayoutInfoProvider = remember(mostPlayedLazyGridState) {
                SnapLayoutInfoProvider(
                    lazyGridState = mostPlayedLazyGridState,
                    positionInLayout = { layoutSize, itemSize ->
                        (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                    }
                )
            }

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateTopPadding()))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    NavigationTile(
                        title = stringResource(R.string.history),
                        icon = R.drawable.history,
                        onClick = { navController.navigate("history") },
                        modifier = Modifier.weight(1f)
                    )

                    NavigationTile(
                        title = stringResource(R.string.stats),
                        icon = R.drawable.trending_up,
                        onClick = { navController.navigate("stats") },
                        modifier = Modifier.weight(1f)
                    )

                    if (isLoggedIn) {
                        NavigationTile(
                            title = stringResource(R.string.account),
                            icon = R.drawable.person,
                            onClick = {
                                navController.navigate("account")
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.quick_picks),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                        .padding(12.dp)
                )

                quickPicks?.let { quickPicks ->
                    if (quickPicks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ListItemHeight * 4)
                        ) {
                            Text(
                                text = stringResource(R.string.quick_picks_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    } else {
                        LazyHorizontalGrid(
                            state = mostPlayedLazyGridState,
                            rows = GridCells.Fixed(4),
                            flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider),
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ListItemHeight * 4)
                        ) {
                            items(
                                items = quickPicks,
                                key = { it.id }
                            ) { originalSong ->
                                val song by database.song(originalSong.id).collectAsState(initial = originalSong)

                                SongListItem(
                                    song = song!!,
                                    showInLibraryIcon = true,
                                    isActive = song!!.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = song!!,
                                                        navController = navController,
                                                        playerConnection = playerConnection,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .width(horizontalLazyGridItemWidth)
                                        .clickable {
                                            if (song!!.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = song!!.id), song!!.toMediaMetadata()))
                                            }
                                        }
                                )
                            }
                        }
                    }
                }

                if (newReleaseAlbums.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                            .clickable {
                                navController.navigate("new_release")
                            }
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.new_release_albums),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }

                        Icon(
                            painter = painterResource(R.drawable.navigate_next),
                            contentDescription = null
                        )
                    }

                    LazyRow(
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues()
                    ) {
                        items(
                            items = newReleaseAlbums,
                            key = { it.id }
                        ) { album ->
                            YouTubeGridItem(
                                item = album,
                                isActive = mediaMetadata?.album?.id == album.id,
                                isPlaying = isPlaying,
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("album/${album.id}")
                                        },
                                        onLongClick = {
                                            menuState.show {
                                                YouTubeAlbumMenu(
                                                    album = album,
                                                    navController = navController,
                                                    playerConnection = playerConnection,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    )
                                    .animateItemPlacement()
                            )
                        }
                    }
                }

                Spacer(Modifier.height(LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()))
            }

            if (!quickPicks.isNullOrEmpty() || newReleaseAlbums.isNotEmpty()) {
                FloatingActionButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .windowInsetsPadding(
                            LocalPlayerAwareWindowInsets.current
                                .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                        )
                        .padding(16.dp),
                    onClick = {
                        if (Random.nextBoolean() && !quickPicks.isNullOrEmpty()) {
                            val song = quickPicks!!.random()
                            playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = song.id), song.toMediaMetadata()))
                        } else if (newReleaseAlbums.isNotEmpty()) {
                            val album = newReleaseAlbums.random()
                            playerConnection.playQueue(YouTubeAlbumRadio(album.playlistId))
                        }
                    }) {
                    Icon(
                        painter = painterResource(R.drawable.casino),
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
fun NavigationTile(
    title: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp)
            .alpha(if (enabled) 1f else 0.5f)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
