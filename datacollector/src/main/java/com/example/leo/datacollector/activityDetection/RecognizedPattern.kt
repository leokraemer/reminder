package com.example.leo.datacollector.activityDetection

import org.threeten.bp.LocalTime

/**
 * Created by Leo on 21.12.2017.
 */

interface RecognizedPattern{
    val name : String
    val startTime : LocalTime
}