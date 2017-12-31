package palasjir.viewer.render

import com.hackoeur.jglm.Mat4
import com.jogamp.opengl.GL4
import com.jogamp.opengl.GL4.*
import com.jogamp.opengl.util.glsl.ShaderState
import palasjir.viewer.utils.ShaderProgramHelper

private val VS = "cube_shader.vert"
private val FS = "cube_shader.frag"

class CubeProgram : ShaderProgramHelper(VS, FS) {

    private var mvpLoc: Int = -1

    fun init(gl: GL4, st: ShaderState) {
        super.init(gl)
        st.attachShaderProgram(gl, program, true)
        mvpLoc = st.getUniformLocation(gl, "mvp")
    }

    fun display(gl: GL4, st: ShaderState, mvp: Mat4, boxRenderer: BoxRenderer) {
        st.attachShaderProgram(gl, program, true) // attach cube program
        st.useProgram(gl, true)
        gl.glUniformMatrix4fv(mvpLoc, 1, false, mvp.buffer)
        boxRenderer.draw(gl, GL_FRONT)
        st.useProgram(gl, false)
        st.attachShaderProgram(gl, program, false) // detach cube program
    }

}