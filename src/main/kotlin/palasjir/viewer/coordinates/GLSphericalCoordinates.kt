package palasjir.viewer.coordinates

import com.hackoeur.jglm.Vec3
import org.apache.commons.math3.geometry.euclidean.threed.SphericalCoordinates
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D

class GLSphericalCoordinates : SphericalCoordinates {

    constructor(v: Vec3) : super(Vector3D(v.z.toDouble(), v.x.toDouble(), v.y.toDouble()))

    constructor(radius: Double, theta: Double, phi: Double) : super(radius, theta, phi)

    val cartesian: Vec3
        get() {
            val v = super.getCartesian()
            return Vec3(v.y.toFloat(), v.z.toFloat(), v.x.toFloat())
        }
}
