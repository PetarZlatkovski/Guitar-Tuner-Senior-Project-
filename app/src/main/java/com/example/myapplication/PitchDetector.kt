package com.example.myapplication

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class TunerResult(
    val note: String,
    val cents: Float,
    val frequency: Double
)

class PitchDetector(
    private val sampleRate: Int = 44100,
    private val fftSize: Int = 4096
) {
    private val fft = FftLogic(fftSize)
    private val freqResolution = sampleRate.toDouble() / fftSize

    private val minBin = (80.0  / freqResolution).toInt()
    private val maxBin = (1100.0 / freqResolution).toInt().coerceAtMost(fftSize / 2 - 2)

    private val NOTE_NAMES = arrayOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")
    private val SILENCE_THRESHOLD = 0.008f

    fun process(buffer: ShortArray): TunerResult? {
        fft.compute(buffer)

        val magnitudes = FloatArray(fftSize / 2) { i ->
            sqrt(fft.real[i] * fft.real[i] + fft.imag[i] * fft.imag[i]).toFloat()
        }

        // HPS (Harmonic Product Spectrum)
        val hps = magnitudes.copyOf()
        for (i in minBin..maxBin) {
            if (i * 2 < magnitudes.size) hps[i] *= magnitudes[i * 2]
            if (i * 3 < magnitudes.size) hps[i] *= magnitudes[i * 3]
        }

        // Find peak in HPS output, not raw magnitudes
        var peakBin = minBin
        for (i in minBin..maxBin) {
            if (hps[i] > hps[peakBin]) peakBin = i
        }

        if (magnitudes[peakBin] < SILENCE_THRESHOLD) return null

        // Parabolic interpolation
        val alpha  = magnitudes[peakBin - 1]
        val beta   = magnitudes[peakBin]
        val gamma  = magnitudes[peakBin + 1]
        val denom  = alpha - 2f * beta + gamma
        // If denom is ~0 the peak is flat — skip interpolation rather than produce NaN
        val offset = if (abs(denom) < 1e-10f) 0.0 else 0.5 * (alpha - gamma) / denom
        val refinedFreq = (peakBin + offset) * freqResolution

        // Guard: NaN or non-positive freq would crash Math.log
        if (refinedFreq.isNaN() || refinedFreq <= 0.0) return null

        return frequencyToResult(refinedFreq)
    }

    private fun frequencyToResult(freq: Double): TunerResult {
        val midi        = 12.0 * ln(freq / 440.0) / ln(2.0) + 69.0
        val nearestMidi = midi.roundToInt().coerceIn(0, 127)
        val cents       = ((midi - nearestMidi) * 100.0).toFloat()
        val noteName    = NOTE_NAMES[nearestMidi % 12] + ((nearestMidi / 12) - 1)
        return TunerResult(note = noteName, cents = cents, frequency = freq)
    }
}