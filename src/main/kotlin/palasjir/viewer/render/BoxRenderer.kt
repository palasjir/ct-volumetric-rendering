package palasjir.viewer.render

import com.jogamp.common.nio.Buffers
import com.jogamp.opengl.GL2GL3
import com.jogamp.opengl.GL3.*
import com.jogamp.opengl.GL3
import palasjir.viewer.utils.VolumeData
import palasjir.viewer.utils.genBufferId
import palasjir.viewer.utils.genVertexArraysId
import java.nio.FloatBuffer
import java.nio.IntBuffer


class BoxRenderer {

    private var verticesId: Int = -1
    private var vaoId: Int = -1

    /**
     * Initializes the cube which determines the volume.
     *
     * @param gl GL Interface
     * @return VAO ID
     */
    fun init(gl: GL3, verticesId: Int)  {

        this.verticesId = verticesId

        vaoId = genVertexArraysId(gl)
        val vboVId = genBufferId(gl)
        val vboIId = genBufferId(gl)

        val box = UnitCube(
                VolumeData.Companion.IMG_WIDTH.toFloat(),
                VolumeData.Companion.IMG_HEIGHT.toFloat(),
                VolumeData.Companion.IMG_DEPTH.toFloat()
        )

        //now allocate buffers
        gl.glBindVertexArray(vaoId)
        gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, vboVId)
        gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, (24 * java.lang.Float.SIZE).toLong(), FloatBuffer.wrap(box.vertices), GL2GL3.GL_STATIC_DRAW)

        gl.glEnableVertexAttribArray(this.verticesId)
        gl.glVertexAttribPointer(this.verticesId, 3, GL2GL3.GL_FLOAT, false, 0, 0)

        gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, vboIId)
        gl.glBufferData(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, (36 * Buffers.SIZEOF_INT).toLong(), IntBuffer.wrap(UnitCube.Companion.indices), GL2GL3.GL_STATIC_DRAW)
        gl.glBindVertexArray(0)
    }

    fun draw(gl: GL3, face: Int) {
        gl.glEnable(GL_CULL_FACE)
        gl.glCullFace(face)
        gl.glBindVertexArray(vaoId)
        gl.glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0)
        gl.glBindVertexArray(0)
        gl.glDisable(GL_CULL_FACE)
    }
}