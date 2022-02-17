package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

/*
 * Remote video source initialized with attendeeId and hashed by object address. The values are intentionally mutable
 * so that a map of all current configurations can be kept and updated as needed.
 *
 * @property priority: [VideoPriority] - Relative priority for the subscription. See [VideoPriority] for more information.
 * @property targetResolution: [VideoResolution] - A target resolution for the subscription. The actual receive resolution may vary.
 */
class VideoSubscriptionConfiguration(var priority: VideoPriority, var targetResolution: VideoResolution)
