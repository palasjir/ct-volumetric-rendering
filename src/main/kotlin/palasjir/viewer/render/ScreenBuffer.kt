package palasjir.viewer.render

import com.jogamp.opengl.GL3
import com.jogamp.opengl.GL3.*
import palasjir.viewer.utils.genBufferId
import java.nio.FloatBuffer

class ScreenBuffer {

    var id : Int = -1
        private set

    /**
     * Initializes screen buffer. Buffer for creating the screen render of rendered texture.
     *
     * @param gl GL Interface
     */
    fun init(gl: GL3) {
        // The fullscreen quad's FBO
        val data = floatArrayOf(-1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, -1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f, 1.0f, 0.0f)
        id = genBufferId(gl)
        gl.glBindBuffer(GL_ARRAY_BUFFER, id)
        gl.glBufferData(GL_ARRAY_BUFFER, (data.size * java.lang.Float.SIZE).toLong(), FloatBuffer.wrap(data), GL_STATIC_DRAW)
    }


}