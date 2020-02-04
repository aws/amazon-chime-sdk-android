package com.amazon.chime.sdkdemo

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.amazon.chime.sdk.media.AudioVideoFacade
import com.amazon.chime.sdk.media.devicecontroller.MediaDevice
import com.amazon.chime.sdk.utils.logger.ConsoleLogger
import com.amazon.chime.sdk.utils.logger.LogLevel
import java.lang.ClassCastException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeviceManagementFragment : Fragment() {
    private val logger = ConsoleLogger(LogLevel.INFO)
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private lateinit var listener: DeviceManagementEventListener
    private lateinit var audioVideo: AudioVideoFacade

    private val TAG = "DeviceManagementFragment"

    companion object {

        fun newInstance(meetingId: String, name: String): DeviceManagementFragment {
            val fragment = DeviceManagementFragment()

            fragment.arguments =
                Bundle().apply {
                    putString(MeetingHomeActivity.MEETING_ID_KEY, meetingId)
                    putString(MeetingHomeActivity.NAME_KEY, name)
                }
            return fragment
        }
    }

    interface DeviceManagementEventListener {
        fun onJoinMeetingClicked()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is DeviceManagementEventListener) {
            listener = context
        } else {
            logger.error(TAG, "$context must implement DeviceManagementEventListener.")
            throw ClassCastException("$context must implement DeviceManagementEventListener.")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_device_management, container, false)
        val activity = activity as Context

        val meetingId = arguments?.getString(MeetingHomeActivity.MEETING_ID_KEY)
        val name = arguments?.getString(MeetingHomeActivity.NAME_KEY)
        audioVideo = (activity as InMeetingActivity).getAudioVideo()

        val displayedText = getString(R.string.preview_meeting_info, meetingId, name)
        view.findViewById<TextView>(R.id.textViewMeetingPreview)?.text = displayedText

        view.findViewById<Button>(R.id.buttonJoin)?.setOnClickListener {
            listener.onJoinMeetingClicked()
        }

        populateAllDeviceLists(view, activity)
        return view
    }

    private val onMicrophoneSelected = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            audioVideo.chooseAudioInputDevice(parent?.getItemAtPosition(position) as MediaDevice)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }

    private val onSpeakerSelected = object :
        AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            audioVideo.chooseAudioOutputDevice(parent?.getItemAtPosition(position) as MediaDevice)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }

    // TODO: implement device change observer
    private fun populateAllDeviceLists(view: View, context: Context) {
        uiScope.launch {
            val microphoneDevices = listMicrophoneDevices()

            val spinnerMicrophone = view.findViewById<Spinner>(R.id.spinnerMicrophone)
            spinnerMicrophone?.adapter = createSpinnerAdapter(context, microphoneDevices)
            spinnerMicrophone?.onItemSelectedListener = onMicrophoneSelected

            val speakerDevices = listSpeakerDevices()
            val spinnerSpeaker = view.findViewById<Spinner>(R.id.spinnerSpeaker)
            spinnerSpeaker?.adapter = createSpinnerAdapter(context, speakerDevices)
            spinnerSpeaker?.onItemSelectedListener = onSpeakerSelected
        }
    }

    private suspend fun listMicrophoneDevices(): List<MediaDevice> {
        return withContext(Dispatchers.Default) {
            audioVideo.listAudioInputDevices()
        }
    }

    private suspend fun listSpeakerDevices(): List<MediaDevice> {
        return withContext(Dispatchers.Default) {
            audioVideo.listAudioOutputDevices()
        }
    }

    private fun createSpinnerAdapter(context: Context, list: List<MediaDevice>): ArrayAdapter<MediaDevice> {
        return ArrayAdapter(context, android.R.layout.simple_spinner_item, list)
    }
}
