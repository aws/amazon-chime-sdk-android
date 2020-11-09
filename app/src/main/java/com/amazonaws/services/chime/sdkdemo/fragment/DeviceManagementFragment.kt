/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.fragment

import android.content.Context
import android.hardware.camera2.CameraManager
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
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoRenderView
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.CameraCaptureSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.VideoCaptureFormat
import com.amazonaws.services.chime.sdk.meetings.device.DeviceChangeObserver
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdk.meetings.device.MediaDeviceType
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.activity.HomeActivity
import com.amazonaws.services.chime.sdkdemo.activity.MeetingActivity
import com.amazonaws.services.chime.sdkdemo.utils.isLandscapeMode
import java.lang.ClassCastException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeviceManagementFragment : Fragment(), DeviceChangeObserver {
    private val logger = ConsoleLogger(LogLevel.INFO)
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val audioDevices = mutableListOf<MediaDevice>()
    private val videoDevices = mutableListOf<MediaDevice>()
    private val videoFormats = mutableListOf<VideoCaptureFormat>()

    private lateinit var cameraManager: CameraManager

    private lateinit var listener: DeviceManagementEventListener
    private lateinit var audioVideo: AudioVideoFacade
    private lateinit var cameraCaptureSource: CameraCaptureSource
    private lateinit var videoPreview: DefaultVideoRenderView

    private val TAG = "DeviceManagementFragment"

    private lateinit var audioDeviceSpinner: Spinner
    private lateinit var audioDeviceArrayAdapter: ArrayAdapter<MediaDevice>
    private lateinit var videoDeviceSpinner: Spinner
    private lateinit var videoDeviceArrayAdapter: ArrayAdapter<MediaDevice>
    private lateinit var videoFormatSpinner: Spinner
    private lateinit var videoFormatArrayAdapter: ArrayAdapter<VideoCaptureFormat>

    private val VIDEO_ASPECT_RATIO_16_9 = 0.5625

    private val AUDIO_DEVICE_SPINNER_INDEX_KEY = "audioDeviceSpinnerIndex"
    private val VIDEO_DEVICE_SPINNER_INDEX_KEY = "videoDeviceSpinnerIndex"
    private val VIDEO_FORMAT_SPINNER_INDEX_KEY = "videoFormatSpinnerIndex"

    private val MAX_VIDEO_FORMAT_HEIGHT = 800
    private val MAX_VIDEO_FORMAT_FPS = 15

    companion object {
        fun newInstance(meetingId: String, name: String): DeviceManagementFragment {
            val fragment = DeviceManagementFragment()

            fragment.arguments =
                Bundle().apply {
                    putString(HomeActivity.MEETING_ID_KEY, meetingId)
                    putString(HomeActivity.NAME_KEY, name)
                }
            return fragment
        }
    }

    interface DeviceManagementEventListener {
        fun onJoinMeetingClicked()
        fun onCachedDeviceSelected(mediaDevice: MediaDevice)
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
        val view = inflater.inflate(R.layout.fragment_device_management, container, false)
        val context = activity as Context

        val meetingId = arguments?.getString(HomeActivity.MEETING_ID_KEY)
        val name = arguments?.getString(HomeActivity.NAME_KEY)
        audioVideo = (activity as MeetingActivity).getAudioVideo()

        val displayedText = getString(R.string.preview_meeting_info, meetingId, name)
        view.findViewById<TextView>(R.id.textViewMeetingPreview)?.text = displayedText

        view.findViewById<Button>(R.id.buttonJoin)?.setOnClickListener {
            listener.onJoinMeetingClicked()
        }

        // Note we call isSelected and setSelection before setting onItemSelectedListener
        // so that we can control the first time the spinner is set and use previous values
        // if they exist (i.e. before rotation). We will set them after lists are populated.

        audioDeviceSpinner = view.findViewById(R.id.spinnerAudioDevice)
        audioDeviceArrayAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, audioDevices)
        audioDeviceSpinner.adapter = audioDeviceArrayAdapter
        audioDeviceSpinner.isSelected = false
        audioDeviceSpinner.setSelection(0, true)
        audioDeviceSpinner.onItemSelectedListener = onAudioDeviceSelected

        videoDeviceSpinner = view.findViewById(R.id.spinnerVideoDevice)
        videoDeviceArrayAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, videoDevices)
        videoDeviceSpinner.adapter = videoDeviceArrayAdapter
        videoDeviceSpinner.isSelected = false
        videoDeviceSpinner.setSelection(0, true)
        videoDeviceSpinner.onItemSelectedListener = onVideoDeviceSelected

        videoFormatSpinner = view.findViewById(R.id.spinnerVideoFormat)
        videoFormatArrayAdapter =
                ArrayAdapter(context, android.R.layout.simple_spinner_item, videoFormats)
        videoFormatSpinner.adapter = videoFormatArrayAdapter
        videoFormatSpinner.isSelected = false
        videoFormatSpinner.setSelection(0, true)
        videoFormatSpinner.onItemSelectedListener = onVideoFormatSelected

        audioVideo.addDeviceChangeObserver(this)

        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        cameraCaptureSource = (activity as MeetingActivity).getCameraCaptureSource()

        view.findViewById<DefaultVideoRenderView>(R.id.videoPreview)?.let {
            val displayMetrics = context.resources.displayMetrics
            val width =
                    if (isLandscapeMode(context)) displayMetrics.widthPixels / 2 else displayMetrics.widthPixels
            val height = (width * VIDEO_ASPECT_RATIO_16_9).toInt()
            it.layoutParams.width = width
            it.layoutParams.height = height

            it.logger = logger
            it.init((activity as MeetingActivity).getEglCoreFactory())
            cameraCaptureSource.addVideoSink(it)
            videoPreview = it
        }

        uiScope.launch {
            populateDeviceList(audioVideo.listAudioDevices(), audioDevices, audioDeviceArrayAdapter)
            populateDeviceList(MediaDevice.listVideoDevices(cameraManager), videoDevices, videoDeviceArrayAdapter)
            cameraCaptureSource.device ?.let {
                populateVideoFormatList(MediaDevice.listSupportedVideoCaptureFormats(cameraManager, it))
            }

            videoPreview.mirror =
                    cameraCaptureSource.device?.type == MediaDeviceType.VIDEO_FRONT_CAMERA

            var audioDeviceSpinnerIndex = 0
            var videoDeviceSpinnerIndex = 0
            var videoFormatSpinnerIndex = 0
            if (savedInstanceState != null) {
                audioDeviceSpinnerIndex = savedInstanceState.getInt(AUDIO_DEVICE_SPINNER_INDEX_KEY, 0)
                videoDeviceSpinnerIndex = savedInstanceState.getInt(VIDEO_DEVICE_SPINNER_INDEX_KEY, 0)
                videoFormatSpinnerIndex = savedInstanceState.getInt(VIDEO_FORMAT_SPINNER_INDEX_KEY, 0)
            }

            audioDeviceSpinner.setSelection(audioDeviceSpinnerIndex)
            videoDeviceSpinner.setSelection(videoDeviceSpinnerIndex)
            videoFormatSpinner.setSelection(videoFormatSpinnerIndex)

            // Setting the selection won't immediately callback, so we need to explicitly set the values
            // of the camera capturer before starting it
            cameraCaptureSource.device = videoDeviceSpinner.getItemAtPosition(videoDeviceSpinnerIndex) as MediaDevice
            videoPreview.mirror = cameraCaptureSource.device?.type == MediaDeviceType.VIDEO_FRONT_CAMERA
            cameraCaptureSource.format = videoFormatSpinner.getItemAtPosition(videoFormatSpinnerIndex) as VideoCaptureFormat

            cameraCaptureSource.start()
        }

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(AUDIO_DEVICE_SPINNER_INDEX_KEY, audioDeviceSpinner.selectedItemPosition)
        outState.putInt(VIDEO_DEVICE_SPINNER_INDEX_KEY, videoDeviceSpinner.selectedItemPosition)
        outState.putInt(VIDEO_FORMAT_SPINNER_INDEX_KEY, videoFormatSpinner.selectedItemPosition)
    }

    override fun onDestroy() {
        super.onDestroy()

        cameraCaptureSource.stop()
        cameraCaptureSource.removeVideoSink(videoPreview)
        videoPreview.release()
        audioVideo.removeDeviceChangeObserver(this)
    }

    private val onAudioDeviceSelected = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            listener.onCachedDeviceSelected(parent?.getItemAtPosition(position) as MediaDevice)
        }

        // Abstract, requires implementation
        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }

    private val onVideoDeviceSelected = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            cameraCaptureSource.device = parent?.getItemAtPosition(position) as MediaDevice

            videoPreview.mirror =
                    cameraCaptureSource.device?.type == MediaDeviceType.VIDEO_FRONT_CAMERA
        }

        // Abstract, requires implementation
        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }

    private val onVideoFormatSelected = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            cameraCaptureSource.format = parent?.getItemAtPosition(position) as VideoCaptureFormat
        }

        // Abstract, requires implementation
        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }

    private fun populateDeviceList(newDeviceList: List<MediaDevice>, currentDeviceList: MutableList<MediaDevice>, adapter: ArrayAdapter<MediaDevice>) {
        currentDeviceList.clear()
        currentDeviceList.addAll(
                newDeviceList.filter {
                    it.type != MediaDeviceType.OTHER
                }.sortedBy { it.order }
        )
        adapter.notifyDataSetChanged()
        if (currentDeviceList.isNotEmpty()) {
            listener.onCachedDeviceSelected(currentDeviceList[0])
        }
    }

    private fun populateVideoFormatList(freshVideoCaptureFormatList: List<VideoCaptureFormat>) {
        videoFormats.clear()

        val filteredFormats = freshVideoCaptureFormatList.filter { it.height <= MAX_VIDEO_FORMAT_HEIGHT }

        for (format in filteredFormats) {
            // AmazonChimeSDKMedia library doesn't yet support 30FPS so anything above will lead to frame drops
            videoFormats.add(VideoCaptureFormat(format.width, format.height, MAX_VIDEO_FORMAT_FPS))
        }
        videoFormatArrayAdapter.notifyDataSetChanged()
    }

    override fun onAudioDeviceChanged(freshAudioDeviceList: List<MediaDevice>) {
        populateDeviceList(freshAudioDeviceList, audioDevices, audioDeviceArrayAdapter)
    }
}
