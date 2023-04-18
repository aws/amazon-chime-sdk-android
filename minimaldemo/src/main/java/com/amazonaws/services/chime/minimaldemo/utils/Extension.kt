/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.minimaldemo.utils

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.services.chime.sdk.meetings.utils.DefaultModality
import com.amazonaws.services.chime.sdk.meetings.utils.ModalityType
import java.net.URLEncoder

fun encodeURLParam(string: String?): String {
    return URLEncoder.encode(string, "utf-8")
}

fun String.trimSpaces(): String {
    return this.trim().replace("\\s+".toRegex(), "+")
}

fun AppCompatActivity.showToast(msg: String) {
    Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
}

fun String.isContentShare(): Boolean {
    return DefaultModality(this).hasModality(ModalityType.Content)
}
