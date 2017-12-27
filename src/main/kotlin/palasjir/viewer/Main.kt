package palasjir.viewer


import com.jogamp.opengl.GLCapabilities
import com.jogamp.opengl.GLProfile
import com.jogamp.opengl.util.FPSAnimator
import palasjir.viewer.ui.VisualizationPanel

import javax.swing.*
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

private val CANVAS_WIDTH = 500
private val CANVAS_HEIGHT = 500
private val FPS = 25

fun main(args: Array<String>) {

    // Run the GUI codes in the event-dispatching thread for thread safety
    SwingUtilities.invokeLater {

        // Create the OpenGL rendering canvas
        val dimension = Dimension(CANVAS_WIDTH, CANVAS_HEIGHT)
        val caps = GLCapabilities(GLProfile.getMaxProgrammableCore(true))
        val canvas = VisualizationPanel(dimension, caps)

        // Create a animator that drives canvas' render() at the specified FPS.
        val animator = FPSAnimator(canvas, FPS, true)

        // Create the top-level container
        val frame = JFrame() // Swing's JFrame or AWT's Frame
        frame.contentPane.add(canvas)
        val closingThread = Thread {
            if (animator.isStarted) animator.stop()
            System.exit(0)
        }
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                closingThread.start()
            }
        })
        frame.title = "CT Viewer"
        frame.pack()
        frame.isVisible = true
        animator.start() // start the animation loop
    }

}