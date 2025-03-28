/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.data.MetricData
import com.amazonaws.services.chime.sdkdemo.databinding.RowMetricBinding
import com.amazonaws.services.chime.sdkdemo.utils.inflate

class MetricAdapter(
    private val metricsList: Collection<MetricData>
) :
    RecyclerView.Adapter<MetricHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetricHolder {
        val inflatedView = parent.inflate(R.layout.row_metric, false)
        return MetricHolder(inflatedView)
    }

    override fun getItemCount(): Int {
        return metricsList.size
    }

    override fun onBindViewHolder(holder: MetricHolder, position: Int) {
        holder.bindMetrics(metricsList.elementAt(position))
    }
}

class MetricHolder(inflatedView: View) :
    RecyclerView.ViewHolder(inflatedView) {

    private val binding = RowMetricBinding.bind(inflatedView)

    fun bindMetrics(metric: MetricData) {
        val name = metric.metricName
        val value = metric.metricValue.toString()
        binding.metricName.text = name
        binding.metricValue.text = value
        binding.metricName.contentDescription = "$name metric"
        binding.metricValue.contentDescription = "$name value"
    }
}
