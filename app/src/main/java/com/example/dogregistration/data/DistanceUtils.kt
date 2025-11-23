package com.example.dogregistration.data

fun euclideanDistance(a: FloatArray, b: FloatArray): Float {
    val n = minOf(a.size, b.size)
    var sum = 0f
    for (i in 0 until n) {
        val d = a[i] - b[i]
        sum += d * d
    }
    return kotlin.math.sqrt(sum)
}