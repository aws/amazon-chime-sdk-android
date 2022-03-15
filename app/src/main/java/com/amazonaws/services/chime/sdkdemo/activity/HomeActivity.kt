/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoConfiguration
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.AudioMode
import com.amazonaws.services.chime.sdk.meetings.internal.utils.DefaultBackOffRetry
import com.amazonaws.services.chime.sdk.meetings.internal.utils.HttpUtils
import com.amazonaws.services.chime.sdk.meetings.utils.Versioning
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.fragment.DebugSettingsFragment
import com.amazonaws.services.chime.sdkdemo.model.DebugSettingsViewModel
import com.amazonaws.services.chime.sdkdemo.utils.encodeURLParam
import com.amazonaws.services.chime.sdkdemo.utils.showToast
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {
    private val logger = ConsoleLogger(LogLevel.INFO)
    private val uiScope = CoroutineScope(Dispatchers.Main)

    private val MEETING_REGION = "us-east-1"
    private val TAG = "MeetingHomeActivity"
    private val WEBRTC_PERMISSION_REQUEST_CODE = 1

    private val WEBRTC_PERM = arrayOf(
        Manifest.permission.MODIFY_AUDIO_SETTINGS,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    private var meetingEditText: EditText? = null
    private var nameEditText: EditText? = null
    private var audioMode: AppCompatSpinner? = null
    private var authenticationProgressBar: ProgressBar? = null
    private var meetingID: String? = null
    private var yourName: String? = null
    private var testUrl: String = ""
    private var audioModes = listOf("Stereo/48KHz Audio", "Mono/48KHz Audio", "Mono/16KHz Audio")
    private lateinit var audioVideoConfig: AudioVideoConfiguration
    private lateinit var debugSettingsViewModel: DebugSettingsViewModel

    companion object {
        const val MEETING_RESPONSE_KEY = "MEETING_RESPONSE"
        const val MEETING_ID_KEY = "MEETING_ID"
        const val NAME_KEY = "NAME"
        const val MEETING_ENDPOINT_KEY = "MEETING_ENDPOINT_URL"
        const val AUDIO_MODE_KEY = "AUDIO_MODE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        meetingEditText = findViewById(R.id.editMeetingId)
        nameEditText = findViewById(R.id.editName)
        audioMode = findViewById(R.id.audioModeSpinner)
        authenticationProgressBar = findViewById(R.id.progressAuthentication)
        debugSettingsViewModel = ViewModelProvider(this).get(DebugSettingsViewModel::class.java)

        audioMode?.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, audioModes)
        findViewById<Button>(R.id.buttonContinue)?.setOnClickListener {
            joinMeeting()
        }
        findViewById<Button>(R.id.buttonDebugSettings)?.setOnClickListener { showDebugSettings() }

        val versionText: TextView = findViewById(R.id.versionText) as TextView
        versionText.text = "${getString(R.string.version_prefix)}${Versioning.sdkVersion()}"
    }

    private fun showDebugSettings() {
        var debugSettingsFragment = DebugSettingsFragment()
        debugSettingsFragment.show(supportFragmentManager, TAG)
    }

    private fun joinMeeting() {
        when (audioMode?.selectedItemPosition ?: 0) {
            0 -> audioVideoConfig = AudioVideoConfiguration(audioMode = AudioMode.Stereo48K)
            1 -> audioVideoConfig = AudioVideoConfiguration(audioMode = AudioMode.Mono48K)
            2 -> audioVideoConfig = AudioVideoConfiguration(audioMode = AudioMode.Mono16K)
        }

        meetingID = meetingEditText?.text.toString().trim().replace("\\s+".toRegex(), "+")
        yourName = nameEditText?.text.toString().trim().replace("\\s+".toRegex(), "+")
        testUrl = getTestUrl()

        if (meetingID.isNullOrBlank()) {
            showToast(getString(R.string.user_notification_meeting_id_invalid))
        } else if (yourName.isNullOrBlank()) {
            showToast(getString(R.string.user_notification_attendee_name_invalid))
        } else {
            if (hasPermissionsAlready()) {
                authenticate(testUrl, meetingID, yourName)
            } else {
                ActivityCompat.requestPermissions(this, WEBRTC_PERM, WEBRTC_PERMISSION_REQUEST_CODE)
            }
        }
    }

    private fun getTestUrl(): String {
        val endpointUrl = debugSettingsViewModel.endpointUrl.value
        return if (endpointUrl.isNullOrEmpty()) getString(R.string.test_url) else endpointUrl
    }

    private fun hasPermissionsAlready(): Boolean {
        return WEBRTC_PERM.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissionsList: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            WEBRTC_PERMISSION_REQUEST_CODE -> {
                val isMissingPermission: Boolean =
                    grantResults.isEmpty() || grantResults.any { PackageManager.PERMISSION_GRANTED != it }

                if (isMissingPermission) {
                    showToast(getString(R.string.user_notification_permission_error))
                    return
                }
                authenticate(testUrl, meetingID, yourName)
            }
        }
    }

    private fun authenticate(
        meetingUrl: String,
        meetingId: String?,
        attendeeName: String?
    ) =
        uiScope.launch {
            logger.info(
                TAG,
                "Joining meeting. meetingUrl: $meetingUrl, meetingId: $meetingId, attendeeName: $attendeeName"
            )
            if (!meetingUrl.startsWith("http")) {
                showToast(getString(R.string.user_notification_meeting_url_error))
            } else {
                authenticationProgressBar?.visibility = View.VISIBLE

                val primaryMeetingId = debugSettingsViewModel.primaryMeetingId.value
                val meetingResponseJson: String? = joinMeeting(meetingUrl, meetingId, attendeeName, primaryMeetingId)

                authenticationProgressBar?.visibility = View.INVISIBLE

                if (meetingResponseJson == null) {
                    showToast(getString(R.string.user_notification_meeting_start_error))
                } else {
                    val intent = Intent(applicationContext, MeetingActivity::class.java).apply {
                        putExtras(
                            bundleOf(
                                MEETING_RESPONSE_KEY to meetingResponseJson,
                                MEETING_ID_KEY to meetingId,
                                NAME_KEY to attendeeName,
                                MEETING_ENDPOINT_KEY to meetingUrl,
                                AUDIO_MODE_KEY to audioVideoConfig.audioMode.value
                            )
                        )
                    }
                    startActivity(intent)
                }
            }
        }

    private suspend fun joinMeeting(
        meetingUrl: String,
        meetingId: String?,
        attendeeName: String?,
        primaryMeetingId: String?
    ): String? {
        val meetingServerUrl = if (meetingUrl.endsWith("/")) meetingUrl else "$meetingUrl/"
        var url = "${meetingServerUrl}join?title=${encodeURLParam(meetingId)}&name=${encodeURLParam(
            attendeeName)}&region=${encodeURLParam(MEETING_REGION)}"
        if (!primaryMeetingId.isNullOrEmpty()) {
            url += "&primaryExternalMeetingId=${encodeURLParam(primaryMeetingId)}"
        }
        val response = HttpUtils.post(URL(url), "", DefaultBackOffRetry(), logger)
        return if (response.httpException == null) {
            response.data
        } else {
            logger.error(TAG, "Unable to join meeting. ${response.httpException}")
            null
        }
    }
}
