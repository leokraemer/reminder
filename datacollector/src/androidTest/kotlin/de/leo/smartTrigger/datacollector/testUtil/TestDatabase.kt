package de.leo.smartTrigger.datacollector.testUtil

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabase
import de.leo.smartTrigger.datacollector.datacollection.database.JitaiDatabaseOpenHelper
import de.leo.smartTrigger.datacollector.datacollection.database.TABLE_SENSORDATA
import de.leo.smartTrigger.datacollector.datacollection.database.TIMESTAMP
import de.leo.smartTrigger.datacollector.datacollection.models.SensorDataSet
import de.leo.smartTrigger.datacollector.utils.getObjectListFromCursor
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.IllegalStateException


class TestDatabase(context: Context) : JitaiDatabase(context) {

    override fun initializeDatabase(context: Context): SQLiteOpenHelper =
        MyDatabaseOpenHelper(context, null, 1025)

    fun getSensorDataSets(count: Int): List<SensorDataSet> {
        val c = db.readableDatabase.query(TABLE_SENSORDATA, null,
                                          null,
                                          null,
                                          null,
                                          null,
                                          TIMESTAMP,
                                          "$count")
        return getObjectListFromCursor(c, ::sensorDataSetFromCursor)
    }

    fun getAllSensorDataSets(): List<SensorDataSet> {
        val c = db.readableDatabase.query(TABLE_SENSORDATA, null,
                                          null,
                                          null,
                                          null,
                                          null,
                                          TIMESTAMP)
        return getObjectListFromCursor(c, ::sensorDataSetFromCursor)
    }

    class MyDatabaseOpenHelper(val context: Context,
                               name: String? = null,
                               val version: Int = 1025) : JitaiDatabaseOpenHelper
                                                          (context,
                                                           name,
                                                           version) {
        override fun onCreate(db: SQLiteDatabase?) {
            BufferedReader(InputStreamReader(context.getAssets().open("databases/testdb.sql"))).apply {
                lines().forEach {
                    try {
                        db!!.execSQL(it)
                    } catch (e: SQLiteException) {
                        Log.w("generate test db", e)
                    }
                }
                close()
            }

            onUpgrade(db, 1, version)
        }

        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
            super.onUpgrade(db, oldVersion, newVersion)
        }
    }

    fun swapContext(context: Context) {
        if (db.writableDatabase.isOpen)
            this.context = context
        else
            throw IllegalStateException("db was not created yet")
    }
}


