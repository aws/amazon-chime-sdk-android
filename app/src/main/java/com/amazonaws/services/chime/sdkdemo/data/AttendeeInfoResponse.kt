/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazonaws.services.chime.sdkdemo.data

import com.google.gson.annotations.SerializedName

data class AttendeeInfoResponse(
    @SerializedName("AttendeeInfo") val attendeeInfo: AttendeeInfo
)

data class AttendeeInfo(
    @SerializedName("AttendeeId") val attendeeId: String,
    @SerializedName("Name") val name: String
)
