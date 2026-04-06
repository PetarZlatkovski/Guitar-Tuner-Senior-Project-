package com.example.myapplication

import kotlin.math.abs
import kotlin.math.pow

class NoteTree {

    data class Note(
        val name: String,
        val frequency: Double
    )

    private inner class Node(val note: Note) {
        var left:  Node? = null
        var right: Node? = null
    }

    private var root: Node? = null
    private var size = 0

    fun insert(name: String, frequency: Double) {
        root = insertRec(root, Note(name, frequency))
        size++
    }

    private fun insertRec(node: Node?, note: Note): Node {
        if (node == null) return Node(note)
        return when {

            note.frequency < node.note.frequency -> {
                node.left  = insertRec(node.left,  note); node }

            note.frequency > node.note.frequency -> {
                node.right = insertRec(node.right, note); node }

            else -> node
        }
    }

    fun findClosest(frequency: Double): Note? {
        return findClosestRec(root, frequency, null)
    }

    private fun findClosestRec(node: Node?, frequency: Double, best: Note?): Note? {
        if (node == null) return best


        val newBest = if (best == null ||
            abs(node.note.frequency - frequency) < abs(best.frequency - frequency)
        )
            node.note else best


        return if (frequency < node.note.frequency)
            findClosestRec(node.left,  frequency, newBest)
        else
            findClosestRec(node.right, frequency, newBest)
    }

    fun inOrder(): List<Note> {
        val result = mutableListOf<Note>()
        inOrderRec(root, result)
        return result
    }

    private fun inOrderRec(node: Node?, result: MutableList<Note>) {
        node ?: return
        inOrderRec(node.left, result)
        result.add(node.note)
        inOrderRec(node.right, result)
    }

    fun size() = size

    companion object {

        fun buildGuitarRange(): NoteTree {
            val tree = NoteTree()
            val noteNames = arrayOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")


            for (midi in 40..76) {
                val freq = 440.0 * 2.0.pow((midi - 69.0) / 12.0)
                val octave = (midi / 12) - 1
                val name = noteNames[midi % 12] + octave
                tree.insert(name, freq)
            }
            return tree
        }
    }
}