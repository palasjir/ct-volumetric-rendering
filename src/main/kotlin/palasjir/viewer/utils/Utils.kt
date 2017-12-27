package palasjir.viewer.utils

import com.jogamp.opengl.GL
import com.jogamp.opengl.GL3

import java.nio.IntBuffer

fun genBufferId(gl: GL): Int {
    val buffer = IntBuffer.allocate(1)
    gl.glGenBuffers(1, buffer)
    return buffer.get(0)
}

fun genVertexArraysId(gl: GL3): Int {
    val buffer = IntBuffer.allocate(1)
    gl.glGenVertexArrays(1, buffer)
    return buffer.get(0)
}

fun genTextureId(gl: GL): Int {
    val tfIDbuff = IntBuffer.allocate(1)
    gl.glGenTextures(1, tfIDbuff)
    return tfIDbuff.get(0)
}

fun genFrameBufferId(gl: GL): Int {
    val buffer = IntBuffer.allocate(1)
    gl.glGenFramebuffers(1, buffer)
    return buffer.get(0)
}

fun genRenderBufferId(gl: GL): Int {
    val buffer = IntBuffer.allocate(1)
    gl.glGenRenderbuffers(1, buffer)
    return buffer.get(0)
}
