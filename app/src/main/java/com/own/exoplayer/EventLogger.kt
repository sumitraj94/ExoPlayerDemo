package com.own.exoplayer

import android.os.SystemClock
import android.util.Log
import android.view.Surface
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.RepeatMode
import com.google.android.exoplayer2.RendererCapabilities
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.decoder.DecoderCounters
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.MetadataRenderer
import com.google.android.exoplayer2.metadata.emsg.EventMessage
import com.google.android.exoplayer2.metadata.id3.ApicFrame
import com.google.android.exoplayer2.metadata.id3.CommentFrame
import com.google.android.exoplayer2.metadata.id3.GeobFrame
import com.google.android.exoplayer2.metadata.id3.Id3Frame
import com.google.android.exoplayer2.metadata.id3.PrivFrame
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame
import com.google.android.exoplayer2.metadata.id3.UrlLinkFrame
import com.google.android.exoplayer2.source.AdaptiveMediaSourceEventListener
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroup
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.video.VideoRendererEventListener
import java.io.IOException
import java.text.NumberFormat
import java.util.Locale

/**
 * Logs player events using [Log].
 */
class EventLogger(private val trackSelector: MappingTrackSelector) : Player.EventListener,
    AudioRendererEventListener, VideoRendererEventListener, AdaptiveMediaSourceEventListener,
    ExtractorMediaSource.EventListener, DefaultDrmSessionManager.EventListener,
    MetadataRenderer.Output {

    companion object {
        private const val TAG = "EventLogger"
        private const val MAX_TIMELINE_ITEM_LINES = 3
        private val TIME_FORMAT: NumberFormat = NumberFormat.getInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
            isGroupingUsed = false
        }
    }

    private val window = Timeline.Window()
    private val period = Timeline.Period()
    private val startTimeMs = SystemClock.elapsedRealtime()

    // Player.EventListener

    override fun onLoadingChanged(isLoading: Boolean) {
        Log.d(TAG, "loading [$isLoading]")
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, state: Int) {
        Log.d(TAG, "state [${getSessionTimeString()}, $playWhenReady, ${getStateString(state)}]")
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        Log.d(TAG, "repeatMode [${getRepeatModeString(repeatMode)}]")
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        // Do nothing
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        Log.d(
            TAG,
            "playbackParameters " + String.format("[speed=%.2f, pitch=%.2f]", playbackParameters.speed, playbackParameters.pitch)
        )
    }

    override fun onSeekProcessed() {
        // Do nothing
    }

    override fun onPlayerError(e: ExoPlaybackException) {
        Log.e(TAG, "playerFailed [${getSessionTimeString()}]", e)
    }

    override fun onPositionDiscontinuity(reason: Int) {
        // Do nothing
    }

    override fun onTimelineChanged(timeline: Timeline, manifest: Any?, reason: Int) {
        val periodCount = timeline.periodCount
        val windowCount = timeline.windowCount
        Log.d(TAG, "sourceInfo [periodCount=$periodCount, windowCount=$windowCount")
        for (i in 0 until minOf(periodCount, MAX_TIMELINE_ITEM_LINES)) {
            timeline.getPeriod(i, period)
            Log.d(TAG, "  " + "period [" + getTimeString(period.durationMs) + "]")
        }
        if (periodCount > MAX_TIMELINE_ITEM_LINES) {
            Log.d(TAG, "  ...")
        }
        for (i in 0 until minOf(windowCount, MAX_TIMELINE_ITEM_LINES)) {
            timeline.getWindow(i, window)
            Log.d(
                TAG,
                "  " + "window [" + getTimeString(window.durationMs) + ", "
                        + window.isSeekable + ", " + window.isDynamic + "]"
            )
        }
        if (windowCount > MAX_TIMELINE_ITEM_LINES) {
            Log.d(TAG, "  ...")
        }
        Log.d(TAG, "]")
    }

    override fun onTracksChanged(ignored: TrackGroupArray?, trackSelections: TrackSelectionArray) {
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo
        if (mappedTrackInfo == null) {
            Log.d(TAG, "Tracks []")
            return
        }
        Log.d(TAG, "Tracks [")
        // Log tracks associated to renderers.
        for (rendererIndex in 0 until mappedTrackInfo.length) {
            val rendererTrackGroups = mappedTrackInfo.getTrackGroups(rendererIndex)
            val trackSelection = trackSelections[rendererIndex]
            if (rendererTrackGroups.length > 0) {
                Log.d(TAG, "  Renderer:$rendererIndex [")
                for (groupIndex in 0 until rendererTrackGroups.length) {
                    val trackGroup = rendererTrackGroups[groupIndex]
                    val adaptiveSupport = getAdaptiveSupportString(
                        trackGroup.length,
                        mappedTrackInfo.getAdaptiveSupport(rendererIndex, groupIndex, false)
                    )
                    Log.d(
                        TAG,
                        "    Group:$groupIndex, adaptive_supported=$adaptiveSupport ["
                    )
                    for (trackIndex in 0 until trackGroup.length) {
                        val status = getTrackStatusString(trackSelection, trackGroup, trackIndex)
                        val formatSupport = getFormatSupportString(
                            mappedTrackInfo.getTrackFormatSupport(
                                rendererIndex,
                                groupIndex,
                                trackIndex
                            )
                        )
                        Log.d(
                            TAG, "      " + status + " Track:" + trackIndex + ", "
                                    + Format.toLogString(trackGroup.getFormat(trackIndex))
                                    + ", supported=" + formatSupport
                        )
                    }
                    Log.d(TAG, "    ]")
                }
                // Log metadata for at most one of the tracks selected for the renderer.
                if (trackSelection != null) {
                    for (selectionIndex in 0 until trackSelection.length()) {
                        val metadata = trackSelection.getFormat(selectionIndex).metadata
                        if (metadata != null) {
                            Log.d(TAG, "    Metadata [")
                            printMetadata(metadata, "      ")
                            Log.d(TAG, "    ]")
                            break
                        }
                    }
                }
                Log.d(TAG, "  ]")
            }
        }
        // Log tracks not associated with a renderer.
        val unassociatedTrackGroups = mappedTrackInfo.unassociatedTrackGroups
        if (unassociatedTrackGroups.length > 0) {
            Log.d(TAG, "  Renderer:None [")
            for (groupIndex in 0 until unassociatedTrackGroups.length) {
                Log.d(TAG, "    Group:$groupIndex [")
                val trackGroup = unassociatedTrackGroups[groupIndex]
                for (trackIndex in 0 until trackGroup.length) {
                    val status = getTrackStatusString(false)
                    val formatSupport = getFormatSupportString(
                        RendererCapabilities.FORMAT_UNSUPPORTED_TYPE
                    )
                    Log.d(
                        TAG, "      " + status + " Track:" + trackIndex + ", "
                                + Format.toLogString(trackGroup.getFormat(trackIndex))
                                + ", supported=" + formatSupport
                    )
                }
                Log.d(TAG, "    ]")
            }
            Log.d(TAG, "  ]")
        }
        Log.d(TAG, "]")
    }

    // MetadataRenderer.Output

    // MetadataRenderer.Output
    override fun onMetadata(metadata: Metadata) {
        Log.d(TAG, "onMetadata [")
        printMetadata(metadata, "  ")
        Log.d(TAG, "]")
    }

    // AudioRendererEventListener

    // AudioRendererEventListener
    override fun onAudioEnabled(counters: DecoderCounters?) {
        Log.d(TAG, "audioEnabled [" + getSessionTimeString() + "]")
    }

    override fun onAudioSessionId(audioSessionId: Int) {
        Log.d(TAG, "audioSessionId [$audioSessionId]")
    }

    override fun onAudioDecoderInitialized(
        decoderName: String, elapsedRealtimeMs: Long,
        initializationDurationMs: Long
    ) {
        Log.d(TAG, "audioDecoderInitialized [" + getSessionTimeString() + ", " + decoderName + "]")
    }

    override fun onAudioInputFormatChanged(format: Format?) {
        Log.d(
            TAG, "audioFormatChanged [" + getSessionTimeString() + ", " + Format.toLogString(format)
                    + "]"
        )
    }

    override fun onAudioSinkUnderrun(
        bufferSize: Int,
        bufferSizeMs: Long,
        elapsedSinceLastFeedMs: Long
    ) {
    }

    override fun onAudioDisabled(counters: DecoderCounters?) {
        Log.d(TAG, "audioDisabled [" + getSessionTimeString() + "]")
    }


    // VideoRendererEventListener

    // VideoRendererEventListener
    override fun onVideoEnabled(counters: DecoderCounters?) {
        Log.d(TAG, "videoEnabled [" + getSessionTimeString() + "]")
    }

    override fun onVideoDecoderInitialized(
        decoderName: String, elapsedRealtimeMs: Long,
        initializationDurationMs: Long
    ) {
        Log.d(TAG, "videoDecoderInitialized [" + getSessionTimeString() + ", " + decoderName + "]")
    }

    override fun onVideoInputFormatChanged(format: Format?) {
        Log.d(
            TAG, "videoFormatChanged [" + getSessionTimeString() + ", " + Format.toLogString(format)
                    + "]"
        )
    }

    override fun onVideoDisabled(counters: DecoderCounters?) {
        Log.d(TAG, "videoDisabled [" + getSessionTimeString() + "]")
    }

    override fun onDroppedFrames(count: Int, elapsed: Long) {
        Log.d(TAG, "droppedFrames [" + getSessionTimeString() + ", " + count + "]")
    }

    override fun onVideoSizeChanged(
        width: Int, height: Int, unappliedRotationDegrees: Int,
        pixelWidthHeightRatio: Float
    ) {
        Log.d(TAG, "videoSizeChanged [$width, $height]")
    }

    override fun onRenderedFirstFrame(surface: Surface) {
        Log.d(TAG, "renderedFirstFrame [$surface]")
    }

    // DefaultDrmSessionManager.EventListener

    // DefaultDrmSessionManager.EventListener
    override fun onDrmSessionManagerError(e: Exception) {
        printInternalError("drmSessionManagerError", e)
    }

    override fun onDrmKeysRestored() {
        Log.d(TAG, "drmKeysRestored [" + getSessionTimeString() + "]")
    }

    override fun onDrmKeysRemoved() {
        Log.d(TAG, "drmKeysRemoved [" + getSessionTimeString() + "]")
    }

    override fun onDrmKeysLoaded() {
        Log.d(TAG, "drmKeysLoaded [" + getSessionTimeString() + "]")
    }

    // ExtractorMediaSource.EventListener

    // ExtractorMediaSource.EventListener
    override fun onLoadError(error: IOException) {
        printInternalError("loadError", error)
    }

    // AdaptiveMediaSourceEventListener

    // AdaptiveMediaSourceEventListener
    override fun onLoadStarted(
        dataSpec: DataSpec?, dataType: Int, trackType: Int, trackFormat: Format?,
        trackSelectionReason: Int, trackSelectionData: Any?, mediaStartTimeMs: Long,
        mediaEndTimeMs: Long, elapsedRealtimeMs: Long
    ) {
        // Do nothing.
    }

    override fun onLoadError(
        dataSpec: DataSpec?, dataType: Int, trackType: Int, trackFormat: Format?,
        trackSelectionReason: Int, trackSelectionData: Any?, mediaStartTimeMs: Long,
        mediaEndTimeMs: Long, elapsedRealtimeMs: Long, loadDurationMs: Long, bytesLoaded: Long,
        error: IOException, wasCanceled: Boolean
    ) {
        printInternalError("loadError", error)
    }

    override fun onLoadCanceled(
        dataSpec: DataSpec?, dataType: Int, trackType: Int, trackFormat: Format?,
        trackSelectionReason: Int, trackSelectionData: Any?, mediaStartTimeMs: Long,
        mediaEndTimeMs: Long, elapsedRealtimeMs: Long, loadDurationMs: Long, bytesLoaded: Long
    ) {
        // Do nothing.
    }

    override fun onLoadCompleted(
        dataSpec: DataSpec?, dataType: Int, trackType: Int, trackFormat: Format?,
        trackSelectionReason: Int, trackSelectionData: Any?, mediaStartTimeMs: Long,
        mediaEndTimeMs: Long, elapsedRealtimeMs: Long, loadDurationMs: Long, bytesLoaded: Long
    ) {
        // Do nothing.
    }

    override fun onUpstreamDiscarded(trackType: Int, mediaStartTimeMs: Long, mediaEndTimeMs: Long) {
        // Do nothing.
    }

    override fun onDownstreamFormatChanged(
        trackType: Int, trackFormat: Format?, trackSelectionReason: Int,
        trackSelectionData: Any?, mediaTimeMs: Long
    ) {
        // Do nothing.
    }

    // Internal methods

    // Internal methods
    private fun printInternalError(type: String, e: Exception) {
        Log.e(TAG, "internalError [" + getSessionTimeString() + ", " + type + "]", e)
    }

    private fun printMetadata(metadata: Metadata, prefix: String) {
        for (i in 0 until metadata.length()) {
            val entry = metadata[i]
            if (entry is TextInformationFrame) {
                val textInformationFrame = entry
                Log.d(
                    TAG, prefix + String.format(
                        "%s: value=%s", textInformationFrame.id,
                        textInformationFrame.value
                    )
                )
            } else if (entry is UrlLinkFrame) {
                val urlLinkFrame = entry
                Log.d(TAG, prefix + String.format("%s: url=%s", urlLinkFrame.id, urlLinkFrame.url))
            } else if (entry is PrivFrame) {
                val privFrame = entry
                Log.d(TAG, prefix + String.format("%s: owner=%s", privFrame.id, privFrame.owner))
            } else if (entry is GeobFrame) {
                val geobFrame = entry
                Log.d(
                    TAG, prefix + String.format(
                        "%s: mimeType=%s, filename=%s, description=%s",
                        geobFrame.id, geobFrame.mimeType, geobFrame.filename, geobFrame.description
                    )
                )
            } else if (entry is ApicFrame) {
                val apicFrame = entry
                Log.d(
                    TAG, prefix + String.format(
                        "%s: mimeType=%s, description=%s",
                        apicFrame.id, apicFrame.mimeType, apicFrame.description
                    )
                )
            } else if (entry is CommentFrame) {
                val commentFrame = entry
                Log.d(
                    TAG, prefix + String.format(
                        "%s: language=%s, description=%s", commentFrame.id,
                        commentFrame.language, commentFrame.description
                    )
                )
            } else if (entry is Id3Frame) {
                Log.d(TAG, prefix + String.format("%s", entry.id))
            } else if (entry is EventMessage) {
                val eventMessage = entry
                Log.d(
                    TAG, prefix + String.format(
                        "EMSG: scheme=%s, id=%d, value=%s",
                        eventMessage.schemeIdUri, eventMessage.id, eventMessage.value
                    )
                )
            }
        }
    }

    private fun getSessionTimeString(): String {
        return getTimeString(SystemClock.elapsedRealtime() - startTimeMs)
    }

    private fun getTimeString(timeMs: Long): String {
        return if (timeMs == C.TIME_UNSET) "?" else TIME_FORMAT.format((timeMs / 1000f).toDouble())
    }

    private fun getStateString(state: Int): String? {
        return when (state) {
            Player.STATE_BUFFERING -> "B"
            Player.STATE_ENDED -> "E"
            Player.STATE_IDLE -> "I"
            Player.STATE_READY -> "R"
            else -> "?"
        }
    }

    private fun getFormatSupportString(formatSupport: Int): String {
        return when (formatSupport) {
            RendererCapabilities.FORMAT_HANDLED -> "YES"
            RendererCapabilities.FORMAT_EXCEEDS_CAPABILITIES -> "NO_EXCEEDS_CAPABILITIES"
            RendererCapabilities.FORMAT_UNSUPPORTED_SUBTYPE -> "NO_UNSUPPORTED_TYPE"
            RendererCapabilities.FORMAT_UNSUPPORTED_TYPE -> "NO"
            else -> "?"
        }
    }

    private fun getAdaptiveSupportString(trackCount: Int, adaptiveSupport: Int): String {
        return if (trackCount < 2) {
            "N/A"
        } else when (adaptiveSupport) {
            RendererCapabilities.ADAPTIVE_SEAMLESS -> "YES"
            RendererCapabilities.ADAPTIVE_NOT_SEAMLESS -> "YES_NOT_SEAMLESS"
            RendererCapabilities.ADAPTIVE_NOT_SUPPORTED -> "NO"
            else -> "?"
        }
    }

    private fun getTrackStatusString(
        selection: TrackSelection?, group: TrackGroup,
        trackIndex: Int
    ): String {
        return getTrackStatusString(
            selection != null && selection.trackGroup === group && selection.indexOf(trackIndex) != C.INDEX_UNSET
        )
    }

    private fun getTrackStatusString(enabled: Boolean): String {
        return if (enabled) "[X]" else "[ ]"
    }

    private fun getRepeatModeString(@RepeatMode repeatMode: Int): String? {
        return when (repeatMode) {
            Player.REPEAT_MODE_OFF -> "OFF"
            Player.REPEAT_MODE_ONE -> "ONE"
            Player.REPEAT_MODE_ALL -> "ALL"
            else -> "?"
        }
    }
}
