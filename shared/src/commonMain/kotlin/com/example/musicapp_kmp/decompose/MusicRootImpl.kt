package com.example.musicapp_kmp.decompose

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.overlay.ChildOverlay
import com.arkivanov.decompose.router.overlay.OverlayNavigation
import com.arkivanov.decompose.router.overlay.activate
import com.arkivanov.decompose.router.overlay.childOverlay
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.example.musicapp_kmp.network.SpotifyApi
import com.example.musicapp_kmp.network.models.topfiftycharts.Item
import com.example.musicapp_kmp.player.MediaPlayerController

/**
 * Created by abdulbasit on 19/03/2023.
 */
class MusicRootImpl(
    componentContext: ComponentContext,
    private val mediaPlayerController: MediaPlayerController,
    private val dashboardMain: (ComponentContext, (DashboardMainComponent.Output) -> Unit) -> DashboardMainComponent,
    private val chartDetails: (ComponentContext, playlistId: String, playingTrackId: String, (ChartDetailsComponent.Output) -> Unit) -> ChartDetailsComponent,
) : MusicRoot, ComponentContext by componentContext {
    constructor(
        componentContext: ComponentContext, api: SpotifyApi, mediaPlayerController: MediaPlayerController
    ) : this(componentContext = componentContext,
        mediaPlayerController = mediaPlayerController,
        dashboardMain = { childContext, output ->
            DashboardMainComponentImpl(
                componentContext = childContext, spotifyApi = api, output = output
            )
        },
        chartDetails = { childContext, playlistId, playingTrackId, output ->
            ChartDetailsComponentImpl(
                componentContext = childContext,
                spotifyApi = api,
                playlistId = playlistId,
                output = output,
                playingTrackId = playingTrackId
            )
        })

    private val navigation = StackNavigation<Configuration>()
    private val dialogNavigation = OverlayNavigation<DialogConfig>()

    private val stack = childStack(
        source = navigation,
        initialConfiguration = Configuration.Dashboard,
        handleBackButton = true,
        childFactory = ::createChild
    )

    private fun createChild(
        configuration: Configuration, componentContext: ComponentContext
    ): MusicRoot.Child = when (configuration) {
        Configuration.Dashboard -> MusicRoot.Child.Dashboard(
            dashboardMain(
                componentContext, ::dashboardOutput
            )
        )

        is Configuration.Details -> MusicRoot.Child.Details(
            chartDetails(
                componentContext, configuration.playlistId, currentPlayingTrack, ::detailsOutput
            )
        )
    }

    private var playerEvent: PlayerEvent? = null

    //to keep track of the playing track
    private var currentPlayingTrack = "-1"

    private fun dashboardOutput(output: DashboardMainComponent.Output) {
        when (output) {
            is DashboardMainComponent.Output.PlaylistSelected -> navigation.push(Configuration.Details(
                output.playlistId, currentPlayingTrack
            ) { playerEvent ->
                this.playerEvent = playerEvent
            })
        }
    }

    private fun detailsOutput(output: ChartDetailsComponent.Output) {
        when (output) {
            is ChartDetailsComponent.Output.GoBack -> navigation.pop()
            is ChartDetailsComponent.Output.OnPlayAllSelected -> dialogNavigation.activate(DialogConfig(output.playlist))
            is ChartDetailsComponent.Output.OnTrackSelected -> trackUpdateCallbacks?.invoke(output.trackId)
            is ChartDetailsComponent.Output.OnPlayerEvent -> playerEvent = output.playerEvent
        }
    }

    private var trackUpdateCallbacks: ((String) -> Unit?)? = null

    private val player = childOverlay<DialogConfig, PlayerComponent>(
        source = dialogNavigation,
        persistent = false,
        handleBackButton = false,
        childFactory = { config, _ ->
            PlayerComponentImpl(componentContext = componentContext,
                mediaPlayerController = mediaPlayerController,
                trackList = config.playlist,
                output = {
                    when (it) {
                        PlayerComponent.Output.OnPause -> TODO()
                        PlayerComponent.Output.OnPlay -> TODO()

                        is PlayerComponent.Output.OnTrackUpdated -> {
                            currentPlayingTrack = it.trackId
                            playerEvent?.onTrackUpdated(it.trackId)
                        }

                        is PlayerComponent.Output.RegisterCallbacks -> {
                            trackUpdateCallbacks = it.trackUpdateCallback
                        }
                    }
                })
        })

    override val childStack: Value<ChildStack<*, MusicRoot.Child>>
        get() = value()

    override val dialogOverlay: Value<ChildOverlay<*, PlayerComponent>>
        get() = player

    private fun value() = stack

    private sealed class Configuration : Parcelable {
        @Parcelize
        object Dashboard : Configuration()

        @Parcelize
        data class Details(val playlistId: String, val playingTrackId: String, val callback: (PlayerEvent) -> Unit) :
            Configuration()
    }

    @Parcelize
    private data class DialogConfig(
        val playlist: List<Item>
    ) : Parcelable
}


interface PlayerEvent {
    fun onTrackUpdated(trackId: String)
}
