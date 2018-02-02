package com.example.leo.datacollector.activityDetection

import android.content.Context
import android.util.Log
import com.example.leo.datacollector.database.JitaiDatabase
import com.example.leo.datacollector.jitai.manage.Jitai
import com.example.leo.datacollector.models.SensorDataSet
import weka.classifiers.functions.LibSVM
import weka.core.Attribute
import weka.core.DenseInstance
import weka.core.Instances
import weka.core.WekaPackageClassLoaderManager


/**
 * Created by Leo on 21.12.2017.
 */
class ActivityRecognizer(val context: Context) {

    val jitaiList = mutableListOf<Jitai>()
    val db: JitaiDatabase

    val nominal: List<String> = listOf("match", "no_match")
    init {
        db = JitaiDatabase.getInstance(context)
        jitaiList.addAll(db.getActiveJitai())
        val atts = ArrayList<Attribute>()
        atts.add(Attribute("foo"))
        atts.add(Attribute("class", nominal))
    }


    fun recognizeActivity(sensorValues: SensorDataSet) {
        jitaiList.forEach {
            it.check(sensorValues)
        }
    }
}
