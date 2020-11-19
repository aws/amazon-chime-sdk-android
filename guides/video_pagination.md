# Video Pagination with Active Speaker-Based Policy

## Overview

Amazon Chime SDK currently supports [multiple video tiles](https://docs.aws.amazon.com/chime/latest/dg/meetings-sdk.html#mtg-limits) in a meeting. However, in many cases, the application may not want to render all the available video streams, because of the following reasons:

* **Hardware limitation** - On mobile devices, screen sizes are relatively small and network as well as computation resources are rather limited. If an application renders all the available videos on the same screen at the same time, it will consume a lot of network bandwidth and CPU to subscribe to video streams and decode video frames. Each video tile will be extremely small and barely visible, which results in a bad user experience.
* **Use cases do not require multiple videos** - An application could have specific use cases that do not require multiple videos being rendered at the same time. For example, an online fitness training application may only want to show the video from the instructor and the student themselves, and ignore all the video streams from other students.

In the following example, we will show you how to selectively render some of the available video streams using `pauseRemoteVideoTile(tileId)` and `pauseRemoteVideoTile(tileId)` APIs. We will implement the following features:

* Remote videos will be paginated into several pages. Each page contains at most 4 remote videos.
* User can switch between different pages.
* User can manually pause/resume specific video tiles.
* Video tiles from active speakers will be promoted to the top of the list automatically.

## Prerequisite

Amazon Chime SDK provides multiple APIs for sending, receiving, and displaying videos. Before we jump into code implementation, please read [In-depth Look and Comparison Between Video APIs](api_overview.md#8g-in-depth-look-and-comparison-between-video-apis) to understand what these video APIs do under the hood and when you want to call them.

You should also have basic knowledge about how to render an ordered collection of data items and present them using customizable layouts.

## Implementation - Video Pagination

To implement basic video pagination feature, we need to maintain the following states in the application:

```kotlin
// How many remote videos to render at most per page
private val remoteVideoTileCountPerPage = 4

// Index of the page that the user is currently viewing
private var currentRemoteVideoPageIndex = 0

// An ordered list of VideoTileState for remote videos
private val remoteVideoTileStates = mutableListOf<VideoTileState>()

// An ordered list of VideoTileState for remote videos on the current page
private val remoteVideoStatesOnCurrentPage = mutableListOf<VideoTileState>()

// Ids of the video tiles that user paused manually
private val userPausedVideoTileIds = mutableSetOf<Int>()
```

Given 1) the page number that the user is on; and 2) the number of remote videos per page, we can calculate the slice of `VideoTileState`s to render:

```kotlin
private fun updateRemoteVideoStatesOnCurrentPage() {
    remoteVideoStatesOnCurrentPage.clear()

    val remoteVideoStartIndex = currentRemoteVideoPageIndex * remoteVideoTileCountPerPage
    val remoteVideoEndIndex = min(remoteVideoTileStates.size, remoteVideoStartIndex + remoteVideoTileCountPerPage) - 1
    if (remoteVideoStartIndex <= remoteVideoEndIndex) {
        remoteVideoStatesOnCurrentPage.addAll(remoteVideoTileStates.slice(remoteVideoStartIndex..remoteVideoEndIndex))
    }
}
```

Once we have the current page calculated, we can resume videos on the current page and pause rest of the videos. Note that user may also explicitly pause remote videos, and we will not resume those videos even they are on the current page.

```kotlin
private fun resumeAllRemoteVideosOnCurrentPageExceptUserPausedVideos() {
    remoteVideoTileStates.forEach {
        if (remoteVideoStatesOnCurrentPage.contains(it) && !userPausedVideoTileIds.contains(it.tileId)) {
            if (it.pauseState == VideoPauseState.PausedByUserRequest) {
                audioVideo.resumeRemoteVideoTile(it.tileId)
            }
        } else {
            if (it.pauseState != VideoPauseState.PausedByUserRequest) {
                audioVideo.pauseRemoteVideoTile(it.tileId)
            }
        }
    }
}

pauseToggleButton.setOnClickListener {
    if (videoTileState.pauseState != VideoPauseState.PausedByUserRequest) {
        audioVideo.pauseRemoteVideoTile(videoTileState.tileId)
        userPausedVideoTileIds.add(videoTileState.tileId)
    } else {
        audioVideo.resumeRemoteVideoTile(videoTileState.tileId)
        userPausedVideoTileIds.remove(videoTileState.tileId)
    }
}
```

## Implementation - Active Speaker-Based Policy

The application can reorder video tiles based on the active speaker policy. To implement this, when `ActiveSpeakerPolicy` detects new active speakers, we sort `remoteVideoTileStates` accordingly so that videos from active speakers are promoted to the top of the list.

```kotlin
fun updateRemoteVideoStatesBasedOnActiveSpeakers(activeSpeakers: Array<AttendeeInfo>) {
    val activeSpeakerIds = activeSpeakers.map { it.attendeeId }.toHashSet()

    remoteVideoTileStates.sortWith(Comparator<VideoCollectionTile> { lhs, rhs ->
        val lhsIsActiveSpeaker = activeSpeakerIds.contains(lhs.videoTileState.attendeeId)
        val rhsIsActiveSpeaker = activeSpeakerIds.contains(rhs.videoTileState.attendeeId)

        when {
            lhsIsActiveSpeaker && !rhsIsActiveSpeaker -> -1
            !lhsIsActiveSpeaker && rhsIsActiveSpeaker -> 1
            else -> 0
        }
    })
}

override fun onActiveSpeakerDetected(attendeeInfo: Array<AttendeeInfo>) {
    uiScope.launch {
        updateRemoteVideoStatesBasedOnActiveSpeakers(attendeeInfo)

        updateRemoteVideoStatesOnCurrentPage()
        videoTileAdapter.notifyDataSetChanged()
        resumeAllRemoteVideosOnCurrentPageExceptUserPausedVideos()

        refreshVideoUI()
    }
}
```
