package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

/*
 * Enum defining video priority for remote video sources. The 'higher' the number the 'higher' the priority for the source when adjusting video quality
 * to adapt to variable network conditions, i.e. `highest` will be chosen before `high`, `medium`, etc.
 */
enum class VideoPriority(val value: Int) {
    lowest(0),
    low(10),
    medium(20),
    high(30),
    highest(40);
}
