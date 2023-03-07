/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

/**
 * [EventClientType] defines the type of event client configuration that will be
 * sent to the server.
 */
enum class EventClientType {
    /**
     * Meeting related type
     */
    Meet
}

/**
 * [EventClientConfiguration] defines core properties needed for every event client configuration.
 */
interface EventClientConfiguration {
    /**
     * type: [EventClientType] - type of [EventClientConfiguration]
     */
    val type: EventClientType

    /**
     * eventClientJoinToken: [String] - authentication token needed for ingestion url
     */
    val eventClientJoinToken: String

    /**
     * tag: [String] - tagging the source of the events, which will be translated to Type for Ingestion Event
     */
    val tag: String

    /**
     * metadataAttributes: [Map<String, Any>] - the attributes that will be sent to Ingestion Service as metadata
     */
    val metadataAttributes: Map<String, Any>
}
