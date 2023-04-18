/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.minimaldemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import com.amazonaws.services.chime.minimaldemo.databinding.ActivityMainBinding
import com.amazonaws.services.chime.minimaldemo.utils.encodeURLParam
import com.amazonaws.services.chime.minimaldemo.utils.showToast
import com.amazonaws.services.chime.minimaldemo.utils.trimSpaces
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.AudioMode
import com.amazonaws.services.chime.sdk.meetings.internal.utils.DefaultBackOffRetry
import com.amazonaws.services.chime.sdk.meetings.internal.utils.HttpUtils
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    companion object {
        const val MEETING_RESPONSE_KEY = "MEETING_RESPONSE"
        const val MEETING_ID_KEY = "MEETING_ID"
        const val NAME_KEY = "NAME"
        const val AUDIO_MODE_KEY = "AUDIO_MODE"
    }

    private lateinit var binding: ActivityMainBinding
    private val meetingPermissions = arrayOf(
        Manifest.permission.MODIFY_AUDIO_SETTINGS,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    private val meetingPermissionRequestCode = 1
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private var meetingId: String? = null
    private var attendeeName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.contentMain.joinButton.setOnClickListener {
            joinMeeting()
        }
    }

    private fun joinMeeting() {
        meetingId = binding.contentMain.meetingIdInput.text.toString().trimSpaces()
        attendeeName = binding.contentMain.attendeeNameInput.text.toString().trimSpaces()
        if (meetingId.isNullOrBlank()) {
            showToast(getString(R.string.user_notification_meeting_id_invalid))
            return
        }
        if (attendeeName.isNullOrBlank()) {
            showToast(getString(R.string.user_notification_attendee_name_invalid))
            return
        }

        if (hasPermissionsAlready()) {
            authenticate()
        } else {
            ActivityCompat.requestPermissions(this, meetingPermissions, meetingPermissionRequestCode)
        }
    }

    private fun hasPermissionsAlready(): Boolean {
        return meetingPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissionsList: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissionsList, grantResults)
        when (requestCode) {
            meetingPermissionRequestCode -> {
                val isMissingPermission: Boolean =
                    grantResults.isEmpty() || grantResults.any { PackageManager.PERMISSION_GRANTED != it }

                if (isMissingPermission) {
                    showToast(getString(R.string.user_notification_permission_error))
                    return
                }
                authenticate()
            }
        }
    }

    private fun authenticate() {
        if (meetingId.isNullOrBlank() || attendeeName.isNullOrBlank()) return
        uiScope.launch {
            binding.contentMain.progressBar.visibility = View.VISIBLE
            val result = createMeeting()
            binding.contentMain.progressBar.visibility = View.GONE

            if (result == null) {
                showToast(getString(R.string.user_notification_meeting_start_error))
            } else {
                val intent = Intent(applicationContext, MeetingActivity::class.java).apply {
                    putExtras(
                        bundleOf(
                            MEETING_RESPONSE_KEY to result,
                            MEETING_ID_KEY to meetingId,
                            NAME_KEY to attendeeName,
                            AUDIO_MODE_KEY to AudioMode.Stereo48K
                        )
                    )
                }
                startActivity(intent)
            }
        }
    }

    private suspend fun createMeeting(): String? {
        val meetingUrl = getString(R.string.demo_url)
        val meetingRegion = getString(R.string.demo_region)
        val meetingServerUrl = if (meetingUrl.endsWith("/")) meetingUrl else "$meetingUrl/"
        val url = "${meetingServerUrl}join?title=${encodeURLParam(meetingId)}&name=${encodeURLParam(
            attendeeName)}&region=${encodeURLParam(meetingRegion)}"
        val logger = ConsoleLogger()
        val response = HttpUtils.post(URL(url), "", DefaultBackOffRetry(), logger)
        return if (response.httpException == null) {
            response.data
        } else {
            Log.e(TAG, "Unable to join meeting. ${response.httpException}")
            null
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return super.onSupportNavigateUp()
    }
}
