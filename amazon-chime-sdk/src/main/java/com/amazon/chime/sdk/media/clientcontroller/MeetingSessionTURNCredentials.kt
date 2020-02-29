/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.clientcontroller

data class MeetingSessionTURNCredentials(
    val username: String,
    val password: String,
    val ttl: String,
    val uris: Array<String?>
) {
    companion object {
        const val TURN_CREDENTIALS_RESULT_USERNAME = "username"
        const val TURN_CREDENTIALS_RESULT_PASSWORD = "password"
        const val TURN_CREDENTIALS_RESULT_URIS = "uris"
        const val TURN_CREDENTIALS_RESULT_TTL = "ttl"
    }
}
