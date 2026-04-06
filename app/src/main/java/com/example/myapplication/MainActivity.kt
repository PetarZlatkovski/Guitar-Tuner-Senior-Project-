package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
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

    private val tag = "GuitarTuner"
    private val requestAudioPermission = 200
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val fftSize = 4096

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null
    private val detector = PitchDetector(44100, 4096)
    private val kalman = KalmanFilter()
    private val noteTree = NoteTree.buildGuitarRange()
    private lateinit var tuningMeter: TuningMeterView

    private val noteHistory = ArrayDeque<String>(8)
    private val historySize = 8
    private var lastDisplayedNote = "--"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tuningMeter = findViewById(R.id.tuningMeter)
        Log.d(tag, "NoteTree loaded: ${noteTree.size()} notes")
        noteTree.inOrder().forEach { Log.d(tag, "  ${it.name} = ${"%.2f".format(it.frequency)} Hz") }
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                requestAudioPermission
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestAudioPermission && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startAudioCapture()
        }
    }

    override fun onPause() {
        super.onPause()
        isRecording = false
        recordingThread?.join()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    override fun onResume() {
        super.onResume()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            startAudioCapture()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun startAudioCapture() {
        if (isRecording) return

        val bufferSize = maxOf(
            AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat),
            fftSize * 2
        )

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        audioRecord?.startRecording()
        isRecording = true

        recordingThread = Thread {
            val fftBuffer = ShortArray(fftSize)
            try {
                while (isRecording) {
                    val readResult = audioRecord?.read(fftBuffer, 0, fftSize) ?: 0


                    if (readResult == fftSize) {

                        val maxAmp = fftBuffer.maxOfOrNull { kotlin.math.abs(it.toInt()) } ?: 0
                        if (maxAmp < 1500) {
                            runOnUiThread { findViewById<TextView>(R.id.noteLetter).text = "--" }
                            continue
                        }

                        val result = detector.process(fftBuffer)
                        if (result == null) {
                            kalman.reset()
                            noteHistory.clear()
                            lastDisplayedNote = "--"
                            runOnUiThread {
                                tuningMeter.setCents(null)
                                findViewById<TextView>(R.id.noteLetter).text = "--"
                                findViewById<TextView>(R.id.centsLabel).text = "-- cents"
                                findViewById<TextView>(R.id.freqLabel).text  = "-- Hz"
                            }
                        }
                        } else {
                        if (result.confidence == "HIGH") {
                            noteHistory.addLast(displayNote)
                            if (noteHistory.size > historySize) noteHistory.removeFirst()
                        }


                        val majorityNote = noteHistory
                            .groupingBy { it }
                            .eachCount()
                            .maxByOrNull { it.value }
                            ?.takeIf { it.value >= historySize / 2 }
                            ?.key

                        if (majorityNote != null && majorityNote != lastDisplayedNote) {
                            lastDisplayedNote = majorityNote
                        }

                        runOnUiThread {
                            tuningMeter.setCents(smoothedCents)
                            findViewById<TextView>(R.id.noteLetter).text = lastDisplayedNote
                            findViewById<TextView>(R.id.centsLabel).text = "${"%.1f".format(smoothedCents)} cents"
                            findViewById<TextView>(R.id.freqLabel).text  = "${"%.1f".format(smoothedHz)} Hz"
                        }
                        }
                    }

            } catch (e: Exception) {
                Log.e(tag, "Recording thread crashed: ${e.javaClass.simpleName} — ${e.message}", e)
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