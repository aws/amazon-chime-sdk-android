/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.utils

import android.content.ContentValues
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.database.DatabaseTable
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.database.IN_MEMORY_DATABASE
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.database.SQLiteDatabaseManager
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SQLiteDatabaseManagerIntegrationTests {
    private class TestTable : DatabaseTable {
        val pKName = "id"
        val pKType = "TEXT"
        val cDataName = "data"
        val cDataType = "TEXT"
        val cIntName = "number"
        val cIntType = "INTEGER"

        override val tableName: String = "test"
        override val columns: Map<String, String> =
            mapOf(cDataName to cDataType, cIntName to cIntType)
        override val primaryKey: Pair<String, String> = (pKName to pKType)
    }

    private val testTable = TestTable()

    private val contentValues: ContentValues = createEntry(5)

    private class MalformedTestTable : DatabaseTable {
        override val tableName: String = "malformmmeee"
        override val columns: Map<String, String> = mapOf("data" to "TEXTTEEEEEEE")
        override val primaryKey: Pair<String, String> = ("id" to "HELLO YYPEEE;;;;;;")
    }

    companion object {
        private lateinit var sqliteDatabaseManager: SQLiteDatabaseManager

        @BeforeClass
        @JvmStatic
        fun classSetup() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val logger = ConsoleLogger(LogLevel.INFO)
            sqliteDatabaseManager = SQLiteDatabaseManager(context, logger, IN_MEMORY_DATABASE)
        }
    }

    @Before
    fun setup() {
        sqliteDatabaseManager.createTable(testTable)
    }

    @After
    fun tearDown() {
        sqliteDatabaseManager.clear(testTable.tableName)
        dropTable()
        sqliteDatabaseManager.close()
    }

    // Create Table
    @Test
    fun createTableShouldCreateNewTableIfNotExists() {
        dropTable()
        val created = sqliteDatabaseManager.createTable(testTable)

        Assert.assertEquals(true, created)
    }

    @Test
    fun createTableShouldNotFailWhenTableExists() {
        val created = sqliteDatabaseManager.createTable(testTable)

        Assert.assertEquals(true, created)
    }

    @Test
    fun createTableShouldFailWhenGivenColumnIsMalformed() {
        val created = sqliteDatabaseManager.createTable(MalformedTestTable())

        Assert.assertEquals(false, created)
    }

    // Query
    @Test
    fun queryShouldBeAbleToLimitItemSize() {
        val contents = createEntries(5)
        sqliteDatabaseManager.insert(testTable.tableName, contents)

        val resp = sqliteDatabaseManager.query(testTable.tableName, 2)

        Assert.assertEquals(2, resp.size)
    }

    @Test
    fun queryShouldReturnEmptyListWhenTableIsEmpty() {
        val resp = sqliteDatabaseManager.query(testTable.tableName, 10)

        Assert.assertEquals(0, resp.size)
    }

    @Test
    fun queryShouldBeAbleToReturnSameDataAsInserted() {
        val isInsertSuccessful =
            sqliteDatabaseManager.insert(testTable.tableName, listOf(contentValues))
        Assert.assertEquals(true, isInsertSuccessful)

        val expectedId = contentValues.get(testTable.pKName)
        val expectedData = contentValues.get(testTable.cDataName)
        val expectedNumber = contentValues.get(testTable.cIntName)
        val resp = sqliteDatabaseManager.query(testTable.tableName, 2)

        Assert.assertEquals(1, resp.size)
        Assert.assertEquals(expectedId, resp[0][testTable.pKName])
        Assert.assertEquals(expectedData, resp[0][testTable.cDataName])
        Assert.assertEquals((expectedNumber as Int).toLong(), resp[0][testTable.cIntName])
    }

    @Test
    fun queryItemShouldReturnEmptyListWhenTableDoesNotExist() {
        dropTable()
        val resp = sqliteDatabaseManager.query(testTable.tableName, 2)

        Assert.assertEquals(0, resp.size)
    }

    // Insert
    @Test
    fun insertShouldBeAbleToInsertAnItem() {
        val isInsertSuccessful =
            sqliteDatabaseManager.insert(testTable.tableName, listOf(contentValues))

        Assert.assertEquals(true, isInsertSuccessful)
    }

    @Test
    fun insertItemShouldReturnFalseWhenTableDoesNotExist() {
        dropTable()
        val isInsertSuccessful =
            sqliteDatabaseManager.insert(testTable.tableName, listOf(contentValues))

        Assert.assertEquals(false, isInsertSuccessful)
    }

    @Test
    fun insertShouldBeAbleToInsertEmptyList() {
        val isInsertSuccessful =
            sqliteDatabaseManager.insert(testTable.tableName, listOf())

        Assert.assertEquals(true, isInsertSuccessful)
    }

    @Test
    fun insertShouldFailWhenInsertingAnMalformedItem() {
        val malformedContent = ContentValues().apply {
            put("malformed zzzz", "zzzzzz")
            put("malformed2", "ewww")
        }
        val isInsertSuccessful =
            sqliteDatabaseManager.insert(testTable.tableName, listOf(malformedContent))

        Assert.assertEquals(false, isInsertSuccessful)
    }

    @Test
    fun insertMultipleShouldBeAbleToInsertMultipleItems() {
        val size = 5
        val contents = createEntries(size)

        val isInsertSuccessful = sqliteDatabaseManager.insert(testTable.tableName, contents)

        Assert.assertEquals(true, isInsertSuccessful)

        val items = sqliteDatabaseManager.query(testTable.tableName, size)

        Assert.assertEquals(size, items.size)
        val itemsIdSet = items.map { it[testTable.pKName] }.toSet()

        for (content in contents) {
            Assert.assertTrue(itemsIdSet.contains(content[testTable.pKName]))
        }
    }

    @Test
    fun insertShouldFailToInsertTheSameItemAgain() {
        val isInsertSuccessful1 =
            sqliteDatabaseManager.insert(testTable.tableName, listOf(contentValues))
        val isInsertSuccessful2 =
            sqliteDatabaseManager.insert(testTable.tableName, listOf(contentValues))

        Assert.assertEquals(true, isInsertSuccessful1)
        Assert.assertEquals(false, isInsertSuccessful2)

        val resp = sqliteDatabaseManager.query(testTable.tableName, 10)

        Assert.assertEquals(1, resp.size)
    }

    // DELETE
    @Test
    fun deleteShouldBeAbleToDeleteAnItem() {
        val isInsertSuccessful =
            sqliteDatabaseManager.insert(testTable.tableName, listOf(contentValues))
        Assert.assertEquals(true, isInsertSuccessful)

        val id = contentValues.get(testTable.pKName)
        val deleted = sqliteDatabaseManager.delete(
            testTable.tableName,
            testTable.pKName,
            listOf(id as String)
        )

        Assert.assertEquals(1, deleted)
    }

    @Test
    fun deleteShouldReturnZeroWhenEmptyTable() {
        val deleted = sqliteDatabaseManager.delete(
            testTable.tableName,
            testTable.pKName,
            listOf("abc")
        )

        Assert.assertEquals(0, deleted)
    }

    @Test
    fun deleteShouldBeAbleToDeleteMultipleItems() {
        val count = 3
        val contents = createEntries(count)
        sqliteDatabaseManager.insert(testTable.tableName, contents)

        val resp = sqliteDatabaseManager.query(testTable.tableName, count)
        val ids = resp.map { it[testTable.pKName] as String }
        val deleted =
            sqliteDatabaseManager.delete(testTable.tableName, testTable.pKName, ids)

        Assert.assertEquals(count, deleted)
    }

    @Test
    fun deleteShouldBeAbleToDeleteItemsFewerThanInserted() {
        val count = 5
        val expectedDeletedCount = 3
        val contents = createEntries(count)
        sqliteDatabaseManager.insert(testTable.tableName, contents)

        val resp = sqliteDatabaseManager.query(testTable.tableName, expectedDeletedCount)
        val ids = resp.map { it[testTable.pKName] as String }
        val deleted =
            sqliteDatabaseManager.delete(testTable.tableName, testTable.pKName, ids)

        Assert.assertEquals(expectedDeletedCount, deleted)

        val resp2 = sqliteDatabaseManager.query(testTable.tableName, count)

        Assert.assertEquals(count - expectedDeletedCount, resp2.size)
    }

    @Test
    fun deleteEventItemShouldDeleteValidEventItemsWhenPassedWithExtraneousEventItems() {
        val isInsertSuccessful =
            sqliteDatabaseManager.insert(testTable.tableName, listOf(contentValues))

        Assert.assertEquals(true, isInsertSuccessful)

        val id = contentValues.get(testTable.pKName)

        val deleted = sqliteDatabaseManager.delete(
            testTable.tableName,
            testTable.pKName,
            listOf(id as String, "fake-id", "exxxx-id-didid-ddd")
        )

        Assert.assertEquals(1, deleted)

        val resp = sqliteDatabaseManager.query(testTable.tableName, 2)

        Assert.assertEquals(0, resp.size)
    }

    @Test
    fun deleteItemsShouldReturnErrorWhenTableDoesNotExist() {
        dropTable()
        val deletedCount = sqliteDatabaseManager.delete(
            testTable.tableName,
            testTable.pKName,
            listOf("aese-dsdfdf-sdfd")
        )
        Assert.assertEquals(-1, deletedCount)
    }

    @Test
    fun clearShouldDeleteAllItems() {
        val contents = createEntries(5)
        val inserted = sqliteDatabaseManager.insert(testTable.tableName, contents)
        val removed = sqliteDatabaseManager.clear(testTable.tableName)

        Assert.assertEquals(true, inserted)
        Assert.assertEquals(true, removed)

        val resp = sqliteDatabaseManager.query(testTable.tableName, 5)

        Assert.assertEquals(0, resp.size)
    }

    // DROP
    @Test
    fun dropTableShouldReturnTrueWhenTableDoesNotExist() {
        dropTable()
        val dropped = sqliteDatabaseManager.dropTable(testTable.tableName)

        Assert.assertTrue(dropped)
    }

    @Test
    fun dropTableShouldReturnTrueWhenTableExist() {
        sqliteDatabaseManager.createTable(testTable)
        val dropped = sqliteDatabaseManager.dropTable(testTable.tableName)

        Assert.assertTrue(dropped)
    }

    @Test
    fun dropTableShouldReturnFalseWhenInputIsMalformed() {
        val dropped = sqliteDatabaseManager.dropTable("zzz:Ewewer;;;;")

        Assert.assertFalse(dropped)
    }

    private fun dropTable() {
        sqliteDatabaseManager.dropTable(testTable.tableName)
    }

    private fun createEntry(id: Int): ContentValues =
        ContentValues().apply {
            put(testTable.pKName, id.toString())
            put(testTable.cDataName, "data$id")
            put(testTable.cIntName, id)
        }

    private fun createEntries(size: Int): List<ContentValues> =
        (1..size).map { createEntry(it) }.toList()
}
