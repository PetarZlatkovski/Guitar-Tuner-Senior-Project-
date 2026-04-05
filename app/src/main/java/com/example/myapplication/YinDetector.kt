package com.example.myapplication

import kotlin.math.abs

class YinDetector (
    private val sampleRate: Int = 44100,
    private val bufferSize: Int = 2048,     //half buffer to avoid lag
    private val threshold: Double = 0.12
) {
    private val yinBufferSize = bufferSize
    private val yinBuffer = DoubleArray(yinBufferSize)

    private val minLag = (sampleRate / 330.0).toInt()   //134 samples
    private val maxLag = (sampleRate / 80.0).toInt()    //551 samples

    fun detect(buffer: ShortArray): Double? {
        require(buffer.size >= bufferSize)

        computeDifferenceFunction(buffer)

        computeCumulativeMeanNormalized()

        val tau = findTrough() ?: return null

        val refinedTau = interpolate(tau)

        return sampleRate / refinedTau
    }

    private fun computeDifferenceFunction(buffer: ShortArray) {
        for (tau in 0 until yinBufferSize) {
            var sum = 0.0
            for (j in 0 until (buffer.size - tau)) {
                val delta = buffer[j].toDouble() - buffer[j + tau].toDouble()
                sum += delta * delta
            }
            yinBuffer[tau] = sum
        }
    }

    private fun computeCumulativeMeanNormalized() {
        yinBuffer[0] = 1.0
        var runningSum = 0.0
        for (tau in 1 until yinBufferSize) {
            runningSum += yinBuffer[tau]
            yinBuffer[tau] = if (runningSum == 0.0) 1.0
                            else yinBuffer[tau] * tau / runningSum
        }
    }

    private fun findTrough(): Int?{
        var tau = minLag
        while (tau <maxLag) {
            if (yinBuffer[tau] < threshold) {
                while (tau + 1 <maxLag && yinBuffer[tau + 1] < yinBuffer[tau]) tau++
                return tau
            }
            tau++
        }
        return null
    }

    private fun interpolate(tau: Int): Double {
        if (tau <= 0 || tau >= yinBufferSize - 1) return tau.toDouble()
        val alpha = yinBuffer[tau - 1]
        val beta = yinBuffer[tau]
        val gamma = yinBuffer[tau + 1]
        val denom = alpha - 2.0 * beta + gamma
        return if (abs(denom) < 1e-10) tau.toDouble()
               else tau - 0.5 * (alpha - gamma) / denom
    }
}
