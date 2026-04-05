package com.example.myapplication

class KalmanFilter(
    private val processNoise: Double = 0.1,
    private val measurementNoise: Double = 25.0
) {
    private var estimate = 0.0
    private var errorCovariance = 1.0
    private var initialized = false

    fun update(measurement: Double): Double {
        if (!initialized) {
            estimate = measurement
            initialized = true
            return estimate
        }

        val predictedEstimate = estimate
        val predictedCovariance = errorCovariance + processNoise

        val kalmanGain = predictedCovariance / (predictedCovariance + measurementNoise)
        estimate = predictedEstimate + kalmanGain * (measurement - predictedEstimate)
        errorCovariance = (1 - kalmanGain) * predictedCovariance

        return estimate

    }

    fun reset() {
        initialized = false
        estimate = 0.0
        errorCovariance = 1.0
    }
}