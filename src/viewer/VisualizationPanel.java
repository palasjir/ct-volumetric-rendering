/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package viewer;

import com.hackoeur.jglm.Mat4;
import com.hackoeur.jglm.Matrices;
import com.hackoeur.jglm.Vec3;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

import java.awt.Dimension;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES2.*;
import static com.jogamp.opengl.GL2GL3.GL_TEXTURE_1D;

/**
 * @author palasjiri
 */
public class VisualizationPanel extends GLCanvas implements GLEventListener {

    private static final int IMG_WIDTH = 256;
    private static final int IMG_HEIGHT = 256;
    private static final int IMG_DEPTH = 113 * 2;

    private ShaderState st;
    private int frameBufferID;
    private int tfTextureID;
    private int volumeTextureID;
    private int gradientsTextureID;
    private int cubeVAO;
    private IntBuffer renderedTexture = IntBuffer.allocate(1);
    private int viewPortWidth;
    private int viewPortHeight;

    private ShaderProgram rcProgram;
    private ShaderProgram cubeProgram;

    private static Mat4 model;

    static {
        model = new Mat4(1.0f);
        model = Matrices.rotate(model, 90.0f, new Vec3(1.0f, 0.0f, 0.0f));
        model = model.translate(new Vec3(-0.5f, -0.5f, -0.5f));
    }

    private Mat4 MVP;

    // shader locations
    private final int verticesID = 0; // input of vertexShader
    private int matrixID;
    private int rcMvpLoc;

    private int screenSizeLoc;
    private int transferFuncLoc;
    private int backFaceLoc;
    private int volumeLoc;
    private int gradientsLoc;
    private int VLoc;
    private int normalMatrixLoc;

    private Camera camera;

    // The fullscreen quad's FBO
    private static final float[] g_quad_vertex_buffer_data = {
            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            -1.0f, 1.0f, 0.0f,
            -1.0f, 1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            1.0f, 1.0f, 0.0f,};

    private IntBuffer quad_vertexbuffer = IntBuffer.allocate(1);
    private VolumeData volumeData;

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
    }

    @Override
    public void init(GLAutoDrawable drawable) throws GLException {

        GL4 gl = drawable.getGL().getGL4();
//        drawable.setGL(new DebugGL4(gl));
//        gl = drawable.getGL().getGL4();

        initShaders(gl);
        cubeVAO = initCube(gl);

        tfTextureID = initTransferFunction(gl);
        volumeTextureID = initVolumeTexture(gl, IMG_WIDTH, IMG_HEIGHT, IMG_DEPTH);
        gradientsTextureID = initGradients(gl);
        frameBufferID = initFrameBuffer(gl);

        // init screen buffer
        // buffer for creating the screen display of redered texture
        gl.glGenBuffers(1, quad_vertexbuffer);
        gl.glBindBuffer(GL_ARRAY_BUFFER, quad_vertexbuffer.get(0));
        gl.glBufferData(GL_ARRAY_BUFFER, g_quad_vertex_buffer_data.length * Float.SIZE, FloatBuffer.wrap(g_quad_vertex_buffer_data), GL_STATIC_DRAW);

    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();
        st.destroy(gl);
    }

    private void initShaders(GL4 gl) {

        rcProgram = new ShaderProgram();
        ShaderCode vp1 = ShaderCode.create(gl, GL_VERTEX_SHADER, 1, this.getClass(), new String[]{"shaders/raycasting.vert"}, false);
        ShaderCode fp1 = ShaderCode.create(gl, GL_FRAGMENT_SHADER, 1, this.getClass(), new String[]{"shaders/raycasting.frag"}, false);
        rcProgram.add(gl, vp1, System.err);
        rcProgram.add(gl, fp1, System.err);
        st.attachShaderProgram(gl, rcProgram, true);


        screenSizeLoc = st.getUniformLocation(gl, "screenSize");
        transferFuncLoc = st.getUniformLocation(gl, "TransferFunc");
        backFaceLoc = st.getUniformLocation(gl, "exitPoints");
        volumeLoc = st.getUniformLocation(gl, "VolumeTex");
        gradientsLoc = st.getUniformLocation(gl, "gradients");
        VLoc = st.getUniformLocation(gl, "V");
        normalMatrixLoc = st.getUniformLocation(gl, "normalMatrix");
        rcMvpLoc = st.getUniformLocation(gl, "MVP");


        // init cube program
        cubeProgram = new ShaderProgram();
        ShaderCode vpc2 = ShaderCode.create(gl, GL_VERTEX_SHADER, 1, this.getClass(), new String[]{"shaders/cube_shader.vert"}, false);
        ShaderCode fpc2 = ShaderCode.create(gl, GL_FRAGMENT_SHADER, 1, this.getClass(), new String[]{"shaders/cube_shader.frag"}, false);
        cubeProgram.add(gl, vpc2, System.err);
        cubeProgram.add(gl, fpc2, System.err);
        st.attachShaderProgram(gl, cubeProgram, true);
        matrixID = st.getUniformLocation(gl, "MVP");
    }

    // inits the cube which determines the volume.
    private int initCube(GL3 gl) {

        // allocate id's
        IntBuffer vaoID = IntBuffer.allocate(1);
        IntBuffer vboVerticesID = IntBuffer.allocate(1);
        IntBuffer vboIndicesID = IntBuffer.allocate(1);

        // generate id's
        gl.glGenVertexArrays(1, vaoID);
        gl.glGenBuffers(1, vboVerticesID);
        gl.glGenBuffers(1, vboIndicesID);

        UnitCube box = new UnitCube(IMG_WIDTH, IMG_HEIGHT, IMG_DEPTH);

        //now allocate buffers
        gl.glBindVertexArray(vaoID.get(0));
        gl.glBindBuffer(GL_ARRAY_BUFFER, vboVerticesID.get(0));
        gl.glBufferData(GL_ARRAY_BUFFER, 24 * Float.SIZE, FloatBuffer.wrap(box.vertices), GL_STATIC_DRAW);

        gl.glEnableVertexAttribArray(verticesID);
        gl.glVertexAttribPointer(verticesID, 3, GL_FLOAT, false, 0, 0);

        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboIndicesID.get(0));
        gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, 36 * Buffers.SIZEOF_INT, IntBuffer.wrap(UnitCube.indices), GL_STATIC_DRAW);
        gl.glBindVertexArray(0);

        // return id of vao to be used for rendering
        return vaoID.get(0);
    }

    private int initTransferFunction(GL4 gl) {
        TransferFunction tf = new TransferFunction();

        IntBuffer tfIDbuff = IntBuffer.allocate(1);
        gl.glGenTextures(1, tfIDbuff);
        gl.glBindTexture(GL_TEXTURE_1D, tfIDbuff.get(0));
        gl.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        gl.glTexImage1D(GL_TEXTURE_1D, 0, GL_RGBA, 256, 0, GL_RGBA, GL_UNSIGNED_BYTE, tf.getBuffer());

        return tfIDbuff.get(0);
    }

    private int initGradients(GL4 gl) {

        FloatBuffer data = volumeData.gradientsBuffer();

        IntBuffer volumeTextureIDs = IntBuffer.allocate(1);
        gl.glGenTextures(1, volumeTextureIDs);
        // bind 3D texture target
        gl.glBindTexture(GL_TEXTURE_3D, volumeTextureIDs.get(0));
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_REPEAT);
        // pixel transfer happens here from client to OpenGL server
        gl.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        gl.glTexImage3D(GL_TEXTURE_3D, 0, GL_RGB, IMG_WIDTH, IMG_HEIGHT, IMG_DEPTH, 0, GL_RGB, GL_FLOAT, data);

        return volumeTextureIDs.get(0);
    }

    private int initVolumeTexture(GL4 gl, int w, int h, int d) {
        IntBuffer volumeTextureIDs = IntBuffer.allocate(1);
        ByteBuffer buffer = volumeData.getBuffer();
        gl.glGenTextures(1, volumeTextureIDs);
        // bind 3D texture target
        gl.glBindTexture(GL_TEXTURE_3D, volumeTextureIDs.get(0));
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_REPEAT);
        // pixel transfer happens here from client to OpenGL server
        gl.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);


        gl.glTexImage3D(GL_TEXTURE_3D, 0, GL_RED, w, h, d, 0, GL_RED, GL_UNSIGNED_BYTE, buffer);

        // mipmapping ???
        gl.glGenerateMipmap(GL_TEXTURE_3D);
        // destroy
        buffer = null;

        return volumeTextureIDs.get(0);
    }


    @Override
    public void display(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();
//        drawable.setGL(new DebugGL4(gl));
//        gl = drawable.getGL().getGL4();

        // Render to buffer
        gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl.glBindFramebuffer(GL_FRAMEBUFFER, frameBufferID);
        gl.glViewport(0, 0, viewPortWidth, viewPortHeight); // Render on the whole framebuffer, complete from the lower left corner to the upper right
        gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        MVP = camera.mvp(model);

        // draw cube to frame buffer
        st.attachShaderProgram(gl, cubeProgram, true); // attach cube program
        st.useProgram(gl, true);
        gl.glUniformMatrix4fv(matrixID, 1, false, MVP.getBuffer());
        drawBox(gl, GL_FRONT);
        st.useProgram(gl, false);
        st.attachShaderProgram(gl, cubeProgram, false); // detach cube program

        gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl.glViewport(0, 0, viewPortWidth, viewPortHeight);
        gl.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
        gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        raycasting(gl);

    }

    public void raycasting(GL4 gl) {
        st.attachShaderProgram(gl, rcProgram, true);
        st.useProgram(gl, true);
        gl.glUniformMatrix4fv(rcMvpLoc, 1, false, MVP.getBuffer());
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
        Mat4 normalMatrix = camera.getViewMatrix()
                .multiply(model)
                .getInverse()
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
        gl.glBindTexture(GL_TEXTURE_2D, renderedTexture.get(0));
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
        st.attachShaderProgram(gl, rcProgram, false); // detach raycasting program
    }


    private void drawBox(GL3 gl, int face) {
        gl.glEnable(GL_CULL_FACE);
        gl.glCullFace(face);
        gl.glBindVertexArray(cubeVAO);
        gl.glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0);
        gl.glBindVertexArray(0);
        gl.glDisable(GL_CULL_FACE);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
        GL4 gl = drawable.getGL().getGL4();
        viewPortWidth = w;
        viewPortHeight = h;
        gl.glViewport(0, 0, w, h);
        float aspect = (float) w / (float) h;
        camera.setupProjection(45f, aspect, 1, 500);

        frameBufferID = initFrameBuffer(gl);
    }

    private int initFrameBuffer(GL3 gl) {
        // The framebuffer, which regroups 0, 1, or more textures, and 0 or 1 depth buffer.
        IntBuffer FramebufferName = IntBuffer.allocate(1);
        gl.glGenFramebuffers(1, FramebufferName);
        gl.glBindFramebuffer(GL_FRAMEBUFFER, FramebufferName.get(0));

        // The texture we're going to render to
        gl.glGenTextures(1, renderedTexture);

        // "Bind" the newly created texture : all future texture functions will modify this texture
        gl.glBindTexture(GL_TEXTURE_2D, renderedTexture.get(0));

        // Give an empty image to OpenGL ( the last "0" means "empty" )
        gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, viewPortWidth, viewPortHeight, 0, GL_RGB, GL_UNSIGNED_BYTE, null);

        // Poor filtering
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        // The depth buffer
        IntBuffer depthrenderbuffer = IntBuffer.allocate(1);
        gl.glGenRenderbuffers(1, depthrenderbuffer);
        gl.glBindRenderbuffer(GL_RENDERBUFFER, depthrenderbuffer.get(0));
        gl.glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, viewPortWidth, viewPortHeight);
        gl.glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthrenderbuffer.get(0));
        gl.glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, renderedTexture.get(0), 0);

        // Set the list of draw buffers.
        int[] DrawBuffers = {GL_COLOR_ATTACHMENT0};
        gl.glDrawBuffers(1, DrawBuffers, 0);

        if (gl.glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new GLException("Framebuffer not ok!");
        }

        gl.glEnable(GL_DEPTH_TEST);

        return FramebufferName.get(0);
    }


    private class RayCastingLocations {

        private final String fragmentShader = "raycasting.frag";
        private final String vertexShader = "raycasting.vert";

        private ShaderProgram program;

        public void init(GL4 gl) {
            program = new ShaderProgram();
            ShaderCode vp = ShaderCode.create(
                    gl, GL_VERTEX_SHADER, 1, Main.class, new String[]{shaderName(vertexShader)}, false
            );
            ShaderCode fp = ShaderCode.create(
                    gl, GL_FRAGMENT_SHADER, 1, Main.class, new String[]{shaderName(fragmentShader)}, false
            );
            rcProgram.add(gl, vp, System.err);
            rcProgram.add(gl, fp, System.err);
        }

        public ShaderProgram program() {
            return this.program;
        }

        private String shaderName(String name) {
            return "shaders/" + name;
        }

        public void destroy(GL4 gl) {
            program.destroy(gl);
        }
    }

}
