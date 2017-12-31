package palasjir.viewer.render

import com.jogamp.opengl.GL3
import com.jogamp.opengl.GL3.*
import com.jogamp.opengl.GLException
import palasjir.viewer.coordinates.Camera
import palasjir.viewer.utils.*

class FrameBuffer(val camera: Camera) {

    var frameBufferId: Int = -1
        private set
    var renderedTextureId: Int = -1
        private set

    fun init(gl: GL3) {
        // The framebuffer, which regroups 0, 1, or more textures, and 0 or 1 depth buffer.
        frameBufferId = genFrameBufferId(gl)

        gl.glBindFramebuffer(GL_FRAMEBUFFER, frameBufferId)

        // The texture we're going to render to
        renderedTextureId = genTextureId(gl)

        // "Bind" the newly created texture : all future texture functions will modify this texture
        gl.glBindTexture(GL_TEXTURE_2D, renderedTextureId)

        // Give an empty image to OpenGL ( the last "0" means "empty" )
        gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, camera.viewPortWidth, camera.viewPortHeight, 0, GL_RGB, GL_UNSIGNED_BYTE, null)

        // Poor filtering
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

        // The depth buffer
        val depthRenderBufferId = genRenderBufferId(gl)
        gl.glBindRenderbuffer(GL_RENDERBUFFER, depthRenderBufferId)
        gl.glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, camera.viewPortWidth, camera.viewPortHeight)
        gl.glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRenderBufferId)
        gl.glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, renderedTextureId, 0)

        // Set the list of draw buffers.
        val drawBuffers = intArrayOf(GL_COLOR_ATTACHMENT0)
        gl.glDrawBuffers(1, drawBuffers, 0)

        if (gl.glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw GLException("Framebuffer not ok!")
        }

        gl.glEnable(GL_DEPTH_TEST)
    }

    fun clear(gl: GL3) {
        gl.glBindFramebuffer(GL_FRAMEBUFFER, 0)
        gl.glViewport(0, 0, camera.viewPortWidth, camera.viewPortHeight)
        gl.glClearColor(0.2f, 0.2f, 0.2f, 1.0f)
        gl.glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
    }

    fun activate(gl: GL3) {
        // Render to buffer
        gl.glBindFramebuffer(GL_FRAMEBUFFER, 0)
        gl.glBindFramebuffer(GL_FRAMEBUFFER, frameBufferId)
        // Render on the whole framebuffer, complete from the lower left corner to the upper right
        gl.glViewport(0, 0, camera.viewPortWidth, camera.viewPortHeight)
        gl.glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
    }
}