package palasjir.viewer.coordinates

import com.hackoeur.jglm.Mat4
import com.hackoeur.jglm.Matrices

class ProjMat {

    private val aspectRatio: Float
    private val fov: Float
    private val near: Float
    private val far: Float
    private val mat: Mat4 
    
    constructor(fov: Float, aspectRatio: Float, near: Float, far: Float) {
        this.aspectRatio = aspectRatio
        this.fov = fov
        this.near = near
        this.far = far
        this.mat = Matrices.perspective(fov, aspectRatio, near, far)
    }

    constructor(fov: Float, w: Int, h: Int, near: Float, far: Float): this(fov, aspect(w, h), near, far)
    
    constructor(): this(45f, 1f, 1f, 500f)

    operator fun times(mat: Mat4): Mat4 = this.multiply(mat)

    fun multiply(mat: Mat4): Mat4 = this.mat.multiply(mat)
    
    fun zoom(zoomFactor: Float): ProjMat = ProjMat(fov * zoomFactor, aspectRatio, near, far)

}

private fun aspect(w: Int, h: Int): Float {
    return w.toFloat() / h.toFloat()
}
