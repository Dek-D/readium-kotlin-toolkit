/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media2

import android.app.PendingIntent
import android.content.Context
import androidx.media2.common.MediaItem
import androidx.media2.common.MediaMetadata
import androidx.media2.common.SessionPlayer
import androidx.media2.session.MediaSession
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.media2.SessionPlayerConnector
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DataSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.readium.dekd.navigator.Navigator
import org.readium.dekd.shared.publication.*
import org.readium.dekd.shared.util.Try
import org.readium.dekd.shared.util.flatMap
import timber.log.Timber
import java.util.concurrent.Executors
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * An audiobook navigator to connect to a MediaSession from Jetpack Media2.
 *
 * Use [create] to get an instance for a given publication, and build a session from it
 * with the [session] method. Apps are responsible for attaching this session to a service able to
 * expose it.
 *
 * You can build a [MediaNavigator] upon any Media2 [SessionPlayer] implementation
 * providing [create] with it. If you don't, ExoPlayer will be used, without cache.
 * You can build your own [SessionPlayer] based on [ExoPlayer] using [ExoPlayerDataSource].
 */
@ExperimentalMedia2
@OptIn(ExperimentalTime::class)
class MediaNavigator private constructor(
    override val publication: Publication,
    private val playerFacade: SessionPlayerFacade,
    private val playerCallback: SessionPlayerCallback,
    private val configuration: Configuration
) : Navigator {

    private val coroutineScope: CoroutineScope = MainScope()

    // This is used only when the Flow's first element is already available, so it doesn't block any thread.
    private fun <T> Flow<T>.stateInFirst(coroutineScope: CoroutineScope): StateFlow<T> =
        stateIn(coroutineScope, SharingStarted.Lazily, runBlocking { first() })

    @ExperimentalTime
    internal fun List<Duration>.sum(): Duration = fold(0.seconds) { a, b -> a + b }

    private val totalDuration: Duration? =
        this.playerFacade.playlist!!.metadata.durations?.sum()

    private val currentLocatorFlow: Flow<Locator> =
        playerCallback.currentItem.map { currentItem ->
            val playlistMetadata = this.playerFacade.playlist!!.map { it.metadata!! }
            locator(
                currentItem,
                playlistMetadata
            )
        }

    private fun locator(
        item: SessionPlayerCallback.Item,
        playlist: List<MediaMetadata>
    ): Locator {
        val position = item.position
        val link = publication.readingOrder[item.index]
        val itemStartPosition = playlist.slice(0 until item.index).durations?.sum()
        val totalProgression =
            if (itemStartPosition == null) null
            else totalDuration?.let { (itemStartPosition + position) / it }

        val locator = requireNotNull(publication.locatorFromLink(link))
        return locator.copyWithLocations(
            fragments = listOf("t=${position.inWholeSeconds}"),
            progression = item.duration?.let { position / it },
            totalProgression = totalProgression
        )
    }

    override val currentLocator: StateFlow<Locator> =
        currentLocatorFlow.stateInFirst(coroutineScope)

    private val playbackStateFlow: Flow<Playback> =
        combine(
            playerCallback.playerState,
            playerCallback.playbackSpeed,
            playerCallback.currentItem,
            playerCallback.bufferingState
        ) { currentState, playbackSpeed, currentItem, bufferingState ->
            val state = when (currentState) {
                SessionPlayerState.Playing ->
                    Playback.State.Playing
                SessionPlayerState.Idle, SessionPlayerState.Error ->
                    Playback.State.Error
                SessionPlayerState.Paused ->
                    if (playerCallback.playbackCompleted) {
                        Playback.State.Finished
                    } else {
                        Playback.State.Paused
                    }
            }
            Playback(
                state = state,
                rate = playbackSpeed.toDouble(),
                resource = Playback.Resource(
                    index = currentItem.index,
                    link = publication.readingOrder[currentItem.index],
                    position = currentItem.position,
                    duration = currentItem.duration
                ),
                buffer = Playback.Buffer(
                    isPlayable = bufferingState != SessionPlayerBufferingState.BUFFERING_STATE_BUFFERING_AND_STARVED,
                    position = currentItem.buffered
                )
            )
        }

    /**
     * Indicates the navigator current state.
     */
    val playback: StateFlow<Playback> =
        playbackStateFlow.stateInFirst(coroutineScope)

    /**
     * Sets the speed of the media playback.
     *
     * Normal speed is 1.0 and 0.0 is incorrect.
     */
    suspend fun setPlaybackRate(rate: Double): Try<Unit, Exception> =
        playerFacade.setPlaybackSpeed(rate).toNavigatorResult()

    /**
     * Resumes or start the playback at the current location.
     */
    suspend fun play(): Try<Unit, Exception> =
        playerFacade.play().toNavigatorResult()

    /**
     * Pauses the playback.
     */
    suspend fun pause(): Try<Unit, Exception> =
        playerFacade.pause().toNavigatorResult()

    /**
     * Seeks to the given time at the given resource.
     */
    suspend fun seek(index: Int, position: Duration): Try<Unit, Exception> =
        playerFacade.seekTo(index, position).toNavigatorResult()

    /**
     * Seeks to the given locator.
     */
    suspend fun go(locator: Locator): Try<Unit, Exception> {
        val itemIndex = publication.readingOrder.indexOfFirstWithHref(locator.href)
            ?: return Try.failure(Exception.InvalidArgument("Invalid href ${locator.href}."))
        val position = locator.locations.time ?: Duration.ZERO
        Timber.v("Go to locator $locator")
        return seek(itemIndex, position)
    }

    /**
     * Seeks to the beginning of the given link.
     */
    suspend fun go(link: Link): Try<Unit, Exception> {
        val locator = publication.locatorFromLink(link)
            ?: return Try.failure(Exception.InvalidArgument("Resource not found at ${link.href}"))
        return go(locator)
    }

    /**
     * Skips to a little amount of time later.
     */
    suspend fun goForward(): Try<Unit, Exception> =
        seekBy(configuration.skipForwardInterval)

    /**
     * Skips to a little amount of time before.
     */
    suspend fun goBackward(): Try<Unit, Exception> =
        seekBy(-configuration.skipBackwardInterval)

    private suspend fun seekBy(offset: Duration): Try<Unit, Exception> =
        this.playerFacade.playlist!!.metadata.durations
            ?.let { smartSeekBy(offset, it) }
            ?: dummySeekBy(offset)

    private suspend fun smartSeekBy(
        offset: Duration,
        durations: List<Duration>
    ): Try<Unit, Exception> {
        val (newIndex, newPosition) =
            SmartSeeker.dispatchSeek(
                offset,
                playerFacade.currentPosition!!,
                playerFacade.currentIndex!!,
                durations
            )
        Timber.v("Smart seeking by $offset resolved to item $newIndex position $newPosition")
        return playerFacade.seekTo(newIndex, newPosition).toNavigatorResult()
    }

    private suspend fun dummySeekBy(offset: Duration): Try<Unit, Exception> {
        val newIndex = playerFacade.currentIndex!!
        val newPosition = playerFacade.currentPosition!! + offset
        return playerFacade.seekTo(newIndex, newPosition).toNavigatorResult()
    }

    /**
     * Stops the playback.
     *
     * Compared to [pause], the navigator may clear its state in whatever way is appropriate. For
     * example, recovering a player's resources.
     */
    fun close() {
        playerFacade.unregisterPlayerCallback(playerCallback)
        playerCallback.close()
        playerFacade.close()
        coroutineScope.cancel()
    }

    /**
     * Builds a [MediaSession] for this navigator.
     */
    fun session(context: Context, activityIntent: PendingIntent, id: String? = null): MediaSession =
        playerFacade.session(context, id, activityIntent)


    data class Configuration(
        val positionRefreshRate: Double = 2.0,  // Hz
        val skipForwardInterval: Duration = 30.seconds,
        val skipBackwardInterval: Duration = 30.seconds,
    )

    @ExperimentalTime
    data class Playback(
        val state: State,
        val rate: Double,
        val resource: Resource,
        val buffer: Buffer
    ) {

        enum class State {
            Playing,
            Paused,
            Finished,
            Error
        }

        data class Resource(
            val index: Int,
            val link: Link,
            val position: Duration,
            val duration: Duration?
        )

        data class Buffer(
            val isPlayable: Boolean,
            val position: Duration
        )
    }

    enum class Buffering {
        Starved,
        Completed,
        Ongoing
    }

    sealed class Exception(override val message: String) : kotlin.Exception(message) {

        class SessionPlayer internal constructor(
            internal val error: SessionPlayerError
        ) : Exception("${error.name} error occurred in SessionPlayer.")

        class InvalidArgument(message: String): Exception(message)
    }

    /*
     * Compatibility
     */

    private fun launchAndRun(runnable: suspend () -> Unit, callback: () -> Unit) =
        coroutineScope.launch { runnable() }.invokeOnCompletion { callback() }

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        launchAndRun({ go(locator) }, completion)
        return true
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean {
        launchAndRun({ go(link) }, completion)
        return true
    }

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        launchAndRun({ goForward() }, completion)
        return true
    }

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        launchAndRun({ goBackward() }, completion)
        return true
    }

    companion object {

        suspend fun create(
            context: Context,
            publication: Publication,
            initialLocator: Locator?,
            configuration: Configuration = Configuration(),
            player: SessionPlayer = createPlayer(context, publication),
            metadataFactory: MediaMetadataFactory = DefaultMetadataFactory(publication)
        ): Try<MediaNavigator, Exception> {

            val positionRefreshDelay = (1.0 / configuration.positionRefreshRate).seconds
            val callback = SessionPlayerCallback(positionRefreshDelay)
            val callbackExecutor = Executors.newSingleThreadExecutor()
            player.registerPlayerCallback(callbackExecutor, callback)

            val facade = SessionPlayerFacade(player, callback.seekCompleted)
            return preparePlayer(publication, facade, metadataFactory)
                // Ignoring failure to set initial locator
                .onSuccess { goInitialLocator(publication, initialLocator, facade) }
                // Player must be ready to play when MediaNavigator's constructor is called.
                .map { MediaNavigator(publication, facade, callback, configuration) }
        }

        private suspend fun preparePlayer(
            publication: Publication,
            player: SessionPlayerFacade,
            metadataFactory: MediaMetadataFactory
        ): Try<Unit, Exception> {
            val playlist = publication.readingOrder.indices.map { index ->
                val metadata = metadataFactory.resourceMetadata(index)
                MediaItem.Builder()
                    .setMetadata(metadata)
                    .build()
            }

            val publicationMetadata = metadataFactory.publicationMetadata()

            return player.setPlaylist(playlist, publicationMetadata)
                .flatMap { player.prepare() }
                .toNavigatorResult()
        }

        private fun createPlayer(context: Context, publication: Publication): SessionPlayer {
            val dataSourceFactory: DataSource.Factory = ExoPlayerDataSource.Factory(publication)

            val player: ExoPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true
                )
                .setHandleAudioBecomingNoisy(true)
                .build()

            return SessionPlayerConnector(player)
        }

        private suspend fun goInitialLocator(
            publication: Publication,
            initialLocator: Locator?,
            player: SessionPlayerFacade
        ) {
            initialLocator?.let { locator ->
                val itemIndex = publication.readingOrder.indexOfFirstWithHref(locator.href)
                    ?: run { Timber.e("Invalid initial locator."); return }
                val position = locator.locations.time
                    ?: Duration.ZERO
                player.seekTo(itemIndex, position)
                    .onFailure { Timber.w("Failed to seek to the provided initial locator.") }
            }
        }

        internal fun SessionPlayerResult.toNavigatorResult(): Try<Unit, Exception> =
            if (isSuccess)
                Try.success(Unit)
            else
                this.mapFailure { Exception.SessionPlayer(it.error) }
    }
}
