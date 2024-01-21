package com.example.poraprojekt

data class HistoryItem(
    val sensorType: String,
    val sensorData: String,
    val absoluteTime: String,
    val location: String,
    val casTrajanja: Long
)