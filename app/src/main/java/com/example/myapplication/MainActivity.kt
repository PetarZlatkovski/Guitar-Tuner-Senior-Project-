package com.example.myapplication

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Button
import android.widget.LinearLayout

class MainActivity : AppCompatActivity() {

    lateinit var noteTextView : TextView
    var hertz : Int = 392
    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        noteTextView = findViewById(R.id.noteLetter)

        // Method 1: Change text with a button click
        val testButton = Button(this)
        testButton.text = "Test Change Note"
        testButton.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // Add button to the layout
        val mainLayout = findViewById<LinearLayout>(R.id.main)
        mainLayout.addView(testButton)

        testButton.setOnClickListener {
            changeNoteBasedOnHertz()
        }


    }

    private fun changeNoteBasedOnHertz() {
        when (hertz) {
            440 -> {
                noteTextView.text = "A"
                hertz = 392  // Change to next test value
            }
            392 -> {
                noteTextView.text = "G"
                hertz = 329
            }
            329 -> {
                noteTextView.text = "E"
                hertz = 440
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()

        handler.removeCallbacksAndMessages(null)
    }
}