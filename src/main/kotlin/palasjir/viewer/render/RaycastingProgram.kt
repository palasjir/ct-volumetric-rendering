package palasjir.viewer.render

import com.hackoeur.jglm.Mat4
import com.jogamp.opengl.GL2GL3.*
import com.jogamp.opengl.GL4
import com.jogamp.opengl.util.glsl.ShaderState
import palasjir.viewer.coordinates.Camera
import palasjir.viewer.utils.ShaderProgramHelper
import palasjir.viewer.utils.VolumeData
import palasjir.viewer.utils.genTextureId

private val VS = "raycasting.vert"
private val FS = "raycasting.frag"

class RaycastingProgram(val volumeData: VolumeData): ShaderProgramHelper(VS, FS) {

    private var screenSizeLoc: Int = -1
    private var transferFuncLoc: Int = -1
    private var backFaceLoc: Int = -1
    private var volumeLoc: Int = -1
    private var gradientsLoc: Int = -1
    private var viewMatLoc: Int = -1
    private var normalMatLoc: Int = -1
    private var rayMvpLoc: Int = -1

    private var tfTextureId: Int = -1
    private var volumeTextureId: Int = -1
    private var gradientsTextureId: Int = -1

    fun init(gl: GL4, st: ShaderState) {
        super.init(gl)
        st.attachShaderProgram(gl, this.program, true)
        screenSizeLoc = st.getUniformLocation(gl, "screenSize")
        transferFuncLoc = st.getUniformLocation(gl, "transferFn")
        backFaceLoc = st.getUniformLocation(gl, "exitPoints")
        volumeLoc = st.getUniformLocation(gl, "volume")
        gradientsLoc = st.getUniformLocation(gl, "gradients")
        viewMatLoc = st.getUniformLocation(gl, "viewMat")
        normalMatLoc = st.getUniformLocation(gl, "normalMat")
        rayMvpLoc = st.getUniformLocation(gl, "mvp")

        tfTextureId = initTransferFunction(gl)
        volumeTextureId = initVolumeTexture(gl)
        gradientsTextureId = initGradients(gl)

    }

    fun display(gl: GL4, st: ShaderState, mvp: Mat4, camera: Camera, model: Mat4, boxRenderer: BoxRenderer, renderedTextureId: Int) {
        st.attachShaderProgram(gl, program, true)
        st.useProgram(gl, true)

        gl.glUniformMatrix4fv(rayMvpLoc, 1, false, mvp.buffer)
        gl.glUniform2f(
                screenSizeLoc,
                camera.viewPortWidth.toFloat(),
                camera.viewPortHeight.toFloat()
        )
        gl.glUniformMatrix4fv(
                viewMatLoc,
                1,
                false,
                camera.viewMatrix.buffer
        )

        val normalMatrix = camera.normalMatrix(model)
        gl.glUniformMatrix4fv(
                normalMatLoc,
                1,
                false,
                normalMatrix.buffer
        )

        // tf uniform
        gl.glActiveTexture(GL_TEXTURE0)
        gl.glBindTexture(GL_TEXTURE_1D, tfTextureId)
        gl.glUniform1i(transferFuncLoc, 0)

        // backface texture
        gl.glActiveTexture(GL_TEXTURE1)
        gl.glBindTexture(GL_TEXTURE_2D, renderedTextureId)
        gl.glUniform1i(backFaceLoc, 1)

        // volume texture
        gl.glActiveTexture(GL_TEXTURE2)
        gl.glBindTexture(GL_TEXTURE_3D, volumeTextureId)
        gl.glUniform1i(volumeLoc, 2)

        // gradients texture
        gl.glActiveTexture(GL_TEXTURE3)
        gl.glBindTexture(GL_TEXTURE_3D, gradientsTextureId)
        gl.glUniform1i(gradientsLoc, 3)

        boxRenderer.draw(gl, GL_BACK)

        st.useProgram(gl, false)
        st.attachShaderProgram(gl, program, false) // detach exec program
    }

    private fun initTransferFunction(gl: GL4): Int {
        val tf = TransferFunction()
        val id = genTextureId(gl)

        gl.glBindTexture(GL_TEXTURE_1D, id)
        gl.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        gl.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        gl.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        gl.glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        gl.glTexImage1D(GL_TEXTURE_1D, 0, GL_RGBA, TransferFunction.Companion.SIZE, 0, GL_RGBA, GL_UNSIGNED_BYTE, tf.buffer)

        return id
    }

    private fun initGradients(gl: GL4): Int {
        val id = genTextureId(gl)

        // bind 3D texture target
        gl.glBindTexture(GL_TEXTURE_3D, id)
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_REPEAT)
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_REPEAT)

        // pixel transfer happens here from client to OpenGL server
        gl.glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        val data = volumeData.gradientsBuffer()
        gl.glTexImage3D(GL_TEXTURE_3D, 0, GL_RGB, VolumeData.Companion.IMG_WIDTH, VolumeData.Companion.IMG_HEIGHT, VolumeData.Companion.IMG_DEPTH, 0, GL_RGB, GL_FLOAT, data)

        return id
    }

    private fun initVolumeTexture(gl: GL4): Int {
        val id = genTextureId(gl)

        // bind 3D texture target
        gl.glBindTexture(GL_TEXTURE_3D, id)
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_REPEAT)
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_REPEAT)

        // pixel transfer happens here from client to OpenGL server
        gl.glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        val buffer = volumeData.buffer
        gl.glTexImage3D(
                GL_TEXTURE_3D,
                0,
                GL_RED,
                VolumeData.Companion.IMG_WIDTH,
                VolumeData.Companion.IMG_HEIGHT,
                VolumeData.Companion.IMG_DEPTH,
                0,
                GL_RED,
                GL_UNSIGNED_BYTE,
                buffer
        )
        gl.glGenerateMipmap(GL_TEXTURE_3D)
        return id
    }


}