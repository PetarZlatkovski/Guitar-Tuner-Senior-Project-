package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlin.math.ln
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {

    private val TAG = "GuitarTuner"
    private val REQUEST_AUDIO_RECORD_PERMISSION = 200
    private val SAMPLE_RATE = 44100
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val FFT_SIZE = 4096

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null
    private val detector = PitchDetector(44100, 4096)
    private val kalman = KalmanFilter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO_RECORD_PERMISSION)
        } else {
            startAudioCapture()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO_RECORD_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startAudioCapture()
        }
    }

    private fun startAudioCapture() {

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        audioRecord?.startRecording()
        isRecording = true

        recordingThread = Thread {
            val fftBuffer = ShortArray(FFT_SIZE)
            try {
                while (isRecording) {
                    val readResult = audioRecord?.read(fftBuffer, 0, FFT_SIZE) ?: 0

                    // Tells user if AudioRecord itself is failing
                    if (readResult == FFT_SIZE) {
                        // Gate: skip frame entirely if the raw signal is too quiet
                        val maxAmp = fftBuffer.maxOfOrNull { kotlin.math.abs(it.toInt()) } ?: 0
                        if (maxAmp < 1500) {
                            runOnUiThread { findViewById<TextView>(R.id.noteLetter).text = "--" }
                            continue
                        }

                        val result = detector.process(fftBuffer)
                        if (result == null) {
                            kalman.reset()
                            runOnUiThread { findViewById<TextView>(R.id.noteLetter).text = "--" }
                        } else {
                            val smoothedHz   = kalman.update(result.frequency)
                            // Re-derive note name from smoothed frequency
                            val midi         = 12.0 * ln(smoothedHz / 440.0) / ln(2.0) + 69.0
                            val nearestMidi  = midi.roundToInt().coerceIn(0, 127)
                            val noteNames    = arrayOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")
                            val smoothedNote = noteNames[nearestMidi % 12] + ((nearestMidi / 12) - 1)
                            val smoothedCents = ((midi - nearestMidi) * 100.0).toFloat()

                            Log.d(TAG, "Note: $smoothedNote  Cents: ${"%.1f".format(smoothedCents)}  " +
                                    "Hz: ${"%.2f".format(smoothedHz)}  Confidence: ${result.confidence}")

                            runOnUiThread {
                                findViewById<TextView>(R.id.noteLetter).text = smoothedNote
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Recording thread crashed: ${e.javaClass.simpleName} — ${e.message}", e)
            }
        }.apply { start() }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}