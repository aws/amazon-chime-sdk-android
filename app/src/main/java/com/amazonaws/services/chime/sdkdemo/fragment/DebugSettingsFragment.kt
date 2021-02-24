package com.amazonaws.services.chime.sdkdemo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.model.DebugSettingsViewModel
import kotlinx.android.synthetic.main.fragment_debug_settings.view.*

class DebugSettingsFragment : DialogFragment() {
    private lateinit var debugSettingsViewModel: DebugSettingsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout to use as embedded fragment
        return inflater.inflate(R.layout.fragment_debug_settings, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Light_NoActionBar)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        debugSettingsViewModel = ViewModelProvider(requireActivity()).get(DebugSettingsViewModel::class.java)
        view.endpointUrlEditText.setText(debugSettingsViewModel.endpointUrl.value)
        setupClickListeners(view)
    }

    private fun setupClickListeners(view: View) {
        view.saveButton.setOnClickListener {
            debugSettingsViewModel.sendEndpointUrl(view.endpointUrlEditText.text.toString())
            dismiss()
        }
    }
}
