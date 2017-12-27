package palasjir.viewer.utils;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import java.nio.IntBuffer;

public class Utils {

    public static int genBufferId(GL gl) {
        IntBuffer buffer = IntBuffer.allocate(1);
        gl.glGenBuffers(1, buffer);
        return buffer.get(0);
    }

    public static int genVertexArraysId(GL3 gl) {
        IntBuffer buffer = IntBuffer.allocate(1);
        gl.glGenVertexArrays(1, buffer);
        return buffer.get(0);
    }

    public static int genTextureId(GL gl) {
        IntBuffer tfIDbuff = IntBuffer.allocate(1);
        gl.glGenTextures(1, tfIDbuff);
        return tfIDbuff.get(0);
    }

    public static int genFrameBufferId(GL gl) {
        IntBuffer buffer = IntBuffer.allocate(1);
        gl.glGenFramebuffers(1, buffer);
        return buffer.get(0);
    }

    public static int genRenderBufferId(GL gl) {
        IntBuffer buffer = IntBuffer.allocate(1);
        gl.glGenRenderbuffers(1, buffer);
        return buffer.get(0);
    }

}
