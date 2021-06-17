/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.ingestion.database

import android.content.ContentValues

interface DatabaseManager {
    /**
     * Create Table based on the primary keys and other keys defined.
     *
     * @param table: [DatabaseTable] - Table to create
     * @return whether creating table was successful or not
     */
    fun createTable(table: DatabaseTable): Boolean

    /**
     * Drop/Delete Table based on given table name
     *
     * @param tableName: [String] - Table name to drop
     * @return whether dropping table was successful or not
     */
    fun dropTable(tableName: String): Boolean

    /**
     * Query table based on size. Since the order is not specified, it will most likely get based on
     * insertion order.
     *
     * @param tableName: [String] - Name of table to query
     * @param size: [Int] - size to query. It will return fewer if table contains fewer.
     * @return list of map whose key is column name and value is value of that column.
     */
    fun query(tableName: String, size: Int? = 1000): List<Map<String, Any?>>

    /**
     * Insert a list of items into the table.
     *
     * @param tableName: [String] - name of table
     * @param contentValues: [List<ContentValues>] - values to insert
     * @return whether insertion was successful or not.
     */
    fun insert(tableName: String, contentValues: List<ContentValues>): Boolean

    /**
     * Delete items based on the table name, given primary key, and values.
     *
     * @param tableName: [String] - name of table
     * @param keyName: [String] - column name of primary key
     * @param ids: [List<String>] - ids to delete
     * @return number of rows deleted. Returns -1 if there is error
     */
    fun delete(tableName: String, keyName: String, ids: List<String>): Int

    /**
     * Clear the table.
     *
     * @param tableName: [String] - name of table
     * @return whether clear was successful or not.
     */
    fun clear(tableName: String): Boolean
}
