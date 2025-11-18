package com.example.fyp_fitledger.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.collections.contains

class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        // Create table statement
        executeSqlFromFile(db, "create_table.sql")
        executeSqlFromFile(db, "insert_statements.sql")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "mydatabase.db"
        private const val DATABASE_VERSION = 1
    }

    private fun executeSqlFromFile(db: SQLiteDatabase, fileName: String) {
        try {
            val assetFiles = context.assets.list("")
            Log.d("SQLite", "Files in assets: ${assetFiles?.joinToString()}")

            // Check if the file exists
            if (!assetFiles!!.contains(fileName)) {
                Log.e("SQLite", "File '$fileName' not found in assets!")
                return
            }

            val inputStream = context.assets.open(fileName) // Now context is accessible
            val reader = BufferedReader(InputStreamReader(inputStream))

            var line: String?
            val stringBuilder = StringBuilder()

            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line).append("\n")  // Append each line
            }

            reader.close()
            inputStream.close()

            val sqlCommands = stringBuilder.toString().split(";")  // Split by semicolon

            for (command in sqlCommands) {
                if (command.trim().isNotEmpty()) {
                    db.execSQL(command.trim())  // Execute each command
                }
            }
            Log.d("SQLite", "SQL file executed successfully!")

        } catch (e: Exception) {
            Log.e("SQLite", "Error executing SQL file", e)
        }
    }
}