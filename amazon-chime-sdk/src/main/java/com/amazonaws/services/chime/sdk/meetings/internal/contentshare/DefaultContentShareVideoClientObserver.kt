/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.contentshare

import android.content.Context
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareStatus
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareStatusCode
import com.amazonaws.services.chime.sdk.meetings.internal.metric.ClientMetricsCollector
import com.amazonaws.services.chime.sdk.meetings.internal.utils.DNSServerUtils
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ObserverUtils
import com.amazonaws.services.chime.sdk.meetings.internal.utils.TURNRequestUtils
import com.amazonaws.services.chime.sdk.meetings.internal.video.TURNCredentials
import com.amazonaws.services.chime.sdk.meetings.internal.video.TURNRequestParams
import com.amazonaws.services.chime.sdk.meetings.session.URLRewriter
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.audio.audioclient.AudioClient
import com.xodee.client.video.VideoClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DefaultContentShareVideoClientObserver(
    private val context: Context,
    private val logger: Logger,
    private val turnRequestParams: TURNRequestParams,
    private val clientMetricsCollector: ClientMetricsCollector,
    private val urlRewriter: URLRewriter
) : ContentShareVideoClientObserver {
    private val TAG = "DefaultContentShareVideoClientObserver"

    private val contentShareObservers = mutableSetOf<ContentShareObserver>()

    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun subscribeToVideoClientStateChange(observer: ContentShareObserver) {
        contentShareObservers.add(observer)
    }

    override fun unsubscribeFromVideoClientStateChange(observer: ContentShareObserver) {
        contentShareObservers.remove(observer)
    }

    override fun isConnecting(client: VideoClient?) {
        logger.debug(TAG, "content share video client is connecting")
    }

    override fun didConnect(client: VideoClient?, controlStatus: Int) {
        logger.debug(TAG, "content share video client is connected")
        ObserverUtils.notifyObserverOnMainThread(contentShareObservers) {
            it.onContentShareStarted()
        }
    }

    override fun didFail(client: VideoClient?, status: Int, controlStatus: Int) {
        logger.info(TAG, "content share video client is failed with $controlStatus")
        resetContentShareVideoClientMetrics()
        ObserverUtils.notifyObserverOnMainThread(contentShareObservers) {
            it.onContentShareStopped(
                ContentShareStatus(
                    ContentShareStatusCode.VideoServiceFailed
                )
            )
        }
    }

    override fun didStop(client: VideoClient?) {
        logger.info(TAG, "content share video client is stopped")
        resetContentShareVideoClientMetrics()
        ObserverUtils.notifyObserverOnMainThread(contentShareObservers) {
            it.onContentShareStopped(
                ContentShareStatus(
                    ContentShareStatusCode.OK
                )
            )
        }
    }

    override fun requestTurnCreds(client: VideoClient?) {
        logger.info(TAG, "requestTurnCreds")
        uiScope.launch {
            val turnResponse: TURNCredentials? = TURNRequestUtils.doTurnRequest(turnRequestParams, logger)
            with(turnResponse) {
                val isActive = client?.isActive ?: false
                if (this != null && isActive) {
                    val newUris = uris.map { url ->
                        url?.let {
                            urlRewriter(it)
                        }
                    }.toTypedArray()
                    client?.updateTurnCredentials(
                        username,
                        password,
                        ttl,
                        newUris,
                        turnRequestParams.signalingUrl,
                        VideoClient.VideoClientTurnStatus.VIDEO_CLIENT_TURN_FEATURE_ON
                    )
                } else {
                    client?.updateTurnCredentials(
                        null,
                        null,
                        null,
                        null,
                        null,
                        VideoClient.VideoClientTurnStatus.VIDEO_CLIENT_TURN_STATUS_CCP_FAILURE
                    )
                }
            }
        }
    }

    override fun getAvailableDnsServers(): Array<String> {
        return DNSServerUtils.getAvailableDnsServers(context, logger)
    }

    override fun onCameraChanged() {
        // No implementation since we don't use default camera for content share
    }

    override fun onMetrics(metrics: IntArray?, values: DoubleArray?) {
        if (metrics == null || values == null) return

        val metricMap = mutableMapOf<Int, Double>()
        (metrics.indices).map { i -> metricMap[metrics[i]] = values[i] }
        clientMetricsCollector.processContentShareVideoClientMetrics(metricMap)
    }

    private fun resetContentShareVideoClientMetrics() {
        clientMetricsCollector.processContentShareVideoClientMetrics(emptyMap())
    }

    override fun cameraSendIsAvailable(client: VideoClient?, available: Boolean) {
        // No implementation since we don't use default camera for content share
    }

    override fun didReceiveFrame(
        client: VideoClient?,
        frame: Any?,
        profileId: String?,
        displayId: Int,
        pauseType: Int,
        videoId: Int
    ) {
        // No implementation since content share is send only and we don't use this path for local rendering
    }

    override fun pauseRemoteVideo(client: VideoClient?, display_id: Int, pause: Boolean) {
        // No implementation since content share is send only and we don't get any remote video
    }

    override fun onLogMessage(logLevel: Int, message: String?) {
        if (message == null) return
        // Only print error and fatal as the Media team's request to avoid noise for application
        // that has log level set to INFO or higher. All other cases, print as verbose
        if (logLevel == AudioClient.L_ERROR || logLevel == AudioClient.L_FATAL) {
            logger.error(TAG, message)
        } else {
            logger.verbose(TAG, message)
        }
    }

    override fun onTurnURIsReceived(uris: List<String>): List<String> {
        return uris.map(urlRewriter)
    }
}
