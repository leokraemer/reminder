package com.example.leo.datacollector.utils

import java.util.*

/**
 * Created by Leo on 12.12.2017.
 */
fun averageDequeue(dequeue: ArrayDeque<FloatArray>): FloatArray {
    var sum: FloatArray = dequeue.fold(floatArrayOf(0F, 0F, 0F), { acc, value ->
        acc[0] += value[0]
        acc[1] += value[1]
        acc[2] += value[2]
        acc
    })
    sum[0] = sum[0] / dequeue.size
    sum[1] = sum[1] / dequeue.size
    sum[2] = sum[2] / dequeue.size
    return sum
}

