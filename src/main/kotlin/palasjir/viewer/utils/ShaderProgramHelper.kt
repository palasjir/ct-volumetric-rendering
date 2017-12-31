package palasjir.viewer.utils

import com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER
import com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER
import com.jogamp.opengl.GL4
import com.jogamp.opengl.util.glsl.ShaderCode
import com.jogamp.opengl.util.glsl.ShaderProgram

open class ShaderProgramHelper(
        private val vertexShader: String,
        private val fragmentShader: String
) {

    val program: ShaderProgram = ShaderProgram()

    fun init(gl: GL4) {
        add(gl, GL_VERTEX_SHADER, vertexShader)
        add(gl, GL_FRAGMENT_SHADER, fragmentShader)
    }

    private fun shaderName(name: String): String {
        return "shaders/" + name
    }

    private fun create(gl: GL4, type: Int, name: String): ShaderCode {
        return ShaderCode.create(
                gl,
                type,
                1,
                this.javaClass,
                arrayOf(shaderName(name)),
                false
        )
    }

    private fun add(gl: GL4, type: Int, name: String) {
        val code = create(gl, type, name)
        program.add(gl, code, System.err)
    }
}
