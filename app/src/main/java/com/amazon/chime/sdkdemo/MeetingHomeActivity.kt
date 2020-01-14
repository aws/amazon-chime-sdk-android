package com.amazon.chime.sdkdemo

import android.Manifest
import android.content.pm.PackageManager
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.amazon.chime.sdk.media.AudioVideoFacade
import com.amazon.chime.sdk.media.DefaultAudioVideoFacade

class MeetingHomeActivity : AppCompatActivity() {

    companion object {
        private val TAG = "MeetingHomeActivity"
        private val WEBRTC_PERM = arrayOf(
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        private val WEBRTC_PERMISSION_REQUEST_CODE = 1
        class AuthenticationAsyncTask(val context: MeetingHomeActivity) :
            AsyncTask<String, Integer, String>() {
            private var resp: String? = null
            private val activityReference: WeakReference<MeetingHomeActivity> =
                WeakReference(context)

            override fun onPreExecute() {
                super.onPreExecute()
                activityReference.get()?.authenticationProgressBar?.visibility = View.VISIBLE
            }

            override fun doInBackground(vararg params: String): String? {
                var url: String = params[0]
                var meetingID: String = params[1]
                var name: String = params[2]
                var region = "us-east-1"

                var jsonParam = "join?title=$meetingID&name=$name&region=$region"
                var serverUrl = URL("$url$jsonParam")

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
                        if (responseCode != 200) {
                            return null
                        } else {
                            resp = response.toString()
                        }
                    }

                } catch (e: Exception) {
                    println(e)
                }
                return resp
            }

            override fun onPostExecute(result: String?) {
                super.onPostExecute(result)
                Log.d(TAG, "Value of result is $result")
                //TODO Add the MeetingSession object here instead of using Facade directly
//                var facade: AudioVideoFacade =
//                    DefaultAudioVideoFacade(context.getApplicationContext(), result)
//                facade.start()
                activityReference.get()?.authenticationProgressBar?.visibility = View.INVISIBLE
            }
        }
    }
    private var meetingEditText: EditText? = null
    private var nameEditText: EditText? = null
    private var continueButton: Button? = null
    private var authenticationProgressBar: ProgressBar? = null
    private var meetingID: String? = null
    private var yourName: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meeting_home)
        meetingEditText = findViewById(R.id.editMeetingId) as EditText
        nameEditText = findViewById(R.id.editName) as EditText
        continueButton = findViewById(R.id.buttonContinue) as Button
        continueButton?.setOnClickListener { joinMeeting() }
        authenticationProgressBar = findViewById<ProgressBar>(R.id.progressAuthentication)
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
                val authenticationAsyncTask = AuthenticationAsyncTask(this)
                authenticationAsyncTask.execute(getString(R.string.test_url), meetingID, yourName)
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
        val toast: Toast
        when (requestCode) {
            WEBRTC_PERMISSION_REQUEST_CODE -> {
                if (grantResults.size == 0 || grantResults.any { PackageManager.PERMISSION_GRANTED != it }) {
                    toast = Toast.makeText(
                        this,
                        getString(R.string.permission_error),
                        Toast.LENGTH_LONG
                    )
                    toast.show()
                    return
                }
                val authenticationAsyncTask = AuthenticationAsyncTask(this)
                authenticationAsyncTask.execute(
                    getString(R.string.test_url),
                    meetingID,
                    yourName
                )
            }
        }
    }
}
