package com.amazon.chime.sdk.session

data class CreateAttendeeResponse(val attendee: Attendee)

data class Attendee(val attendeeId: String,
                    val joinToken: String)
