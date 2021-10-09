/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.adapter

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.data.Caption
import com.amazonaws.services.chime.sdkdemo.utils.inflate
import kotlinx.android.synthetic.main.row_caption.view.*

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
        holder.bindCaption(captions.elementAt(position))
    }
}

class CaptionHolder(inflatedView: View) :
    RecyclerView.ViewHolder(inflatedView) {
    private var view: View = inflatedView

    fun bindCaption(caption: Caption) {
        val captionTextBackgroundColor: String = when {
            caption.speakerName.isEmpty() -> {
                // light grey
                "#929292"
            }
            caption.isPartial -> {
                // yellow
                "#F6F19D"
            }
            else -> {
                // white
                "#FFFFFF"
            }
        }
        captionTextBackgroundColor.let { view.captionText.setBackgroundColor(Color.parseColor(it)) }
        view.speakerName.text = caption.speakerName
        view.captionText.text = caption.content
        view.captionText.contentDescription = caption.content
    }
}
