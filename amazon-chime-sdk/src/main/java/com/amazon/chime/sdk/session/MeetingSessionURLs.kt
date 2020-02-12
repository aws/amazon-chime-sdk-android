package com.amazon.chime.sdk.session

/**
 * [[MeetingSessionURLs]] contains the URLs that will be used to reach the
 * meeting service.
 */
data class MeetingSessionURLs(val audioHostURL: String, val turnControlURL: String, val signalingURL: String)
