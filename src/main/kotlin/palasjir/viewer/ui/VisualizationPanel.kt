package palasjir.viewer.ui

import com.hackoeur.jglm.Mat4
import com.hackoeur.jglm.Matrices
import com.hackoeur.jglm.Vec3
import com.jogamp.common.nio.Buffers
import com.jogamp.opengl.*
import com.jogamp.opengl.GL2GL3.*
import com.jogamp.opengl.awt.GLCanvas
import com.jogamp.opengl.util.glsl.ShaderState
import palasjir.viewer.coordinates.Camera
import palasjir.viewer.render.TransferFunction
import palasjir.viewer.render.UnitCube
import palasjir.viewer.utils.*
import palasjir.viewer.utils.VolumeData
import java.awt.Dimension
import java.nio.FloatBuffer
import java.nio.IntBuffer

class VisualizationPanel
@Throws(GLException::class)
constructor(
        dimension: Dimension,
        caps: GLCapabilities
) : GLCanvas(caps), GLEventListener {

    private var viewPortWidth: Int = 0
    private var viewPortHeight: Int = 0

    private var frameBufferId: Int = 0
    private var tfTextureID: Int = 0
    private var volumeTextureID: Int = 0
    private var gradientsTextureID: Int = 0
    private var cubeVAO: Int = 0
    private var renderedTextureID: Int = 0

    private val st: ShaderState = ShaderState()
    private var rayProgram: ShaderProgramHelper = ShaderProgramHelper(
            "raycasting.vert",
            "raycasting.frag"
    )
    private var cubeProgram: ShaderProgramHelper = ShaderProgramHelper(
            "cube_shader.vert",
            "cube_shader.frag"
    )

    private val model: Mat4
    private var mvp: Mat4? = null
    private val camera: Camera = Camera(3.0)
    private val volumeData: VolumeData

    // shader locations
    private var cubeMvpLoc: Int = 0
    private var rayMvpLoc: Int = 0
    private var screenSizeLoc: Int = 0
    private var transferFuncLoc: Int = 0
    private var backFaceLoc: Int = 0
    private var volumeLoc: Int = 0
    private var gradientsLoc: Int = 0
    private var VLoc: Int = 0
    private var normalMatrixLoc: Int = 0

    init {
        preferredSize = dimension
        viewPortWidth = dimension.width
        viewPortHeight = dimension.height
        addGLEventListener(this)
        
        val mAdapter = VisualisationMouseInputAdapter(camera)
        addMouseListener(mAdapter)
        addMouseMotionListener(mAdapter)
        addMouseWheelListener(mAdapter)

        volumeData = VolumeData()

        val rot = Matrices.rotate(90.0f, Vec3(1.0f, 0.0f, 0.0f))

        model = rot.multiply(Mat4.MAT4_IDENTITY).translate(Vec3(-0.5f, -0.5f, -0.5f))
    }

    @Throws(GLException::class)
    override fun init(drawable: GLAutoDrawable) {
        val gl = getGL(drawable)
        initShaderPrograms(gl)
        cubeVAO = initCube(gl)
        tfTextureID = initTransferFunction(gl)
        volumeTextureID = initVolumeTexture(
                gl,
                VolumeData.Companion.IMG_WIDTH,
                VolumeData.Companion.IMG_HEIGHT,
                VolumeData.Companion.IMG_DEPTH
        )
        gradientsTextureID = initGradients(gl)
        frameBufferId = initFrameBuffer(gl)
        initScreenBuffer(gl)
    }

    override fun display(drawable: GLAutoDrawable) {
        val gl = getGL(drawable)

        mvp = camera.mvp(model)

        activateFrameBuffer(gl, frameBufferId)
        drawCube(gl) // draw cube to frame buffer
        clearFrameBuffer(gl)
        raycasting(gl)
    }

    override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, w: Int, h: Int) {
        val gl = getGL(drawable)
        viewPortWidth = w
        viewPortHeight = h
        gl.glViewport(0, 0, w, h)
        camera.setupProjection(45f, w, h, 1f, 500f)
        frameBufferId = initFrameBuffer(gl)
    }

    override fun dispose(drawable: GLAutoDrawable) {
        val gl = getGL(drawable)
        st.destroy(gl)
    }

    /**
     * Initializes screen buffer. Buffer for creating the screen render of rendered texture.
     *
     * @param gl GL Interface
     */
    private fun initScreenBuffer(gl: GL4) {
        // The fullscreen quad's FBO
        val data = floatArrayOf(-1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, -1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f, 1.0f, 0.0f)
        val id = genBufferId(gl)
        gl.glBindBuffer(GL_ARRAY_BUFFER, id)
        gl.glBufferData(GL_ARRAY_BUFFER, (data.size * java.lang.Float.SIZE).toLong(), FloatBuffer.wrap(data), GL_STATIC_DRAW)
    }

    private fun initShaderPrograms(gl: GL4) {
        initRayCastingProgram(gl)
        initCubeProgram(gl)
    }

    private fun initRayCastingProgram(gl: GL4) {
        rayProgram.init(gl)
        st.attachShaderProgram(gl, rayProgram.program, true)
        screenSizeLoc = st.getUniformLocation(gl, "screenSize")
        transferFuncLoc = st.getUniformLocation(gl, "TransferFunc")
        backFaceLoc = st.getUniformLocation(gl, "exitPoints")
        volumeLoc = st.getUniformLocation(gl, "VolumeTex")
        gradientsLoc = st.getUniformLocation(gl, "gradients")
        VLoc = st.getUniformLocation(gl, "V")
        normalMatrixLoc = st.getUniformLocation(gl, "normalMatrix")
        rayMvpLoc = st.getUniformLocation(gl, "MVP")
    }

    private fun initCubeProgram(gl: GL4) {
        cubeProgram.init(gl)
        st.attachShaderProgram(gl, cubeProgram.program, true)
        cubeMvpLoc = st.getUniformLocation(gl, "MVP")
    }

    /**
     * Initializes the cube which determines the volume.
     *
     * @param gl GL Interface
     * @return VAO ID
     */
    private fun initCube(gl: GL3): Int {

        val vaoId = genVertexArraysId(gl)
        val vboVId = genBufferId(gl)
        val vboIId = genBufferId(gl)

        val box = UnitCube(
                VolumeData.Companion.IMG_WIDTH.toFloat(),
                VolumeData.Companion.IMG_HEIGHT.toFloat(),
                VolumeData.Companion.IMG_DEPTH.toFloat()
        )

        //now allocate buffers
        gl.glBindVertexArray(vaoId)
        gl.glBindBuffer(GL_ARRAY_BUFFER, vboVId)
        gl.glBufferData(GL_ARRAY_BUFFER, (24 * java.lang.Float.SIZE).toLong(), FloatBuffer.wrap(box.vertices), GL_STATIC_DRAW)

        gl.glEnableVertexAttribArray(verticesID)
        gl.glVertexAttribPointer(verticesID, 3, GL_FLOAT, false, 0, 0)

        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboIId)
        gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, (36 * Buffers.SIZEOF_INT).toLong(), IntBuffer.wrap(UnitCube.Companion.indices), GL_STATIC_DRAW)
        gl.glBindVertexArray(0)

        // return id of vao to be used for rendering
        return vaoId
    }

    private fun initTransferFunction(gl: GL4): Int {
        val tf = TransferFunction()
        val id = genTextureId(gl)

        gl.glBindTexture(GL_TEXTURE_1D, id)
        gl.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        gl.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        gl.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        gl.glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        gl.glTexImage1D(GL_TEXTURE_1D, 0, GL_RGBA, 256, 0, GL_RGBA, GL_UNSIGNED_BYTE, tf.buffer)

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

    private fun initVolumeTexture(gl: GL4, w: Int, h: Int, d: Int): Int {
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
        gl.glTexImage3D(GL_TEXTURE_3D, 0, GL_RED, w, h, d, 0, GL_RED, GL_UNSIGNED_BYTE, buffer)
        gl.glGenerateMipmap(GL_TEXTURE_3D)

        return id
    }

    private fun raycasting(gl: GL4) {
        st.attachShaderProgram(gl, rayProgram.program, true)
        st.useProgram(gl, true)
        gl.glUniformMatrix4fv(rayMvpLoc, 1, false, mvp!!.buffer)
        gl.glUniform2f(
                screenSizeLoc,
                viewPortWidth.toFloat(),
                viewPortHeight.toFloat()
        )
        gl.glUniformMatrix4fv(
                VLoc,
                1,
                false,
                camera.viewMatrix!!.buffer
        )

        val normalMatrix = Matrices.invert(
                camera.viewMatrix!!.multiply(model))
                .transpose()
        gl.glUniformMatrix4fv(
                normalMatrixLoc,
                1,
                false,
                normalMatrix.buffer
        )

        // tf uniform
        gl.glActiveTexture(GL_TEXTURE0)
        gl.glBindTexture(GL_TEXTURE_1D, tfTextureID)
        gl.glUniform1i(transferFuncLoc, 0)

        // backface texture
        gl.glActiveTexture(GL_TEXTURE1)
        gl.glBindTexture(GL_TEXTURE_2D, renderedTextureID)
        gl.glUniform1i(backFaceLoc, 1)

        // volume texture
        gl.glActiveTexture(GL_TEXTURE2)
        gl.glBindTexture(GL_TEXTURE_3D, volumeTextureID)
        gl.glUniform1i(volumeLoc, 2)

        // gradients texture
        gl.glActiveTexture(GL_TEXTURE3)
        gl.glBindTexture(GL_TEXTURE_3D, gradientsTextureID)
        gl.glUniform1i(gradientsLoc, 3)

        drawBox(gl, GL_BACK)
        st.useProgram(gl, false)
        st.attachShaderProgram(gl, rayProgram.program, false) // detach raycasting program
    }

    private fun drawBox(gl: GL3, face: Int) {
        gl.glEnable(GL_CULL_FACE)
        gl.glCullFace(face)
        gl.glBindVertexArray(cubeVAO)
        gl.glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0)
        gl.glBindVertexArray(0)
        gl.glDisable(GL_CULL_FACE)
    }

    private fun initFrameBuffer(gl: GL3): Int {
        // The framebuffer, which regroups 0, 1, or more textures, and 0 or 1 depth buffer.
        val id = genFrameBufferId(gl)

        gl.glBindFramebuffer(GL_FRAMEBUFFER, id)

        // The texture we're going to render to
        renderedTextureID = genTextureId(gl)

        // "Bind" the newly created texture : all future texture functions will modify this texture
        gl.glBindTexture(GL_TEXTURE_2D, renderedTextureID)

        // Give an empty image to OpenGL ( the last "0" means "empty" )
        gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, viewPortWidth, viewPortHeight, 0, GL_RGB, GL_UNSIGNED_BYTE, null)

        // Poor filtering
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

        // The depth buffer
        val depthRenderBufferId = genRenderBufferId(gl)
        gl.glBindRenderbuffer(GL_RENDERBUFFER, depthRenderBufferId)
        gl.glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, viewPortWidth, viewPortHeight)
        gl.glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRenderBufferId)
        gl.glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, renderedTextureID, 0)

        // Set the list of draw buffers.
        val drawBuffers = intArrayOf(GL_COLOR_ATTACHMENT0)
        gl.glDrawBuffers(1, drawBuffers, 0)

        if (gl.glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw GLException("Framebuffer not ok!")
        }

        gl.glEnable(GL_DEPTH_TEST)

        return id
    }

    private fun getGL(drawable: GLAutoDrawable): GL4 {
        if (DEBUG) {
            val gl = drawable.gl.gL4
            drawable.gl = DebugGL4(gl)
        }
        return drawable.gl.gL4
    }

    private fun clearFrameBuffer(gl: GL) {
        gl.glBindFramebuffer(GL_FRAMEBUFFER, 0)
        gl.glViewport(0, 0, viewPortWidth, viewPortHeight)
        gl.glClearColor(0.2f, 0.2f, 0.2f, 1.0f)
        gl.glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
    }

    private fun drawCube(gl: GL3) {
        st.attachShaderProgram(gl, cubeProgram.program, true) // attach cube program
        st.useProgram(gl, true)
        gl.glUniformMatrix4fv(cubeMvpLoc, 1, false, mvp!!.buffer)
        drawBox(gl, GL_FRONT)
        st.useProgram(gl, false)
        st.attachShaderProgram(gl, cubeProgram.program, false) // detach cube program
    }

    private fun activateFrameBuffer(gl: GL, frameBufferId: Int) {
        // Render to buffer
        gl.glBindFramebuffer(GL_FRAMEBUFFER, 0)
        gl.glBindFramebuffer(GL_FRAMEBUFFER, frameBufferId)
        // Render on the whole framebuffer, complete from the lower left corner to the upper right
        gl.glViewport(0, 0, viewPortWidth, viewPortHeight)
        gl.glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
    }

    companion object {
        private val verticesID = 0 // input of vertexShader
        private val DEBUG = false
    }

}
