package palasjir.viewer.ui;

import com.hackoeur.jglm.Mat4;
import com.hackoeur.jglm.Matrices;
import com.hackoeur.jglm.Vec3;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.glsl.ShaderState;
import palasjir.viewer.utils.ShaderProgramHelper;
import palasjir.viewer.render.TransferFunction;
import palasjir.viewer.utils.VolumeData;
import palasjir.viewer.coordinates.Camera;
import palasjir.viewer.render.UnitCube;

import java.awt.Dimension;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES2.*;
import static com.jogamp.opengl.GL2GL3.*;
import static palasjir.viewer.utils.Utils.*;

public class VisualizationPanel extends GLCanvas implements GLEventListener {

    private static final int verticesID = 0; // input of vertexShader
    private static boolean DEBUG = false;

    private int viewPortWidth;
    private int viewPortHeight;

    private int frameBufferId;
    private int tfTextureID;
    private int volumeTextureID;
    private int gradientsTextureID;
    private int cubeVAO;
    private int renderedTextureID;

    private ShaderState st;
    private ShaderProgramHelper rayProgram;
    private ShaderProgramHelper cubeProgram;

    private Mat4 model;
    private Mat4 MVP;
    private Camera camera;
    private VolumeData volumeData;

    // shader locations
    private int cubeMvpLoc;
    private int rayMvpLoc;
    private int screenSizeLoc;
    private int transferFuncLoc;
    private int backFaceLoc;
    private int volumeLoc;
    private int gradientsLoc;
    private int VLoc;
    private int normalMatrixLoc;

    public VisualizationPanel(Dimension dimension) throws GLException {
        super(new GLCapabilities(GLProfile.getMaxProgrammableCore(true)));
        st = new ShaderState();
        setPreferredSize(dimension);
        viewPortWidth = dimension.width;
        viewPortHeight = dimension.height;
        addGLEventListener(this);

        camera = new Camera(3.0f);
        VisualisationMouseInputAdapter mAdapter = new VisualisationMouseInputAdapter(camera);
        addMouseListener(mAdapter);
        addMouseMotionListener(mAdapter);
        addMouseWheelListener(mAdapter);

        volumeData = new VolumeData();

        Mat4 rot = Matrices.rotate(90.0f, new Vec3(1.0f, 0.0f, 0.0f));

        model = new Mat4(1.0f);
        model = rot.multiply(model);
        model = model.translate(new Vec3(-0.5f, -0.5f, -0.5f));
    }

    @Override
    public void init(GLAutoDrawable drawable) throws GLException {
        GL4 gl = getGL(drawable);
        initShaderPrograms(gl);
        cubeVAO = initCube(gl);
        tfTextureID = initTransferFunction(gl);
        volumeTextureID = initVolumeTexture(gl, VolumeData.IMG_WIDTH, VolumeData.IMG_HEIGHT, VolumeData.IMG_DEPTH);
        gradientsTextureID = initGradients(gl);
        frameBufferId = initFrameBuffer(gl);
        initScreenBuffer(gl);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL4 gl = getGL(drawable);

        MVP = camera.mvp(model);

        activateFrameBuffer(gl, frameBufferId);
        drawCube(gl); // draw cube to frame buffer
        clearFrameBuffer(gl);
        raycasting(gl);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
        GL4 gl = getGL(drawable);
        viewPortWidth = w;
        viewPortHeight = h;
        gl.glViewport(0, 0, w, h);
        camera.setupProjection(45f, w, h, 1, 500);
        frameBufferId = initFrameBuffer(gl);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL4 gl = getGL(drawable);
        st.destroy(gl);
    }

    /**
     * Initializes screen buffer. Buffer for creating the screen render of rendered texture.
     *
     * @param gl GL Interface
     */
    private void initScreenBuffer(GL4 gl) {
        // The fullscreen quad's FBO
        float[] data = {
                -1.0f, -1.0f, 0.0f,
                1.0f, -1.0f, 0.0f,
                -1.0f, 1.0f, 0.0f,
                -1.0f, 1.0f, 0.0f,
                1.0f, -1.0f, 0.0f,
                1.0f, 1.0f, 0.0f
        };
        int id = genBufferId(gl);
        gl.glBindBuffer(GL_ARRAY_BUFFER, id);
        gl.glBufferData(GL_ARRAY_BUFFER, data.length * Float.SIZE, FloatBuffer.wrap(data), GL_STATIC_DRAW);
    }

    private void initShaderPrograms(GL4 gl) {
        initRayCastingProgram(gl);
        initCubeProgram(gl);
    }

    private void initRayCastingProgram(GL4 gl) {
        rayProgram = new ShaderProgramHelper(
                "raycasting.vert",
                "raycasting.frag"
        );
        rayProgram.init(gl);
        st.attachShaderProgram(gl, rayProgram.program(), true);
        screenSizeLoc = st.getUniformLocation(gl, "screenSize");
        transferFuncLoc = st.getUniformLocation(gl, "TransferFunc");
        backFaceLoc = st.getUniformLocation(gl, "exitPoints");
        volumeLoc = st.getUniformLocation(gl, "VolumeTex");
        gradientsLoc = st.getUniformLocation(gl, "gradients");
        VLoc = st.getUniformLocation(gl, "V");
        normalMatrixLoc = st.getUniformLocation(gl, "normalMatrix");
        rayMvpLoc = st.getUniformLocation(gl, "MVP");
    }

    private void initCubeProgram(GL4 gl) {
        cubeProgram = new ShaderProgramHelper(
                "cube_shader.vert",
                "cube_shader.frag"
        );
        cubeProgram.init(gl);
        st.attachShaderProgram(gl, cubeProgram.program(), true);
        cubeMvpLoc = st.getUniformLocation(gl, "MVP");
    }

    /**
     * Initializes the cube which determines the volume.
     *
     * @param gl GL Interface
     * @return VAO ID
     */
    private int initCube(GL3 gl) {

        int vaoId = genVertexArraysId(gl);
        int vboVId = genBufferId(gl);
        int vboIId = genBufferId(gl);

        UnitCube box = new UnitCube(VolumeData.IMG_WIDTH, VolumeData.IMG_HEIGHT, VolumeData.IMG_DEPTH);

        //now allocate buffers
        gl.glBindVertexArray(vaoId);
        gl.glBindBuffer(GL_ARRAY_BUFFER, vboVId);
        gl.glBufferData(GL_ARRAY_BUFFER, 24 * Float.SIZE, FloatBuffer.wrap(box.vertices), GL_STATIC_DRAW);

        gl.glEnableVertexAttribArray(verticesID);
        gl.glVertexAttribPointer(verticesID, 3, GL_FLOAT, false, 0, 0);

        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboIId);
        gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, 36 * Buffers.SIZEOF_INT, IntBuffer.wrap(UnitCube.indices), GL_STATIC_DRAW);
        gl.glBindVertexArray(0);

        // return id of vao to be used for rendering
        return vaoId;
    }

    private int initTransferFunction(GL4 gl) {
        TransferFunction tf = new TransferFunction();
        int id = genTextureId(gl);

        gl.glBindTexture(GL_TEXTURE_1D, id);
        gl.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        gl.glTexImage1D(GL_TEXTURE_1D, 0, GL_RGBA, 256, 0, GL_RGBA, GL_UNSIGNED_BYTE, tf.getBuffer());

        return id;
    }

    private int initGradients(GL4 gl) {
        int id = genTextureId(gl);

        // bind 3D texture target
        gl.glBindTexture(GL_TEXTURE_3D, id);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_REPEAT);

        // pixel transfer happens here from client to OpenGL server
        gl.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        FloatBuffer data = volumeData.gradientsBuffer();
        gl.glTexImage3D(GL_TEXTURE_3D, 0, GL_RGB, VolumeData.IMG_WIDTH, VolumeData.IMG_HEIGHT, VolumeData.IMG_DEPTH, 0, GL_RGB, GL_FLOAT, data);

        return id;
    }

    private int initVolumeTexture(GL4 gl, int w, int h, int d) {
        int id = genTextureId(gl);

        // bind 3D texture target
        gl.glBindTexture(GL_TEXTURE_3D, id);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_REPEAT);

        // pixel transfer happens here from client to OpenGL server
        gl.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        ByteBuffer buffer = volumeData.getBuffer();
        gl.glTexImage3D(GL_TEXTURE_3D, 0, GL_RED, w, h, d, 0, GL_RED, GL_UNSIGNED_BYTE, buffer);
        gl.glGenerateMipmap(GL_TEXTURE_3D);

        return id;
    }

    private void raycasting(GL4 gl) {
        st.attachShaderProgram(gl, rayProgram.program(), true);
        st.useProgram(gl, true);
        gl.glUniformMatrix4fv(rayMvpLoc, 1, false, MVP.getBuffer());
        gl.glUniform2f(
                screenSizeLoc,
                (float) viewPortWidth,
                (float) viewPortHeight
        );
        gl.glUniformMatrix4fv(
                VLoc,
                1,
                false,
                camera.getViewMatrix().getBuffer()
        );

        Mat4 normalMatrix = Matrices.invert(camera.getViewMatrix()
                .multiply(model))
                .transpose();
        gl.glUniformMatrix4fv(
                normalMatrixLoc,
                1,
                false,
                normalMatrix.getBuffer()
        );

        // tf uniform
        gl.glActiveTexture(GL_TEXTURE0);
        gl.glBindTexture(GL_TEXTURE_1D, tfTextureID);
        gl.glUniform1i(transferFuncLoc, 0);

        // backface texture
        gl.glActiveTexture(GL_TEXTURE1);
        gl.glBindTexture(GL_TEXTURE_2D, renderedTextureID);
        gl.glUniform1i(backFaceLoc, 1);

        // volume texture
        gl.glActiveTexture(GL_TEXTURE2);
        gl.glBindTexture(GL_TEXTURE_3D, volumeTextureID);
        gl.glUniform1i(volumeLoc, 2);

        // gradients texture
        gl.glActiveTexture(GL_TEXTURE3);
        gl.glBindTexture(GL_TEXTURE_3D, gradientsTextureID);
        gl.glUniform1i(gradientsLoc, 3);

        drawBox(gl, GL_BACK);
        st.useProgram(gl, false);
        st.attachShaderProgram(gl, rayProgram.program(), false); // detach raycasting program
    }

    private void drawBox(GL3 gl, int face) {
        gl.glEnable(GL_CULL_FACE);
        gl.glCullFace(face);
        gl.glBindVertexArray(cubeVAO);
        gl.glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0);
        gl.glBindVertexArray(0);
        gl.glDisable(GL_CULL_FACE);
    }

    private int initFrameBuffer(GL3 gl) {
        // The framebuffer, which regroups 0, 1, or more textures, and 0 or 1 depth buffer.
        int id = genFrameBufferId(gl);

        gl.glBindFramebuffer(GL_FRAMEBUFFER, id);

        // The texture we're going to render to
        renderedTextureID = genTextureId(gl);

        // "Bind" the newly created texture : all future texture functions will modify this texture
        gl.glBindTexture(GL_TEXTURE_2D, renderedTextureID);

        // Give an empty image to OpenGL ( the last "0" means "empty" )
        gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, viewPortWidth, viewPortHeight, 0, GL_RGB, GL_UNSIGNED_BYTE, null);

        // Poor filtering
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        // The depth buffer
        int depthRenderBufferId = genRenderBufferId(gl);
        gl.glBindRenderbuffer(GL_RENDERBUFFER, depthRenderBufferId);
        gl.glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, viewPortWidth, viewPortHeight);
        gl.glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRenderBufferId);
        gl.glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, renderedTextureID, 0);

        // Set the list of draw buffers.
        int[] DrawBuffers = {GL_COLOR_ATTACHMENT0};
        gl.glDrawBuffers(1, DrawBuffers, 0);

        if (gl.glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new GLException("Framebuffer not ok!");
        }

        gl.glEnable(GL_DEPTH_TEST);

        return id;
    }

    private GL4 getGL(GLAutoDrawable drawable) {
        if (DEBUG) {
            GL4 gl = drawable.getGL().getGL4();
            drawable.setGL(new DebugGL4(gl));
        }
        return drawable.getGL().getGL4();
    }

    private void clearFrameBuffer(GL gl) {
        gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl.glViewport(0, 0, viewPortWidth, viewPortHeight);
        gl.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
        gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    private void drawCube(GL3 gl) {
        st.attachShaderProgram(gl, cubeProgram.program(), true); // attach cube program
        st.useProgram(gl, true);
        gl.glUniformMatrix4fv(cubeMvpLoc, 1, false, MVP.getBuffer());
        drawBox(gl, GL_FRONT);
        st.useProgram(gl, false);
        st.attachShaderProgram(gl, cubeProgram.program(), false); // detach cube program
    }

    private void activateFrameBuffer(GL gl, int frameBufferId) {
        // Render to buffer
        gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl.glBindFramebuffer(GL_FRAMEBUFFER, frameBufferId);
        // Render on the whole framebuffer, complete from the lower left corner to the upper right
        gl.glViewport(0, 0, viewPortWidth, viewPortHeight);
        gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

}
