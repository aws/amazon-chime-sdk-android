/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.ingestion.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.Cursor.FIELD_TYPE_BLOB
import android.database.Cursor.FIELD_TYPE_FLOAT
import android.database.Cursor.FIELD_TYPE_INTEGER
import android.database.Cursor.FIELD_TYPE_NULL
import android.database.Cursor.FIELD_TYPE_STRING
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger

const val DATABASE_NAME = "AmazonChimeSDKEvent.db"
const val IN_MEMORY_DATABASE = ":memory:"

class SQLiteDatabaseManager(
    context: Context,
    private val logger: Logger,
    databaseName: String = DATABASE_NAME
) :
    // Context, DatabaseName, Factory (We have no factory), version (It will be 1 for first version)
    SQLiteOpenHelper(context, databaseName, null, 1), DatabaseManager {
    private val TAG = "SQLiteDatabaseManager"
    private val invalidDeleteCount = -1

    override fun onCreate(database: SQLiteDatabase?) {
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // We don't do anything on upgrade. We'll create different table
    }

    override fun createTable(table: DatabaseTable): Boolean {
        val builder = StringBuilder()
        // TODO: sanitize tableName
        builder.append("CREATE TABLE IF NOT EXISTS ${table.tableName} ")

        val primaryKeyList =
            listOf("${table.primaryKey.first} ${table.primaryKey.second} PRIMARY KEY")
        val columnList =
            table.columns.entries.map { entry -> "${entry.key} ${entry.value}" }.toList()

        val keyList = primaryKeyList + columnList
        builder.append("(${keyList.joinToString(",")})")
        return try {
            writableDatabase.execSQL(builder.toString())
            true
        } catch (exception: Exception) {
            logger.error(TAG, "Unable to create table $builder: ${exception.localizedMessage}")
            false
        }
    }

    override fun query(tableName: String, size: Int?): List<Map<String, Any?>> {
        val retrievedData = mutableListOf<Map<String, Any?>>()
        try {
            val cursor =
                writableDatabase.query(tableName, null, null, null, null, null, null, size?.toString())
            cursor.use {
                while (it.moveToNext()) {
                    try {
                        retrievedData.add(retrieveColumn(it))
                    } catch (exception: Exception) {
                        logger.error(
                            TAG,
                            "Unable to query an item from $tableName: ${exception.localizedMessage}"
                        )
                    }
                }
            }
        } catch (exception: Exception) {
            logger.error(TAG, "Unable to obtain data from $tableName: ${exception.message}")
        }

        return retrievedData
    }

    override fun dropTable(tableName: String): Boolean {
        // TODO: sanitize tableName
        return try {
            writableDatabase.execSQL("DROP TABLE IF EXISTS $tableName;")
            true
        } catch (exception: Exception) {
            logger.error(
                TAG,
                "Unable to drop table $tableName: ${exception.localizedMessage}"
            )
            false
        }
    }

    override fun insert(
        tableName: String,
        contentValues: List<ContentValues>
    ): Boolean {
        if (contentValues.isEmpty()) return true

        // Handle multiple
        writableDatabase.beginTransaction()

        try {
            for (contentValue in contentValues) {
                writableDatabase.insertOrThrow(tableName, null, contentValue)
            }
            writableDatabase.setTransactionSuccessful()
        } catch (exception: Exception) {
            logger.error(
                TAG,
                "Unable to insert items into $tableName: ${exception.localizedMessage}"
            )
            return false
        } finally {
            writableDatabase.endTransaction()
        }
        return true
    }

    override fun delete(tableName: String, keyName: String, ids: List<String>): Int {
        if (ids.isEmpty()) return 0

        val idsString = ids.toTypedArray()
        val whereClause = ids.joinToString(",") { "?" }
        return delete(tableName, "$keyName in ($whereClause)", idsString)
    }

    override fun clear(tableName: String): Boolean {
        return delete(tableName, null, null) != invalidDeleteCount
    }

    private fun retrieveColumn(cursor: Cursor): Map<String, Any?> {
        val column = mutableMapOf<String, Any?>()
        val columnCount = cursor.columnCount
        for (columnIndex in 0 until columnCount) {
            if (!cursor.isNull(columnIndex)) {
                column[cursor.getColumnName(columnIndex)] = retrieveValue(cursor, columnIndex)
            }
        }

        return column
    }

    private fun retrieveValue(cursor: Cursor, columnIndex: Int): Any? {
        return when (cursor.getType(columnIndex)) {
            FIELD_TYPE_NULL -> null
            FIELD_TYPE_FLOAT -> cursor.getDouble(columnIndex)
            FIELD_TYPE_STRING -> cursor.getString(columnIndex)
            FIELD_TYPE_INTEGER -> cursor.getLong(columnIndex)
            FIELD_TYPE_BLOB -> cursor.getBlob(columnIndex)
            else -> null
        }
    }

    private fun delete(tableName: String, whereClause: String?, whereArgs: Array<String>?): Int {
        return try {
            writableDatabase.delete(tableName, whereClause, whereArgs)
        } catch (exception: Exception) {
            logger.error(
                TAG,
                "Unable to delete from $tableName, whereClause: $whereClause, whereArgs: ${whereArgs?.joinToString(
                    ","
                )}: ${exception.localizedMessage}"
            )
            invalidDeleteCount
        }
    }
}
