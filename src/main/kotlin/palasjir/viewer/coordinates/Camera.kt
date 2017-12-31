package palasjir.viewer.coordinates

import com.hackoeur.jglm.Mat4
import com.hackoeur.jglm.Matrices
import com.hackoeur.jglm.Matrices.*
import com.hackoeur.jglm.Vec3
import palasjir.kglm.*

import java.awt.*
import java.lang.Math.toRadians

class Camera(zoom: Float) {

    private val up: Vec3 = Vec3(0f, 1f, 0f)
    private val at: Vec3 = Vec3(0f, 0f, 0f)
    private var zoomFactor = 1f
    private var eye: GLSphericalCoordinates
    
    var viewMatrix: Mat4
        private set
    var viewPortWidth: Int = -1
    var viewPortHeight: Int = -1
    
    private var projectionMatrix: ProjMat

    init {
        eye = GLSphericalCoordinates(Vec3(0f, 0f, zoom))
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
        viewPortWidth = w
        viewPortHeight = h
        projectionMatrix = ProjMat(fov, w, h, near, far)
    }

    fun setupDefaultProjection(w: Int, h: Int) = this.setupProjection(45f, w, h, 1f, 500f)
    
    var position: Vec3
        get() = eye.cartesian
        set(eye) {
            this.eye = GLSphericalCoordinates(eye)
            viewMatrix = updateView()
        }

    val viewDirection: Vec3
        get() = at.subtract(eye.cartesian).unitVector

    fun mvp(model: Mat4): Mat4 = projectionMatrix * viewMatrix * model
    fun normalMatrix(model: Mat4): Mat4 = Matrices.invert(viewMatrix * model).transpose()
    
    private val topLock = Math.toRadians(0.000000001)
    private val bottomLock = Math.toRadians(179.999999999)

}
