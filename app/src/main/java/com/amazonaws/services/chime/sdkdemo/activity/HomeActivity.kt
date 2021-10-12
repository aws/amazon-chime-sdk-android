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
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.amazonaws.services.chime.sdk.meetings.internal.utils.DefaultBackOffRetry
import com.amazonaws.services.chime.sdk.meetings.internal.utils.HttpUtils
import com.amazonaws.services.chime.sdk.meetings.utils.Versioning
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.fragment.DebugSettingsFragment
import com.amazonaws.services.chime.sdkdemo.model.DebugSettingsViewModel
import com.amazonaws.services.chime.sdkdemo.utils.encodeURLParam
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeActivity : BaseActivity() {
    private val logger = ConsoleLogger(LogLevel.INFO)
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

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
    private var authenticationProgressBar: ProgressBar? = null
    private var meetingID: String? = null
    private var yourName: String? = null
    private var testUrl: String = ""
    private lateinit var debugSettingsViewModel: DebugSettingsViewModel

    companion object {
        const val MEETING_RESPONSE_KEY = "MEETING_RESPONSE"
        const val MEETING_ID_KEY = "MEETING_ID"
        const val NAME_KEY = "NAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        meetingEditText = findViewById(R.id.editMeetingId)
        nameEditText = findViewById(R.id.editName)
        authenticationProgressBar = findViewById(R.id.progressAuthentication)
        debugSettingsViewModel = ViewModelProvider(this).get(DebugSettingsViewModel::class.java)

        findViewById<ImageButton>(R.id.buttonContinue)?.setOnClickListener { joinMeeting() }
        findViewById<Button>(R.id.buttonDebugSettings)?.setOnClickListener { showDebugSettings() }

        val versionText: TextView = findViewById(R.id.versionText) as TextView
        versionText.text = "${getString(R.string.version_prefix)}${Versioning.sdkVersion()}"
    }

    private fun showDebugSettings() {
        var debugSettingsFragment = DebugSettingsFragment()
        debugSettingsFragment.show(supportFragmentManager, TAG)
    }

    private fun joinMeeting() {
        meetingID = meetingEditText?.text.toString().trim().replace("\\s+".toRegex(), "+")
        yourName = nameEditText?.text.toString().trim().replace("\\s+".toRegex(), "+")
        testUrl = getTestUrl()

        if (meetingID.isNullOrBlank()) {
            showToast(this, getString(R.string.user_notification_meeting_id_invalid))
        } else if (yourName.isNullOrBlank()) {
            showToast(this, getString(R.string.user_notification_attendee_name_invalid))
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
                    showToast(this, getString(R.string.user_notification_permission_error))
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
                showToast(applicationContext, getString(R.string.user_notification_meeting_url_error))
            } else {
                authenticationProgressBar?.visibility = View.VISIBLE
                val meetingResponseJson: String? = joinMeeting(meetingUrl, meetingId, attendeeName)

                authenticationProgressBar?.visibility = View.INVISIBLE

                if (meetingResponseJson == null) {
                    showToast(applicationContext, getString(R.string.user_notification_meeting_start_error))
                } else {
                    val intent = Intent(applicationContext, MeetingActivity::class.java)
                    intent.putExtra(MEETING_RESPONSE_KEY, meetingResponseJson)
                    intent.putExtra(MEETING_ID_KEY, meetingId)
                    intent.putExtra(NAME_KEY, attendeeName)
                    startActivity(intent)
                }
            }
        }

    private suspend fun joinMeeting(
        meetingUrl: String,
        meetingId: String?,
        attendeeName: String?
    ): String? {
        val meetingServerUrl = if (meetingUrl.endsWith("/")) meetingUrl else "$meetingUrl/"
        val url = "${meetingServerUrl}join?title=${encodeURLParam(meetingId)}&name=${encodeURLParam(
            attendeeName)}&region=${encodeURLParam(MEETING_REGION)}"
        return try {
            val response = HttpUtils.post(URL(url), "", DefaultBackOffRetry(), logger)

            if (response.httpException == null) {
                response.data
            } else {
                logger.error(TAG, "Unable to join meeting. ${response.httpException}")
                null
            }
        } catch (exception: Exception) {
            logger.error(TAG, "There was an exception while joining the meeting: $exception")
            null
        }
    }
}
