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

fun averageDequeueHighPass(dequeue: ArrayDeque<FloatArray>): FloatArray {
    var gravity = floatArrayOf(0f, 0f, 0f)
    var sum = floatArrayOf(0f, 0f, 0f)
    val alpha = 0.8f
    val iterator = dequeue.iterator()
    while (iterator.hasNext()) {
        val entry = iterator.next()
        gravity[0] = alpha * gravity[0] + (1 - alpha) * entry[0]
        gravity[1] = alpha * gravity[1] + (1 - alpha) * entry[1]
        gravity[2] = alpha * gravity[2] + (1 - alpha) * entry[2]
        sum[0] += (entry[0] - gravity[0])
        sum[1] += (entry[1] - gravity[1])
        sum[2] += (entry[2] - gravity[2])
    }
    sum[0] = sum[0] / dequeue.size
    sum[1] = sum[1] / dequeue.size
    sum[2] = sum[2] / dequeue.size
    return sum
}