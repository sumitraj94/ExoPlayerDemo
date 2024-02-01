package com.own.exoplayer

import android.app.Application
import com.google.android.exoplayer2.source.hls.BuildConfig


import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.util.Util


var userAgent: String? = null

class ExoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        userAgent = Util.getUserAgent(this, " Exoplayer")
    }

    fun buildDataSourceFactory(bandwidthMeter: DefaultBandwidthMeter?): DataSource.Factory? {
        return DefaultDataSourceFactory(
            this, bandwidthMeter,
            buildHttpDataSourceFactory(bandwidthMeter)
        )
    }

    fun buildHttpDataSourceFactory(bandwidthMeter: DefaultBandwidthMeter?): HttpDataSource.Factory? {
        return DefaultHttpDataSourceFactory(userAgent, bandwidthMeter)
    }

    fun useExtensionRenderers(): Boolean {
        return  BuildConfig.FLAVOR.equals("withExtensions")
    }




}