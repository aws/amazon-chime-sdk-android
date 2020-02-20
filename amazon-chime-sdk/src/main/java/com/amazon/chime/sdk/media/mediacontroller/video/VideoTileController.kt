package com.amazon.chime.sdk.media.mediacontroller.video

interface VideoTileController : VideoTileControllerFacade {

    /**
     * To initialize anything related to VideoTileController
     */
    fun initialize()

    /**
     * To destroy anything related to VideoTileController
     */
    fun destroy()

    /**
     * Called whenever there is anew Video frame received for any of the attendee in the meeting
     *
     * @param frame: [Any] - Video frame
     * @param profileId: [String] - Profile Id of the attendee
     * @param displayId: [Int] - display Id
     * @param pauseType: [Int] - pauseType
     * @param videoId: [Int] - Video Id
     */
    fun onReceiveFrame(
        frame: Any?,
        attendeeId: String?,
        displayId: Int,
        pauseType: Int,
        videoId: Int
    )
}
