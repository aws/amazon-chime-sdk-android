/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.ingestion.database

/**
 * Database Table that defines the columns and the primary key.
 * Currently foreign keys are not supported.
 * Only single primary key is supported.
 */
interface DatabaseTable {
    /**
     * Name of database table.
     */
    val tableName: String

    /**
     * Columns other than primary keys.
     * Key would be column name and value would be type.
     * For example, mapOf("data" to "TEXT")
     */
    val columns: Map<String, String>

    /**
     * A primary key for the database
     */
    val primaryKey: Pair<String, String>
}
