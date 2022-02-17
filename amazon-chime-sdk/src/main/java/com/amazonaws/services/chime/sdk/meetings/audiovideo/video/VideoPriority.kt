package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

/*
 * Enum defining video priority for remote video sources. The 'higher' the number the 'higher' the priority for the source when adjusting video quality
 * to adapt to variable network conditions, i.e. `Highest` will be chosen before `High`, `Medium`, etc.
 */
enum class VideoPriority(val value: Int) {
    Lowest(0),
    Low(10),
    Medium(20),
    High(30),
    Highest(40);
}
