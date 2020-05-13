/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.session

/**
 * [URLRewriter] Function to transform URLs.
 * Use this to rewrite URLs to traverse proxies.
 * @param url: Url string
 * @return A new url string manipulated
 */
typealias URLRewriter = (url: String) -> String

/**
 * The default implementation returns the original URL unchanged.
 */
fun defaultUrlRewriter(url: String) = url
