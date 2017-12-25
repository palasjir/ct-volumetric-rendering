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
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES2.*;
import static com.jogamp.opengl.GL2GL3.GL_TEXTURE_1D;

/**
 *
 * @author palasjiri
 */
public class VisualizationPanel extends GLCanvas implements GLEventListener {

    private static final String IMAGES_DIR = "cthead/cthead-16bit";
    // Konstanty určující velikost obrázku
    protected static final int IMG_WIDTH = 256;
    protected static final int IMG_HEIGHT = 256;
    protected static final int IMG_DEPTH = 113 * 2;
    
    int[][][] voxels;

    ShaderState st;

    Animator animator;

    // transfer function 1d texture
    int g_tffTexObj;

    // backface texture id
    int bfTexID;

    // 3D texture id
    int g_volTexObj;

    float stepSize = 0.001f;

    int g_angle = 1;

    // frame buffer
    int frameBufferID;
    int tfTextureID;
    int volumeTextureID;
    int gradientsTextureID;
    int backFaceTextureID;
    int cubeVAO;
    IntBuffer renderedTexture = IntBuffer.allocate(1);

    // programs
    ShaderProgram bfProgram;
    ShaderProgram rcProgram;
    ShaderProgram cubeProgram;
    ShaderProgram scrProgram;

    // Geometry
    static Mat4 model = new Mat4(1.0f);
    
    static{
        model = Matrices.rotate(model, 90.0f, new Vec3(1.0f,0.0f,0.0f));
        model = model.translate(new Vec3(-0.5f, -0.5f, -0.5f));
    }
    
    Mat4 MVP;

    // shader locations
    int verticesID; // input of vertexShader
    int matrixID;   // uniform location of MVP matrix
    int texID;
    int timeID;
    
    int screenSizeLoc;
    int stepSizeLoc;
    int transferFuncLoc;
    int backFaceLoc;
    int volumeLoc;
    int gradientsLoc;

    VisualizationMouseInputAdapter mAdapter;
    Camera camera;

    // The fullscreen quad's FBO
    static final float[] g_quad_vertex_buffer_data = {
        -1.0f, -1.0f, 0.0f,
        1.0f, -1.0f, 0.0f,
        -1.0f, 1.0f, 0.0f,
        -1.0f, 1.0f, 0.0f,
        1.0f, -1.0f, 0.0f,
        1.0f, 1.0f, 0.0f,};

    IntBuffer quad_vertexbuffer = IntBuffer.allocate(1);
    VolumeData vData;
    private int VLoc;
    private int normalMatrixLoc;
    
    public VisualizationPanel(Dimension dimension) throws GLException {
        super(new GLCapabilities(GLProfile.get(GLProfile.GL3)));
        GLProfile profile = GLProfile.getMaxProgrammable(true);
        GLCapabilities capabilities = new GLCapabilities(profile);


        st = new ShaderState();
        setPreferredSize(dimension);
        addGLEventListener(this);

        camera = new Camera(3.0f);
        mAdapter = new VisualizationMouseInputAdapter(camera);

        addMouseListener(mAdapter);
        addMouseMotionListener(mAdapter);
        addMouseWheelListener(mAdapter);
        
        vData = new VolumeData();
    }

    @Override
    public void init(GLAutoDrawable drawable) throws GLException {

        GL3 gl = drawable.getGL().getGL3();

        initShaders(gl);
        cubeVAO = initCube(gl);

        tfTextureID = initTransferFunction(gl);
        volumeTextureID = initVolumeTexture(gl, null, IMG_WIDTH, IMG_HEIGHT, IMG_DEPTH);
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
        GL3 gl = drawable.getGL().getGL3();
        st.destroy(gl);
    }

    private void initShaders(GL3 gl) {

        rcProgram = new ShaderProgram();
        ShaderCode vp1 = ShaderCode.create(gl, GL_VERTEX_SHADER, 1, this.getClass(), new String[]{"shaders/raycasting.vert"}, false);
        ShaderCode fp1 = ShaderCode.create(gl, GL_FRAGMENT_SHADER, 1, this.getClass(), new String[]{"shaders/raycasting.frag"}, false);
        rcProgram.add(gl, vp1, System.err);
        rcProgram.add(gl, fp1, System.err);
        st.attachShaderProgram(gl, rcProgram, true);
        vp1.destroy(gl);
        fp1.destroy(gl);
        
        screenSizeLoc = st.getUniformLocation(gl, "ScreenSize");
        stepSizeLoc = st.getUniformLocation(gl, "StepSize");
        transferFuncLoc = st.getUniformLocation(gl, "TransferFunc");
        backFaceLoc = st.getUniformLocation(gl, "ExitPoints");
        volumeLoc = st.getUniformLocation(gl, "VolumeTex");
        gradientsLoc = st.getUniformLocation(gl, "gradients");
        verticesID = st.getAttribLocation(gl, "vVertex");
        VLoc = st.getUniformLocation(gl, "V");
        normalMatrixLoc = st.getUniformLocation(gl, "normalMatrix");
        

        cubeProgram = new ShaderProgram();
        ShaderCode vp2 = ShaderCode.create(gl, GL_VERTEX_SHADER, 1, this.getClass(), new String[]{"shaders/cube_shader.vert"}, false);
        ShaderCode fp2 = ShaderCode.create(gl, GL_FRAGMENT_SHADER, 1, this.getClass(), new String[]{"shaders/cube_shader.frag"}, false);
        cubeProgram.add(gl, vp2, System.err);
        cubeProgram.add(gl, fp2, System.err);
        st.attachShaderProgram(gl, cubeProgram, true);
        vp2.destroy(gl);
        fp2.destroy(gl);

        verticesID = st.getAttribLocation(gl, "vVertex");
        matrixID = st.getUniformLocation(gl, "MVP");
        texID = st.getUniformLocation(gl, "renderedTexture");
        timeID = st.getUniformLocation(gl, "time");
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

    private int initTransferFunction(GL3 gl) {
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
    
    private int initGradients(GL3 gl) {
        
        FloatBuffer data = vData.gradientsBuffer();
        
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
    
    private int initVolumeTexture(GL3 gl, String file, int w, int h, int d) {
        IntBuffer volumeTextureIDs = IntBuffer.allocate(1);
        ByteBuffer buffer = vData.getBuffer();
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
        
        
        gl.glTexImage3D(GL_TEXTURE_3D, 0, GL_LUMINANCE, w, h, d, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, buffer);
        
        // mipmapping ???
        gl.glGenerateMipmap(GL_TEXTURE_3D);
        // destroy
        buffer = null;

        return volumeTextureIDs.get(0);
    }
    

    @Override
    public void display(GLAutoDrawable drawable) {
        GL3 gl = drawable.getGL().getGL3();

        // Render to buffer
        gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl.glBindFramebuffer(GL_FRAMEBUFFER, frameBufferID);
        gl.glViewport(0, 0, getWidth(), getHeight()); // Render on the whole framebuffer, complete from the lower left corner to the upper right
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
        gl.glViewport(0, 0, getWidth(), getHeight());
        gl.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
        gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        raycasting(gl);

    }

    public void raycasting(GL3 gl) {
        st.attachShaderProgram(gl, rcProgram, true); // attach cube program
        st.useProgram(gl, true);
        gl.glUniformMatrix4fv(matrixID, 1, false, MVP.getBuffer());
        gl.glUniform2f(screenSizeLoc, (float) getWidth(), (float) getHeight());
        gl.glUniformMatrix4fv(VLoc, 1, false, camera.getViewMatrix().getBuffer());
        Mat4 normalMatrix = camera.getViewMatrix().multiply(model).getInverse().transpose();
        gl.glUniformMatrix4fv(normalMatrixLoc, 1, false, normalMatrix.getBuffer());
        
        gl.glUniform1f(stepSizeLoc, stepSize);
        // tf uniform
        gl.glActiveTexture(GL_TEXTURE0);
        gl.glBindTexture(GL_TEXTURE_1D, tfTextureID);
        gl.glUniform1i(transferFuncLoc, 0);
        
        // backface texture
        int a = st.getUniformLocation(gl, "exitPoints");
        //int a = gl.glGetUniformLocation(rcProgram.program(), "exitPoints");
        
        //System.out.println(a);
        gl.glActiveTexture(GL_TEXTURE1);
        gl.glBindTexture(GL_TEXTURE_2D, renderedTexture.get(0));
        gl.glUniform1i(a, 1);

        // volume texture
        gl.glActiveTexture(GL_TEXTURE2);
        gl.glBindTexture(GL_TEXTURE_3D, volumeTextureID);
        gl.glUniform1i(volumeLoc, 2);
        
        // gradients texture
        //gl.glActiveTexture(GL_TEXTURE3);
        //gl.glBindTexture(GL_TEXTURE_3D, gradientsTextureID);
        //gl.glUniform1i(gradientsLoc, 3);
        
        // drawBox(gl, GL_BACK);
        st.useProgram(gl, false);
        st.attachShaderProgram(gl, rcProgram, false); // detach raycasting program
    }
    

    void drawBox(GL3 gl, int face) {
        gl.glEnable(GL_CULL_FACE);
        gl.glCullFace(face);
        gl.glBindVertexArray(cubeVAO);
        gl.glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0);
        gl.glBindVertexArray(0);
        gl.glDisable(GL_CULL_FACE);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
        GL3 gl = drawable.getGL().getGL3();
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
        gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, getWidth(), getHeight(), 0, GL_RGB, GL_UNSIGNED_BYTE, null);

        // Poor filtering
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        // The depth buffer
        IntBuffer depthrenderbuffer = IntBuffer.allocate(1);
        gl.glGenRenderbuffers(1, depthrenderbuffer);
        gl.glBindRenderbuffer(GL_RENDERBUFFER, depthrenderbuffer.get(0));
        gl.glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, getWidth(), getHeight());
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

    

    private class VisualizationMouseInputAdapter extends MouseInputAdapter {

        Point start = null;
        Point end = null;

        protected boolean moved = false;
        boolean picked = true;
        protected Point pickedPoint = null;

        Camera camera;

        public VisualizationMouseInputAdapter(Camera camera) {
            this.camera = camera;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                pickedPoint = e.getPoint();
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                start = e.getPoint();
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            end = e.getPoint();
            camera.drag(start, end);
            start = new Point(end);
            moved = true;
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            start = null;
            end = null;
            moved = false;
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            camera.zoom(e.getWheelRotation());
        }

    }

}
