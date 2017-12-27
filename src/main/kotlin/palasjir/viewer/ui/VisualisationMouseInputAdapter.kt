package palasjir.viewer.ui

import palasjir.viewer.coordinates.Camera

import javax.swing.*
import javax.swing.event.MouseInputAdapter
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent

class VisualisationMouseInputAdapter(private val camera: Camera) : MouseInputAdapter() {

    private var start: Point? = null
    private var end: Point? = null

    override fun mousePressed(e: MouseEvent?) {
        if (SwingUtilities.isLeftMouseButton(e!!)) {
            start = e.point
        }
    }

    override fun mouseDragged(e: MouseEvent?) {
        end = e!!.point
        camera.drag(start, end)
        start = Point(end)
    }

    override fun mouseReleased(e: MouseEvent?) {
        start = null
        end = null
    }

    override fun mouseWheelMoved(e: MouseWheelEvent?) {
        camera.zoom(e!!.wheelRotation.toDouble())
    }

}
