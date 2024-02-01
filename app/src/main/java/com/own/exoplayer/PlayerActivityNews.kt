/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.own.exoplayer

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.DefaultRenderersFactory.ExtensionRendererMode
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import com.google.android.exoplayer2.drm.FrameworkMediaDrm
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback
import com.google.android.exoplayer2.drm.UnsupportedDrmException
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException
import com.google.android.exoplayer2.source.BehindLiveWindowException
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.DebugTextViewHelper
import com.google.android.exoplayer2.ui.PlaybackControlView
import com.google.android.exoplayer2.ui.SimpleExoPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSink
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.Util
import java.io.File
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.Random
import java.util.UUID

/**
 * An activity that plays media using [SimpleExoPlayer].
 */
class PlayerActivityNews : AppCompatActivity(), View.OnClickListener, Player.EventListener,
    PlaybackControlView.VisibilityListener {
    var rootView: View? = null
    var url: String? = null
    var videoTimeMS = 0
    private val isVideoPurchased = false
    private var isVideoEnd = false
    private var mainHandler: Handler? = null
    private var eventLogger: EventLogger? = null
    private var simpleExoPlayerView: SimpleExoPlayerView? = null
    private var debugRootView: LinearLayout? = null
    var Urltype: String? = null
    private var debugTextView: TextView? = null
    private var floatingText: TextView? = null
    private var retryButton: Button? = null
    private var mediaDataSourceFactory: DataSource.Factory? = null
    private var player: SimpleExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var trackSelectionHelper: TrackSelectionHelper? = null
    private var debugViewHelper: DebugTextViewHelper? = null
    private var inErrorState = false
    private var lastSeenTrackGroupArray: TrackGroupArray? = null
    private var shouldAutoPlay = false
    private var mFullScreenDialog: Dialog? = null

    // Fields used only for ad playback. The ads loader is loaded via reflection.
    private var resumeWindow = 0
    private var resumePosition: Long = 0
    private var imaAdsLoader: Any? = null // com.google.android.exoplayer2.ext.ima.ImaAdsLoader
    private var loadedAdTagUri: Uri? = null
    private var adOverlayViewGroup: ViewGroup? = null
    private val countVideoPlayInMS: Long = 0
    private val lastVideoPlayInMS: Long = 0
    private var width = 0
    private var height = 0
    private var mExoPlayerFullscreen = false
    private var mFullScreenButton: FrameLayout? = null
    private var mFullScreenIcon: ImageView? = null
    private var speedTV: TextView? = null
    var speedx = ""

    // Activity lifecycle
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.e(
            "COnfig Changed",
            newConfig.orientation.toString() + ""
        ) // 1 = Portrait , 2 = LandScape
         try {
            Log.e(TAG, "onConfigurationChanged: " + newConfig.orientation)
            // 1 = Portrait , 2 = LandScape
            if (newConfig.orientation == 1) {

                /*   final float scale = getResources().getDisplayMetrics().density;
                int pixels = (int) (220 * scale + 0.5f);*/
                val scale = resources.displayMetrics.density
                val pixels = (250 * scale + 0.5f).toInt()
                /* RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, pixels);
                rootView.setLayoutParams(layoutParams);*/
                val layoutParams = RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, pixels
                )
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
                rootView!!.layoutParams = layoutParams
            } else {

//
                val layoutParams = RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                rootView!!.layoutParams = layoutParams
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shouldAutoPlay = true
        clearResumePosition()
        mediaDataSourceFactory = buildDataSourceFactory(true)
        mainHandler = Handler()
        if (CookieHandler.getDefault() !== DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER)
        }

        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
       //  url ="https://live-par-2-cdn-alt.livepush.io/live/bigbuckbunnyclip/index.m3u8"
         url ="https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
       // url = "http://sample.vodobox.net/skate_phantom_flex_4k/skate_phantom_flex_4k.m3u8"



//        // Create a PlaylistItem that points to your content
        Log.e(TAG, "URL: $url")
        rootView = findViewById(R.id.root_new)
        rootView!!.setOnClickListener(this)
        debugRootView = findViewById<View>(R.id.controls_root_new) as LinearLayout
        debugTextView = findViewById<View>(R.id.debug_text_view_new) as TextView
        simpleExoPlayerView = findViewById<View>(R.id.player_view_new) as SimpleExoPlayerView

        // to show marquee name on the videos

        retryButton = findViewById<View>(R.id.retry_button_new) as Button
        retryButton!!.setOnClickListener(this)
        simpleExoPlayerView!!.setControllerVisibilityListener(this)
        simpleExoPlayerView!!.requestFocus()
        initFullscreenDialog()
        initFullscreenButton()
        findViewById<View>(R.id.quality).setOnClickListener {
            val mappedTrackInfo = trackSelector!!.currentMappedTrackInfo
            if (mappedTrackInfo != null) {
                trackSelectionHelper!!.showSelectionDialog(
                    this@PlayerActivityNews, "Quality",
                    trackSelector!!.currentMappedTrackInfo, 0
                )
                // Log.e("Size:", trackSelector.getCurrentMappedTrackInfo().length + "");
            }
        }
    }



    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        releasePlayer()
        shouldAutoPlay = true
        clearResumePosition()
        Log.e(TAG, "onNewIntent")
        setIntent(intent)
    }

    public override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initializePlayer()
        }
    }

    public override fun onResume() {
        super.onResume()
        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer()
        }
    }

    public override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    public override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        // PlaybackControlView.lastPositionInMS = 0;
        releaseAdsLoader()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializePlayer()
        } else {
            finish()
        }
    }

    // Activity input
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // If the event was not handled then see if the player view can handle it.
        return super.dispatchKeyEvent(event) || simpleExoPlayerView!!.dispatchKeyEvent(event)
    }

    // OnClickListener methods
    override fun onClick(view: View) {
        if (view === retryButton) {
            initializePlayer()
        } else if (view.parent === debugRootView) {
            val mappedTrackInfo = trackSelector!!.currentMappedTrackInfo
            if (mappedTrackInfo != null) {
                trackSelectionHelper!!.showSelectionDialog(
                    this, (view as Button).text,
                    trackSelector!!.currentMappedTrackInfo, view.getTag() as Int
                )
            }
        }
    }

    // PlaybackControlView.VisibilityListener implementation
    override fun onVisibilityChange(visibility: Int) {
        debugRootView!!.visibility = visibility
    }

    // Internal methods
    private fun initializePlayer() {
        val intent = intent
        val needNewPlayer = player == null
        if (needNewPlayer) {
            val adaptiveTrackSelectionFactory: TrackSelection.Factory =
                AdaptiveTrackSelection.Factory(
                    BANDWIDTH_METER
                )
            trackSelector = DefaultTrackSelector(adaptiveTrackSelectionFactory)
            trackSelectionHelper =
                TrackSelectionHelper(trackSelector, adaptiveTrackSelectionFactory)
            lastSeenTrackGroupArray = null
            eventLogger = EventLogger(trackSelector)
            val drmSchemeUuid = if (intent.hasExtra(DRM_SCHEME_UUID_EXTRA)) UUID.fromString(
                intent.getStringExtra(
                    DRM_SCHEME_UUID_EXTRA
                )
            ) else null
            var drmSessionManager: DrmSessionManager<FrameworkMediaCrypto?>? = null
            if (drmSchemeUuid != null) {
                val drmLicenseUrl = intent.getStringExtra(DRM_LICENSE_URL)
                val keyRequestPropertiesArray = intent.getStringArrayExtra(
                    DRM_KEY_REQUEST_PROPERTIES
                )
                var errorStringId = R.string.error_drm_unknown
                if (Util.SDK_INT < 18) {
                    errorStringId = R.string.error_drm_not_supported
                } else {
                    try {
                        drmSessionManager = buildDrmSessionManagerV18(
                            drmSchemeUuid, drmLicenseUrl,
                            keyRequestPropertiesArray
                        )
                    } catch (e: UnsupportedDrmException) {
                        errorStringId =
                            if (e.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME) R.string.error_drm_unsupported_scheme else R.string.error_drm_unknown
                    }
                }
                if (drmSessionManager == null) {
                    showToast(errorStringId)
                    return
                }
            }
            val preferExtensionDecoders = intent.getBooleanExtra(PREFER_EXTENSION_DECODERS, false)
            @ExtensionRendererMode val extensionRendererMode =
                if ((application as ExoApp).useExtensionRenderers()) (if (preferExtensionDecoders) DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON) else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
            val renderersFactory = DefaultRenderersFactory(
                this,
                drmSessionManager, extensionRendererMode
            )
            player = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector)
            player!!.addListener(this)
            player!!.addListener(eventLogger)
            player!!.setAudioDebugListener(eventLogger)
            player!!.setVideoDebugListener(eventLogger)
            player!!.setMetadataOutput(eventLogger)
            simpleExoPlayerView!!.player = player
            player!!.setPlayWhenReady(shouldAutoPlay)
            debugViewHelper = DebugTextViewHelper(player, debugTextView)
            debugViewHelper!!.start()
        }
        val action = intent.action
        val uris: Array<Uri>
        val extensions: Array<String?>
        //        if (ACTION_VIEW.equals(action)) {
        uris = arrayOf(Uri.parse(url))
        extensions = arrayOf(intent.getStringExtra(EXTENSION_EXTRA))

        if (Util.maybeRequestReadExternalStoragePermission(this, *uris)) {
            // The player will be reinitialized if the permission is granted.
            return
        }
        val mediaSources = arrayOfNulls<MediaSource>(uris.size)
        for (i in uris.indices) {
            mediaSources[i] = buildMediaSource(uris[i], extensions[i])
        }
        var mediaSource =
            if (mediaSources.size == 1) mediaSources[0] else ConcatenatingMediaSource(*mediaSources)
        val adTagUriString = intent.getStringExtra(AD_TAG_URI_EXTRA)
        if (adTagUriString != null) {
            val adTagUri = Uri.parse(adTagUriString)
            if (adTagUri != loadedAdTagUri) {
                releaseAdsLoader()
                loadedAdTagUri = adTagUri
            }
            try {
                mediaSource = createAdsMediaSource(mediaSource, Uri.parse(adTagUriString))
            } catch (e: Exception) {
            }
        } else {
            releaseAdsLoader()
        }


        val haveResumePosition = resumeWindow != C.INDEX_UNSET
        if (haveResumePosition) {
            player!!.seekTo(resumeWindow, resumePosition)
        }
        player!!.prepare(mediaSource, !haveResumePosition, false)
        if (videoTimeMS != 0) {
            player!!.seekTo(videoTimeMS.toLong())
        }
        inErrorState = false
        updateButtonVisibilities()
        Handler().post {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            height = displayMetrics.heightPixels
            width = displayMetrics.widthPixels
            Log.e("Widht & height", simpleExoPlayerView!!.height.toString() + "")
            Log.e("Widht & height", "$height -- $width")

        }
    }


    private fun buildMediaSource(uri: Uri, overrideExtension: String?): MediaSource {
        val type =
            if (TextUtils.isEmpty(overrideExtension)) Util.inferContentType(uri) else Util.inferContentType(
                ".$overrideExtension"
            )
        /*switch (type) {
            case C.TYPE_SS:
                return new SsMediaSource(uri, buildDataSourceFactory(false),
                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
            case C.TYPE_DASH:
                return new DashMediaSource(uri, buildDataSourceFactory(false),
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
            case C.TYPE_HLS:
                return new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, eventLogger);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(),
                        mainHandler, eventLogger);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }*/


//        if (Urltype.equalsIgnoreCase(Const.VIDEO_LIVE_MULTI)){
//            return new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, eventLogger);
//        }
//        else {
//            return new ExtractorMediaSource(uri,
//                    new CacheDataSourceFactory(this, 100 * 1024 * 1024, 5 * 1024 * 1024),
//                    new DefaultExtractorsFactory(), null, null);
        //  return new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, eventLogger);

        /*  if (Urltype.equalsIgnoreCase(Const.VIDEO_STREAM)){
            return new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, eventLogger);
        }
        else {*/

        return HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, eventLogger)


        /* return new ExtractorMediaSource(uri,
                    new PlayerActivityNews.CacheDataSourceFactory(this, 100 * 1024 * 1024, 5 * 1024 * 1024),
                    new DefaultExtractorsFactory(), null, null);*/
        // }
    }

    @Throws(UnsupportedDrmException::class)
    private fun buildDrmSessionManagerV18(
        uuid: UUID,
        licenseUrl: String?, keyRequestPropertiesArray: Array<String>?
    ): DrmSessionManager<FrameworkMediaCrypto?> {
        val drmCallback = HttpMediaDrmCallback(
            licenseUrl,
            buildHttpDataSourceFactory(false)
        )
        if (keyRequestPropertiesArray != null) {
            var i = 0
            while (i < keyRequestPropertiesArray.size - 1) {
                drmCallback.setKeyRequestProperty(
                    keyRequestPropertiesArray[i],
                    keyRequestPropertiesArray[i + 1]
                )
                i += 2
            }
        }
        return DefaultDrmSessionManager(
            uuid, FrameworkMediaDrm.newInstance(uuid), drmCallback,
            null, mainHandler, eventLogger
        )
    }

    private fun releasePlayer() {
        if (player != null) {
            debugViewHelper!!.stop()
            debugViewHelper = null
            shouldAutoPlay = player!!.playWhenReady
            updateResumePosition()
            player!!.release()
            player = null
            trackSelector = null
            trackSelectionHelper = null
            eventLogger = null
        }
    }


    private fun updateResumePosition() {
        resumeWindow = player!!.currentWindowIndex
        resumePosition = Math.max(0, player!!.contentPosition)
      }

    private fun clearResumePosition() {
        resumeWindow = C.INDEX_UNSET
        resumePosition = C.TIME_UNSET
    }

    /**
     * Returns a newtag DataSource factory.
     *
     * @param useBandwidthMeter Whether to set [.BANDWIDTH_METER] as a listener to the newtag
     * DataSource factory.
     * @return A newtag DataSource factory.
     */
    private fun buildDataSourceFactory(useBandwidthMeter: Boolean): DataSource.Factory? {
        return (application as ExoApp)
            .buildDataSourceFactory(if (useBandwidthMeter) BANDWIDTH_METER else null)
    }

    /**
     * Returns a newtag HttpDataSource factory.
     *
     * @param useBandwidthMeter Whether to set [.BANDWIDTH_METER] as a listener to the newtag
     * DataSource factory.
     * @return A newtag HttpDataSource factory.
     */
    private fun buildHttpDataSourceFactory(useBandwidthMeter: Boolean): HttpDataSource.Factory? {
        return (application as ExoApp)
            .buildHttpDataSourceFactory(if (useBandwidthMeter) BANDWIDTH_METER else null)
    }

    /**
     * Returns an ads media source, reusing the ads loader if one exists.
     *
     * @throws Exception Thrown if it was not possible to create an ads media source, for example, due
     * to a missing dependency.
     */
    @Throws(Exception::class)
    private fun createAdsMediaSource(mediaSource: MediaSource?, adTagUri: Uri): MediaSource {
        // Load the extension source using reflection so the demo app doesn't have to depend on it.
        // The ads loader is reused for multiple playbacks, so that ad playback can resume.
        val loaderClass = Class.forName("com.google.android.exoplayer2.ext.ima.ImaAdsLoader")
        if (imaAdsLoader == null) {
            imaAdsLoader = loaderClass.getConstructor(Context::class.java, Uri::class.java)
                .newInstance(this, adTagUri)
            adOverlayViewGroup = FrameLayout(this)
            // The demo app has a non-null overlay frame layout.
            simpleExoPlayerView!!.overlayFrameLayout.addView(adOverlayViewGroup)
        }
        val sourceClass = Class.forName("com.google.android.exoplayer2.ext.ima.ImaAdsMediaSource")
        val constructor = sourceClass.getConstructor(
            MediaSource::class.java,
            DataSource.Factory::class.java, loaderClass, ViewGroup::class.java
        )
        return constructor.newInstance(
            mediaSource, mediaDataSourceFactory, imaAdsLoader,
            adOverlayViewGroup
        ) as MediaSource
    }

    private fun releaseAdsLoader() {
        if (imaAdsLoader != null) {
            try {
                val loaderClass =
                    Class.forName("com.google.android.exoplayer2.ext.ima.ImaAdsLoader")
                val releaseMethod = loaderClass.getMethod("release")
                releaseMethod.invoke(imaAdsLoader)
            } catch (e: Exception) {
                // Should never happen.
                throw IllegalStateException(e)
            }
            imaAdsLoader = null
            loadedAdTagUri = null
            simpleExoPlayerView!!.overlayFrameLayout.removeAllViews()
        }
    }

    // Player.EventListener implementation
    override fun onLoadingChanged(isLoading: Boolean) {
        // Do nothing.
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        if (playbackState == Player.STATE_READY) {

        } else if (playbackState == Player.STATE_IDLE) {

        } else if (playbackState == Player.STATE_BUFFERING) {

        } else if (playbackState == Player.STATE_ENDED) {
            if (isVideoPurchased) isVideoEnd = true
            showControls()
        }

        updateButtonVisibilities()
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        // Do nothing.
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}

    /*@Override
    public void onPositionDiscontinuity() {
        if (inErrorState) {
            // This will only occur if the user has performed a seek whilst in the error state. Update the
            // resume position so that if the user then retries, playback will resume from the position to
            // which they seeked.
            updateResumePosition();
        }
    }*/
    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        // Do nothing.
    }

    override fun onSeekProcessed() {}

    /*@Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
        // Do nothing.
    }
*/
    override fun onPlayerError(e: ExoPlaybackException) {
        var errorString: String? = null
        if (e.type == ExoPlaybackException.TYPE_RENDERER) {
            val cause = e.rendererException
            if (cause is DecoderInitializationException) {
                // Special case for decoder initialization failures.
                val decoderInitializationException = cause
                errorString = if (decoderInitializationException.decoderName == null) {
                    if (decoderInitializationException.cause is DecoderQueryException) {
                        getString(R.string.error_querying_decoders)
                    } else if (decoderInitializationException.secureDecoderRequired) {
                        getString(
                            R.string.error_no_secure_decoder,
                            decoderInitializationException.mimeType
                        )
                    } else {
                        getString(
                            R.string.error_no_decoder,
                            decoderInitializationException.mimeType
                        )
                    }
                } else {
                    getString(
                        R.string.error_instantiating_decoder,
                        decoderInitializationException.decoderName
                    )
                }
            }
        }
        errorString?.let { showToast(it) }
        inErrorState = true
        if (isBehindLiveWindow(e)) {
            clearResumePosition()
            initializePlayer()
        } else {
            updateResumePosition()
            updateButtonVisibilities()
            showControls()
        }
    }

    override fun onPositionDiscontinuity(reason: Int) {}
    override fun onTimelineChanged(timeline: Timeline, manifest: Any, reason: Int) {}
    override fun onTracksChanged(
        trackGroups: TrackGroupArray,
        trackSelections: TrackSelectionArray
    ) {
        updateButtonVisibilities()
        if (trackGroups !== lastSeenTrackGroupArray) {
            val mappedTrackInfo = trackSelector!!.currentMappedTrackInfo
            if (mappedTrackInfo != null) {
                if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_VIDEO)
                    == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS
                ) {
                    showToast(R.string.error_unsupported_video)
                }
                if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_AUDIO)
                    == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS
                ) {
                    showToast(R.string.error_unsupported_audio)
                }
            }
            lastSeenTrackGroupArray = trackGroups
        }
    }

    // User controls
    private fun updateButtonVisibilities() {
        debugRootView!!.removeAllViews()
        retryButton!!.visibility = if (inErrorState) View.VISIBLE else View.GONE
        debugRootView!!.addView(retryButton)
        if (player == null) {
            return
        }
    }

    private fun showControls() {
        //debugRootView.setVisibility(View.VISIBLE);
    }

    private fun showToast(messageId: Int) {
        showToast(getString(messageId))
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }

    fun buildDataSourceFactory(): DataSource.Factory {
        return DataSource.Factory {
            val evictor = LeastRecentlyUsedCacheEvictor((50 * 1024 * 1024).toLong())
            val cacheDir = Environment.getExternalStorageDirectory()
            val simpleCache = SimpleCache(cacheDir, evictor)
            val dataSource = buildDataSourceFactory().createDataSource()
            val cacheFlags =
                CacheDataSource.FLAG_BLOCK_ON_CACHE or CacheDataSource.FLAG_IGNORE_CACHE_FOR_UNSET_LENGTH_REQUESTS
            CacheDataSource(simpleCache, dataSource, cacheFlags, (50 * 1024 * 1024).toLong())
        }
    }

    internal inner class CacheDataSourceFactory(
        private val context: Context,
        private val maxCacheSize: Long,
        private val maxFileSize: Long
    ) : DataSource.Factory {
        private val defaultDatasourceFactory: DefaultDataSourceFactory

        init {
            val userAgent = Util.getUserAgent(
                context, context.getString(R.string.app_name)
            )
            val bandwidthMeter = DefaultBandwidthMeter()
            defaultDatasourceFactory = DefaultDataSourceFactory(
                context,
                bandwidthMeter,
                DefaultHttpDataSourceFactory(userAgent, bandwidthMeter)
            )
        }

        override fun createDataSource(): DataSource {
            val evictor = LeastRecentlyUsedCacheEvictor(maxCacheSize)
            val simpleCache = SimpleCache(
                File(
                    context.packageName, "media"
                ), evictor
            )
            return CacheDataSource(
                simpleCache,
                defaultDatasourceFactory.createDataSource(),
                FileDataSource(),
                CacheDataSink(simpleCache, maxFileSize),
                CacheDataSource.FLAG_BLOCK_ON_CACHE or CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
                null
            )
        }
    }

    private fun initFullscreenDialog() {
        mFullScreenDialog =
            object : Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
                override fun onBackPressed() {
                    if (mExoPlayerFullscreen) closeFullscreenDialog()
                    super.onBackPressed()
                }
            }
    }

    private fun openFullscreenDialog() {

        // ((ViewGroup) simpleExoPlayerView.getParent()).removeView(simpleExoPlayerView);
        //  mFullScreenDialog.addContentView(simpleExoPlayerView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mFullScreenIcon!!.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.ic_fullscreen_skrink
            )
        )
        mExoPlayerFullscreen = true
        simpleExoPlayerView!!.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        //  mFullScreenDialog.show();
    }

    private fun closeFullscreenDialog() {

        //  ((ViewGroup) simpleExoPlayerView.getParent()).removeView(simpleExoPlayerView);
        //   ((RelativeLayout) findViewById(R.id.root_new)).addView(simpleExoPlayerView);
        mExoPlayerFullscreen = false
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        // mFullScreenDialog.dismiss();
        simpleExoPlayerView!!.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL)
        mFullScreenIcon!!.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.ic_fullscreen_expand
            )
        )
    }

    private fun initFullscreenButton() {

        //  PlaybackControlView controlView = simpleExoPlayerView.findViewById(R.id.exo_controller);
        mFullScreenIcon = findViewById(R.id.exo_fullscreen_icon)
        speedTV = findViewById(R.id.speedTV)
        speedTV!!.setVisibility(View.GONE)
        if (speedTV != null) {
            speedTV!!.setOnClickListener { showSpeedOptions() }
            /* if (data.getVideo_type().equals("0") && data.getIs_vod().equals("1"))
                speedTV.setVisibility(View.VISIBLE);
            else
                speedTV.setVisibility(View.GONE);*/
        }
        if (!TextUtils.isEmpty(speedx)) {
            player!!.setPlaybackParameters(
                PlaybackParameters(
                    java.lang.Float.valueOf(
                        speedx.replace(
                            "x",
                            ""
                        )
                    ), 1f
                )
            )
        }
        mFullScreenButton = findViewById(R.id.exo_fullscreen_button)
        mFullScreenButton!!.setOnClickListener(View.OnClickListener { if (!mExoPlayerFullscreen) openFullscreenDialog() else closeFullscreenDialog() })
    }

    private fun showSpeedOptions() {
//        Context wrapper = new ContextThemeWrapper(getContext(), R.style.MyPopupMenu);
        val popupMenu = PopupMenu(this, speedTV!!, R.style.MyPopupMenu)
        //        PopupMenu popupMenu = new PopupMenu(getContext(), speedTV);
        val menu = popupMenu.menu
        val speeds = resources.getStringArray(R.array.speed_values)
        if (speeds.size != 0) {
            for (speed in speeds) {
                menu.add(speed + "x")
            }
            popupMenu.setOnMenuItemClickListener { item ->
                val title = item.title.toString()
                if (player != null) {
                    speedx = title
                    speedTV!!.text = title
                    player!!.playbackParameters =
                        PlaybackParameters(java.lang.Float.valueOf(title.replace("x", "")), 1f)
                }
                false
            }
            popupMenu.show()
        }
    }

    companion object {
        val TAG = PlayerActivityNews::class.java.simpleName
        const val DRM_SCHEME_UUID_EXTRA = "drm_scheme_uuid"
        const val DRM_LICENSE_URL = "drm_license_url"
        const val DRM_KEY_REQUEST_PROPERTIES = "drm_key_request_properties"
        const val PREFER_EXTENSION_DECODERS = "prefer_extension_decoders"
        const val ACTION_VIEW = "com.google.android.exoplayer.demo.action.VIEW"
        const val EXTENSION_EXTRA = "extension"
        const val ACTION_VIEW_LIST = "com.google.android.exoplayer.demo.action.VIEW_LIST"
        const val URI_LIST_EXTRA = "uri_list"
        const val EXTENSION_LIST_EXTRA = "extension_list"
        const val AD_TAG_URI_EXTRA = "ad_tag_uri"
        private val BANDWIDTH_METER = DefaultBandwidthMeter()
        private var DEFAULT_COOKIE_MANAGER: CookieManager? = null

        init {
            DEFAULT_COOKIE_MANAGER = CookieManager()
            DEFAULT_COOKIE_MANAGER!!.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
        }

        private fun isBehindLiveWindow(e: ExoPlaybackException): Boolean {
            if (e.type != ExoPlaybackException.TYPE_SOURCE) {
                return false
            }
            var cause: Throwable? = e.sourceException
            while (cause != null) {
                if (cause is BehindLiveWindowException) {
                    return true
                }
                cause = cause.cause
            }
            return false
        }
    }
}
