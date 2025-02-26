package com.amazonaws.services.chime.sdkdemo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.databinding.FragmentDebugSettingsBinding
import com.amazonaws.services.chime.sdkdemo.model.DebugSettingsViewModel
import com.amazonaws.services.chime.sdkdemo.utils.addPaddingsForSystemBars

class DebugSettingsFragment : DialogFragment() {
    private lateinit var debugSettingsViewModel: DebugSettingsViewModel
    private var _binding: FragmentDebugSettingsBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout to use as embedded fragment
        _binding = FragmentDebugSettingsBinding.inflate(inflater, container, false)
        addPaddingsForSystemBars(binding.root)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Light_NoActionBar)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        debugSettingsViewModel = ViewModelProvider(requireActivity()).get(DebugSettingsViewModel::class.java)
        binding.endpointUrlEditText.setText(debugSettingsViewModel.endpointUrl.value)
        binding.primaryMeetingIdEditText.setText(debugSettingsViewModel.primaryMeetingId.value)
        setupClickListeners(view)
    }

    private fun setupClickListeners(view: View) {
        binding.saveButton.setOnClickListener {
            debugSettingsViewModel.sendEndpointUrl(binding.endpointUrlEditText.text.toString())
            debugSettingsViewModel.sendPrimaryMeetingId(binding.primaryMeetingIdEditText.text.toString())
            dismiss()
        }
    }
}
