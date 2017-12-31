package palasjir.viewer.ui

import com.hackoeur.jglm.Mat4
import com.hackoeur.jglm.Matrices
import com.hackoeur.jglm.Vec3

import com.jogamp.opengl.*
import com.jogamp.opengl.awt.GLCanvas
import com.jogamp.opengl.util.glsl.ShaderState
import palasjir.kglm.*
import palasjir.viewer.coordinates.Camera
import palasjir.viewer.render.*
import palasjir.viewer.utils.VolumeData
import java.awt.Dimension

class VisualizationPanel
constructor(
        dimension: Dimension,
        caps: GLCapabilities
) : GLCanvas(caps), GLEventListener {

    private val st: ShaderState = ShaderState()
    private val camera: Camera = Camera(INITIAL_ZOOM_LEVEL)
    private val rayProgram = RaycastingProgram(VolumeData())
    private val cubeProgram = CubeProgram()
    private val boxRenderer = BoxRenderer()
    private val frameBuffer = FrameBuffer(camera)
    private val screenBuffer = ScreenBuffer()
    private val model: Mat4

    init {
        preferredSize = dimension
        camera.viewPortWidth = dimension.width
        camera.viewPortHeight = dimension.height
        addGLEventListener(this)
        
        val mAdapter = MouseAdapter(camera)
        addMouseListener(mAdapter)
        addMouseMotionListener(mAdapter)
        addMouseWheelListener(mAdapter)

        val rotX = Matrices.rotate(90f, X_AXIS)

        model = rotX * Mat4.MAT4_IDENTITY.translate(Vec3(-0.5f, -0.5f, -0.5f))
    }

    override fun init(drawable: GLAutoDrawable) {
        val gl = getGL(drawable)
        initShaderPrograms(gl)

        camera.position = Vec3(1f, 0f, INITIAL_ZOOM_LEVEL)

        boxRenderer.init(gl, verticesID)
        screenBuffer.init(gl)
    }

    override fun display(drawable: GLAutoDrawable) {
        val gl = getGL(drawable)

        val mvp = camera.mvp(model)

        frameBuffer.activate(gl)
        cubeProgram.display(gl, st, mvp, boxRenderer) // draw cube to frame buffer

        frameBuffer.clear(gl)
        rayProgram.display(gl, st, mvp, camera, model, boxRenderer, frameBuffer.renderedTextureId)
    }

    override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, w: Int, h: Int) {
        val gl = getGL(drawable)
        gl.glViewport(0, 0, w, h)
        camera.setupDefaultProjection(w, h)
        frameBuffer.init(gl)
    }

    override fun dispose(drawable: GLAutoDrawable) {
        val gl = getGL(drawable)
        st.destroy(gl)
    }

    private fun initShaderPrograms(gl: GL4) {
        rayProgram.init(gl, st)
        cubeProgram.init(gl, st)
    }

    private fun getGL(drawable: GLAutoDrawable): GL4 {
        if (DEBUG) {
            val gl = drawable.gl.gL4
            drawable.gl = DebugGL4(gl)
        }
        return drawable.gl.gL4
    }

    companion object {
        private val verticesID = 0 // input of vertexShader
        private val DEBUG = false
        private val INITIAL_ZOOM_LEVEL = -2f
        private val X_AXIS = Vec3(1f, 0f, 0f)
    }

}
