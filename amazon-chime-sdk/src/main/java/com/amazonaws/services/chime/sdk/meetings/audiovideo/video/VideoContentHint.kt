/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

/**
 * [VideoContentHint] describes the content type of a video source so that downstream encoders, etc. can properly
 * decide on what parameters will work best. These options mirror https://www.w3.org/TR/mst-content-hint/ .
 */
enum class VideoContentHint {
    /**
     * No hint has been provided.
     */
    None,

    /**
     * The track should be treated as if it contains video where motion is important.
     *
     * This is normally webcam video, movies or video games.
     */
    Motion,

    /**
     * The track should be treated as if video details are extra important.
     *
     * This is generally applicable to presentations or web pages with text content, painting or line art.
     */
    Detail,

    /**
     * The track should be treated as if video details are extra important, and that
     * significant sharp edges and areas of consistent color can occur frequently.
     *
     * This is generally applicable to presentations or web pages with text content.
     */
    Text
}
