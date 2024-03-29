# Configuring Remote Video Subscriptions

Amazon Chime SDK allows builders to have complete control over the remote videos received by each of their application’s end-users. This can be accomplished using the API [AudioVideoFacade.updateVideoSourceSubscriptions](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-default-audio-video-facade/update-video-source-subscriptions.html). This document explains how to use the the API (introduced in version 0.15.0), and how applications can take advantage of it to meet their use cases.

## Prerequisites

* You have read the [API overview](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/api_overview.md) and have a basic understanding of the components covered in that document.
* You have completed [Getting Started](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/getting_started.md) and have running application which uses the Amazon Chime SDK.

Note: A physical Android device is recommended for a better testing experience.

## Overview of APIs

To utilize [AudioVideoFacade.updateVideoSourceSubscriptions](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-default-audio-video-facade/update-video-source-subscriptions.html) builders must first become aware of existing remote video sources through subscription to [AudioVideoObserver.onRemoteVideoSourceAvailable](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-observer/on-remote-video-source-available.html) and [AudioVideoObserver.onRemoteVideoSourceUnavailable](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-observer/on-remote-video-source-unavailable.html). 
When an attendee joins a meeting for the first time, the former will be called with the complete list of remote video sources at that point of time. The observers will then be called for any differential updates immediately as information is available. 
Builder applications can use the [RemoteVideoSource](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-remote-video-source/index.html) objects as keys in [AudioVideoFacade.updateVideoSourceSubscriptions](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-default-audio-video-facade/update-video-source-subscriptions.html) 
(*Note: these objects must be the same as provided in observer, do not try to duplicate or recreate* *[RemoteVideoSources](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-remote-video-source/index.html)* *or they may not update properly*) to set which video sources to receive and their related preferences. 
Note that if builders do not call [AudioVideoFacade.updateVideoSourceSubscriptions](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-default-audio-video-facade/update-video-source-subscriptions.html), it will automatically subscribe to up to 25 videos.

The Amazon Chime SDK defines [VideoSubscriptionConfiguration](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-subscription-configuration/index.html) as the the means to individually request remote video sources to receive and set their respective priorities. [AudioVideoFacade.updateVideoSourceSubscriptions](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-default-audio-video-facade/update-video-source-subscriptions.html) ensures bandwidth is reserved for video sources with higher priorities. It can be used with video sources from clients sending simulcast and/or clients sending single streams.

Under constrained networks where simulcast is in use, the Amazon Chime SDK may lower the resolution of remote video sources, starting with the lowest priority sources. All video sources are separated into multiple groups by different priorities. If all video sources within same priority group are at the lowest resolution possible, or simulcast is not being used, it may further pause video tiles until the network has recovered. The same operations will be repeated group by group, from priority lowest to highest.

A typical workflow to use this SDK API would be:

1. Monitor callbacks on [AudioVideoObserver.onRemoteVideoSourceAvailable](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-observer/on-remote-video-source-available.html) / [AudioVideoObserver.onRemoteVideoSourceUnavailable](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-observer/on-remote-video-source-unavailable.html) to receive updates on available sources. *Store these sources as they must be used as keys to following function calls.*
2. Create a [VideoSubscriptionConfiguration](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-subscription-configuration/index.html) for each video stream and then call [AudioVideoFacade.updateVideoSourceSubscriptions](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-default-audio-video-facade/update-video-source-subscriptions.html) , mapping the previous stored sources to configuration as desired
    
3. Repeat step 2 as needed to update the desired receiving set of remote sources and their priorities, either due to changes indicated by [AudioVideoObserver.onRemoteVideoSourceAvailable](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-observer/on-remote-video-source-available.html) / [AudioVideoObserver.onRemoteVideoSourceUnavailable](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-observer/on-remote-video-source-unavailable.html) (note that unavailable videos will be automatically unsubscribed from event if provided in [AudioVideoFacade.updateVideoSourceSubscriptions](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-default-audio-video-facade/update-video-source-subscriptions.html) ) or other application events (like switching the current page of videos).

See API documentation pages for specific, lower level details on each of these APIs.

## Example: Using the subscription API to prioritize a featured video tile

Below, we are going to show one potential use case for a “featured video”, that can be built on top of this API. A video tile can be featured by adding a special video layout that makes one video larger then the others, but those details are not provided here. The video with highest priority will not be paused or downgraded if simulcast uplink policy being enabled on the remote side until all of the rest are also downgrading when encountering network constraints.

First we can have an observer on [AudioVideoObserver.onRemoteVideoSourceAvailable](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-observer/on-remote-video-source-available.html) / [AudioVideoObserver.onRemoteVideoSourceUnavailable](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-observer/on-remote-video-source-unavailable.html) and keep track of if which clients are publishing video and store some initial configuration, before calling [AudioVideoFacade.updateVideoSourceSubscriptions](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-default-audio-video-facade/update-video-source-subscriptions.html) with default configurations.


```kotlin
class MeetingModel : ViewModel() {
    val remoteVideoSourceConfigurations = mutableMapOf<RemoteVideoSource, VideoSubscriptionConfiguration>()
}

override fun onRemoteVideoSourceAvailable(sources: List<RemoteVideoSource>) {
    sources.forEach { 
        meetingModel.remoteVideoSourceConfigurations.put(it, VideoSubscriptionConfiguration(VideoPriority.Medium, VideoResolution.High)) 
    }
    audioVideo.updateVideoSourceSubscription(remoteVideoSourceConfigurations, emptyArray());
}

override fun onRemoteVideoSourceUnavailable(sources: List<RemoteVideoSource>) {
    sources.forEach { 
        meetingModel.remoteVideoSourceConfigurations.remove(it)
    }
    // No need to call `updateVideoSourceSubscription`
}

```

To update this code to handle the pinned user case, we can use other application code to determine if a video source is pinned, and if so increase the priority above the default. This may be easier if storing RemoteVideoSource in the UI view, or using some other mapping. The RemoteVideoSource used must be the same as the one provided in [AudioVideoObserver.onRemoteVideoSourceAvailable](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-observer/on-remote-video-source-available.html). *Do not make new `RemoteVideoSources` from scratch.* 

```kotlin
fun pinTile(attendeeId: String) {
    for ((source, configuration) in remoteVideoSourceConfigurations) {
        if (source.attendeeId == attendeeId) {
            configuration.priority = VideoPriority.Highest
        }
    }
 
    audioVideo.updateVideoSourceSubscriptions(remoteVideoSourceConfigurations, emptyArray())
}

```

`updateVideoSourceSubscription` will trigger the remote selection logic based on the priority settings of each video then update the tile management accordingly.
