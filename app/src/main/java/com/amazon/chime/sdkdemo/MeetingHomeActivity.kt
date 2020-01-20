package com.amazon.chime.sdkdemo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.amazon.chime.sdk.session.DefaultMeetingSession
import com.amazon.chime.sdk.session.MeetingSessionConfiguration
import com.amazon.chime.sdk.session.MeetingSessionCredentials
import com.amazon.chime.sdk.session.MeetingSessionURLs
import com.amazon.chime.sdk.utils.logger.ConsoleLogger
import com.amazon.chime.sdk.utils.logger.LogLevel
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MeetingHomeActivity : AppCompatActivity() {
    private val gson = Gson()
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val WEBRTC_PERMISSION_REQUEST_CODE = 1
    private val MEETING_REGION = "us-east-1"
    private val TAG = "MeetingHomeActivity"

    private val WEBRTC_PERM = arrayOf(
        Manifest.permission.MODIFY_AUDIO_SETTINGS,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    private var meetingEditText: EditText? = null
    private var nameEditText: EditText? = null
    private var continueButton: Button? = null
    private var authenticationProgressBar: ProgressBar? = null
    private var meetingID: String? = null
    private var yourName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meeting_home)
        meetingEditText = findViewById(R.id.editMeetingId)
        nameEditText = findViewById(R.id.editName)
        continueButton = findViewById(R.id.buttonContinue)
        continueButton?.setOnClickListener { joinMeeting() }
        authenticationProgressBar = findViewById(R.id.progressAuthentication)
    }

    private fun joinMeeting() {
        meetingID = meetingEditText?.text.toString().trim().replace("\\s+".toRegex(), "+")
        yourName = nameEditText?.text.toString().trim().replace("\\s+".toRegex(), "+")

        if (meetingID.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.meeting_id_invalid), Toast.LENGTH_LONG).show()
        } else if (yourName.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.name_invalid), Toast.LENGTH_LONG).show()
        } else {
            if (hasPermissionsAlready()) {
                authenticate(getString(R.string.test_url), meetingID, yourName)
            } else {
                ActivityCompat.requestPermissions(this, WEBRTC_PERM, WEBRTC_PERMISSION_REQUEST_CODE)
            }
        }
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
                    Toast.makeText(this, getString(R.string.permission_error), Toast.LENGTH_LONG)
                        .show()
                    return
                }

                authenticate(getString(R.string.test_url), meetingID, yourName)
            }
        }
    }

    private fun authenticate(
        meetingUrl: String,
        meetingId: String?,
        attendeeName: String?
    ) =
        uiScope.launch {
            authenticationProgressBar?.visibility = View.VISIBLE
            Log.i(TAG, "Joining meeting. URL: $meetingUrl")

            val response: String? = joinMeeting(meetingUrl, meetingId, attendeeName)
            val sessionConfig = createSessionConfiguration(response ?: "")

            if (sessionConfig != null) {
                val logger = ConsoleLogger(LogLevel.INFO)
                val meetingSession =
                    DefaultMeetingSession(sessionConfig, logger, applicationContext)

                meetingSession.audioVideo.start()
            }

            authenticationProgressBar?.visibility = View.INVISIBLE
        }

    private suspend fun joinMeeting(
        meetingUrl: String,
        meetingId: String?,
        attendeeName: String?
    ): String? {
        return withContext(ioDispatcher) {
            val serverUrl =
                URL("${meetingUrl}join?title=$meetingId&name=$attendeeName&region=$MEETING_REGION")

            try {
                val response = StringBuffer()
                with(serverUrl.openConnection() as HttpURLConnection) {
                    requestMethod = "POST"
                    doInput = true
                    doOutput = true

                    BufferedReader(InputStreamReader(inputStream)).use {
                        var inputLine = it.readLine()
                        while (inputLine != null) {
                            response.append(inputLine)
                            inputLine = it.readLine()
                        }
                        it.close()
                    }

                    if (responseCode != 200) null else response.toString()
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Error joining meeting. Exception: ${exception.message}")
                null
            }
        }
    }

    private fun createSessionConfiguration(response: String): MeetingSessionConfiguration? {
        if (response.isBlank()) {
            return null
        }

        val meetingResponse = gson.fromJson(response, MeetingResponse::class.java)
        val meeting = meetingResponse.joinInfo.meeting
        val attendee = meetingResponse.joinInfo.attendee

        val credentials = MeetingSessionCredentials(attendee.attendeeId, attendee.joinToken)
        val urls = MeetingSessionURLs(meeting.mediaPlacement.audioHostUrl)
        return MeetingSessionConfiguration(meeting.meetingId, credentials, urls)
    }
}
