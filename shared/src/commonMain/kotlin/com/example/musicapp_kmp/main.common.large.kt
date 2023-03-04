package com.example.musicapp_kmp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.musicapp_kmp.chartdetails.ChartDetailsScreen
import com.example.musicapp_kmp.chartdetails.ChartDetailsScreenLarge
import com.example.musicapp_kmp.chartdetails.ChartDetailsViewModel
import com.example.musicapp_kmp.dashboard.DashboardScreen
import com.example.musicapp_kmp.dashboard.DashboardScreenLarge
import com.example.musicapp_kmp.dashboard.DashboardViewModel
import com.example.musicapp_kmp.network.SpotifyApiImpl
import com.example.musicapp_kmp.network.models.topfiftycharts.Item
import com.example.musicapp_kmp.player.MediaPlayerController
import com.example.musicapp_kmp.playerview.PlayerView

@Composable
internal fun MainCommonLarge(mediaPlayerController: MediaPlayerController) {
    MyApplicationTheme {
        val screenNavigationState =
            remember { mutableStateOf<SelectedScreen>(SelectedScreen.Dashboard) }
        val tracksList = remember { mutableStateOf<List<Item>>(emptyList()) }
        val api = SpotifyApiImpl()
        val dashboardViewModel = DashboardViewModel(api)

        Box {
            Box(modifier = Modifier.fillMaxWidth()) {
                when (val screen = screenNavigationState.value) {
                    SelectedScreen.Dashboard -> {
                        DashboardScreenLarge(dashboardViewModel) {
                            screenNavigationState.value = SelectedScreen.PlaylistDetails(it)
                        }
                    }

                    is SelectedScreen.PlaylistDetails -> {
                        val chartDetailsViewModel = ChartDetailsViewModel(api, screen.playlistId)
                        ChartDetailsScreenLarge(chartDetailsViewModel) {
                            tracksList.value = it
                        }
                    }

                    else -> {}
                }
            }
            Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                if (tracksList.value.isNotEmpty()) {
                    PlayerView(tracksList.value, mediaPlayerController)
                }
            }
        }
    }
}


sealed interface SelectedScreen {
    object Dashboard : SelectedScreen
    data class PlaylistDetails(val playlistId: String) : SelectedScreen
}