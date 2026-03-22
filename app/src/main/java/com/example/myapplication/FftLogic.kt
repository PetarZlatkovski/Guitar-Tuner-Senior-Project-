package com.example.myapplication

import kotlin.math.cos
import kotlin.math.sin

class FftLogic(val size: Int) {

    init { require(size > 0 && size and (size - 1) ==0) {"FFT must be a power of 2"} }

    val real = DoubleArray(size)
    val imag = DoubleArray(size)

    //Precomputed once - reused every frame, avoids allocation on audio thread
    private val hannWindow = DoubleArray(size) { i ->
        0.5 * (1.0 - cos(2.0 * Math.PI * i / (size - 1)))
    }

    fun compute(samples: ShortArray) {
        //Normalize to [-1, 1] and apply Hann window in one pass
        for (i in 0 until size) {
            real[i] = (samples[i] / 32768.0) * hannWindow[i]
            imag[i] = 0.0
        }
        // Bit-reversal permutation
        var j = 0
        for (i in 1 until size) {
            var bit = size shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1}
            j = j xor bit
            if (i < j) {
                real[i] = real[j].also { real[j] = real[i] }
                imag[i] = imag[j].also { imag[j] = imag[i] }
            }
        }
        //Cooley-Tukey implementation
        var len = 2
        while (len <= size) {
            val ang = -2.0 * Math.PI / len
            val baseRe = cos(ang)
            val baseIm = sin(ang)
            var i = 0
            while (i < size) {
                var wRe = 1.0; var wIm = 0.0
                for (k in 0 until len / 2) {
                    val uRe = real[i + k]; val uIm = imag[i + k]
                    val tRe = wRe * real[i+k+len/2] - wIm * imag[i+k+len/2]
                    val tIm = wRe * imag[i+k+len/2] + wIm * real[i+k+len/2]
                    real[i + k]     = uRe + tRe; imag[i + k]     = uIm + tIm
                    real[i+k+len/2] = uRe - tRe; imag[i+k+len/2] = uIm - tIm
                    val nextRe = wRe * baseRe - wIm * baseIm
                    wIm = wRe * baseIm + wIm * baseRe; wRe = nextRe
                }
                i += len
            }
            len = len shl 1
        }
    }
}