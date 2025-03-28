/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.data.Message
import com.amazonaws.services.chime.sdkdemo.databinding.RowMessageBinding
import com.amazonaws.services.chime.sdkdemo.utils.inflate

class MessageAdapter(
    private val messages: Collection<Message>
) :
    RecyclerView.Adapter<MessageHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder {
        val inflatedView = parent.inflate(R.layout.row_message, false)
        return MessageHolder(inflatedView)
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun onBindViewHolder(holder: MessageHolder, position: Int) {
        val message = messages.elementAt(position)
        holder.bindMessage(message)
    }
}

class MessageHolder(inflatedView: View) :
    RecyclerView.ViewHolder(inflatedView) {
    private val binding = RowMessageBinding.bind(inflatedView)

    fun bindMessage(message: Message) {
        binding.senderName.text = message.senderName
        binding.messageTimestamp.text = message.displayTime
        binding.messageText.text = message.text
        binding.messageText.contentDescription = message.text
        binding.messageText.textAlignment =
            if (message.isLocal) View.TEXT_ALIGNMENT_TEXT_END else View.TEXT_ALIGNMENT_TEXT_START
    }
}
