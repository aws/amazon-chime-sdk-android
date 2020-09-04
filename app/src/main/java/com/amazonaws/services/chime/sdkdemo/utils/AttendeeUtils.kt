package com.amazonaws.services.chime.sdkdemo.utils

class AttendeeUtils {
    companion object {
        private val CONTENT_DELIMITER = "#content"
        private val CONTENT_NAME_SUFFIX = "<<Content>>"
        fun getAttendeeName(attendeeId: String, externalUserId: String): String {
            val attendeeName = externalUserId.split('#')[1]

            return if (attendeeId.endsWith(CONTENT_DELIMITER)) {
                "$attendeeName $CONTENT_NAME_SUFFIX"
            } else {
                attendeeName
            }
        }
    }
}
