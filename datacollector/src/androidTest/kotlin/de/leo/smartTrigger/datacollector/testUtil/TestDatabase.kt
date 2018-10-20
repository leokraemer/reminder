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


class TestDatabase private constructor(val dbFileName: String,
                                       val oldVersion: Int,
                                       val newVersion: Int,
                                       context: Context) : JitaiDatabase(context) {

    companion object {
        @Volatile
        private var INSTANCE: TestDatabase? = null

        fun getInstance(dbFileName: String,
                        oldVersion: Int,
                        newVersion: Int,
                        context: Context): TestDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: TestDatabase(dbFileName, oldVersion, newVersion, context).also {
                    INSTANCE = it
                    INSTANCE!!.db =
                        MyDatabaseOpenHelper(context, dbFileName, null, oldVersion, newVersion)
                }
            }


        const val NAME = "mydb.1018"
    }


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
                               val databaseFileName: String,
                               name: String? = null,
                               val oldVersion: Int,
                               val newVersion: Int = 1025) : JitaiDatabaseOpenHelper
                                                             (context,
                                                              name,
                                                              newVersion) {
        override fun onCreate(db: SQLiteDatabase?) {
            BufferedReader(
                InputStreamReader(context.getAssets().open("databases/$databaseFileName"))).apply {
                forEachLine {
                    try {
                        db!!.execSQL(it)
                    } catch (e: SQLiteException) {
                        Log.w("generate test db", e)
                    }
                }
                close()
            }
            onUpgrade(db, oldVersion, newVersion)
        }
    }

    fun swapContext(context: Context) {
        //initialize the database by opening
        if (db.writableDatabase.isOpen)
        //swap the context that is used to create objects
            this.context = context
        else
            throw IllegalStateException("db was not created yet")
    }
}


