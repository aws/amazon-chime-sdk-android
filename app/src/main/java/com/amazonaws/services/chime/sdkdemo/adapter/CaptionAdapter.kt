/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.adapter

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.data.Caption
import com.amazonaws.services.chime.sdkdemo.utils.inflate
import kotlinx.android.synthetic.main.row_caption.view.captionText
import kotlinx.android.synthetic.main.row_caption.view.speakerName

class CaptionAdapter(
    private val captions: Collection<Caption>
) :
    RecyclerView.Adapter<CaptionHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CaptionHolder {
        val inflatedView = parent.inflate(R.layout.row_caption, false)
        return CaptionHolder(inflatedView)
    }

    override fun getItemCount(): Int {
        return captions.size
    }

    override fun onBindViewHolder(holder: CaptionHolder, position: Int) {
        holder.bindCaption(captions.elementAt(position), position)
    }
}

class CaptionHolder(inflatedView: View) :
    RecyclerView.ViewHolder(inflatedView) {
    private var view: View = inflatedView

    fun bindCaption(caption: Caption, position: Int) {
        val speakerName = caption.speakerName ?: ""

        val captionTextBackgroundColor = caption.speakerName?.let { R.color.colorWhite } ?: R.color.colorMissingSpeaker
        view.captionText.setBackgroundResource(captionTextBackgroundColor)
        view.speakerName.text = speakerName
        view.captionText.text = caption.content
        caption.entityContentSet?.let { contents ->
            // Highlight PII identified and redacted words.
            val spannable = SpannableString(caption.content)
            contents.forEach { word ->
                spannable.setSpan(
                    ForegroundColorSpan(Color.GREEN),
                    caption.content.indexOf(word),
                    caption.content.indexOf(word) + word.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                view.captionText.text = spannable
            }
        } ?: run { view.captionText.setTextColor(Color.BLACK) }
        caption.item?.let {
            // Underline unstable words.
            val word = it.content
            if (it.confidence != null && it.confidence!! < 0.3 && !it.content.startsWith("[")) {
                val spannable = SpannableString(caption.content)
                spannable.setSpan(
                    UnderlineSpan(),
                    caption.content.indexOf(word),
                    caption.content.indexOf(word) + word.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                view.captionText.text = spannable
            }
        }
        view.captionText.contentDescription = "caption-$position"
    }
}
