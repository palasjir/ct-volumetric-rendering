package palasjir.viewer.render

class UnitCube(
        x: Float,
        y: Float,
        z: Float
) {

    val vertices: FloatArray

    init {
        val max = Math.max(x, Math.max(y, z))
        val mx = x / max
        val my = y / max
        val mz = z / max
        vertices = floatArrayOf(
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, mz,
                0.0f, my, 0.0f,
                0.0f, my, mz,
                mx, 0.0f, 0.0f,
                mx, 0.0f, mz,
                mx, my, 0.0f,
                mx, my, mz
        )
    }

    companion object {
        val indices = intArrayOf(
                1, 5, 7,
                7, 3, 1,
                0, 2, 6,
                6, 4, 0,
                0, 1, 3,
                3, 2, 0,
                7, 5, 4,
                4, 6, 7,
                2, 3, 7,
                7, 6, 2,
                1, 0, 4,
                4, 5, 1
        )
    }
}
