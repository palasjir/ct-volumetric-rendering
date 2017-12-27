package palasjir.viewer.coordinates

import com.hackoeur.jglm.Mat4
import com.hackoeur.jglm.Matrices.*
import com.hackoeur.jglm.Vec3
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D

import java.awt.*
import java.lang.Math.toRadians

class Camera(zoom: Double) {

    private val up: Vec3 = Vec3(0f, 1f, 0f)
    private val at: Vec3 = Vec3(0f, 0f, 0f)
    private var zoomFactor = 1f
    private var eye: GLSphericalCoordinates
    
    var viewMatrix: Mat4
        private set
    
    private var projectionMatrix: ProjMat

    init {
        eye = GLSphericalCoordinates(Vector3D(0.0, 0.0, zoom))
        viewMatrix = updateView()
        projectionMatrix = ProjMat()
    }

    fun drag(start: Point?, end: Point?) {
        if (start != null && end != null) {

            val dx = start.x - end.x
            val dy = start.y - end.y

            if (dx != 0 || dy != 0) {

                val theta = eye.theta + toRadians(dx.toDouble())
                var phi = eye.phi + toRadians(dy.toDouble())

                // top lock
                if (dy < 0 && phi < 0) {
                    phi = topLock
                }

                // bottom lock
                if (phi > Math.PI) {
                    phi = bottomLock
                }

                eye = GLSphericalCoordinates(eye.r, theta, phi)
            }
        }

        viewMatrix = updateView()
    }

    private fun updateView(): Mat4 {
        return lookAt(eye.cartesian, at, up)
    }

    fun zoom(distance: Double) {
        zoomFactor += distance.toFloat() * 0.01f
        projectionMatrix = projectionMatrix.zoom(zoomFactor)
        viewMatrix = updateView()
    }

    fun setupProjection(fov: Float, w: Int, h: Int, near: Float, far: Float) {
        projectionMatrix = ProjMat(fov, w, h, near, far)
    }
    
    var position: Vec3
        get() = eye.cartesian
        set(eye) {
            this.eye = GLSphericalCoordinates(eye)
            viewMatrix = updateView()
        }

    val viewDirection: Vec3
        get() = at.subtract(eye.cartesian).unitVector

    fun mvp(model: Mat4): Mat4 {
        return Mat4(projectionMatrix.multiply(viewMatrix).multiply(model))
    }
    
    private val topLock = Math.toRadians(0.000000001)
    private val bottomLock = Math.toRadians(179.999999999)

}
