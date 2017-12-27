package palasjir.viewer.utils

import java.awt.image.BufferedImage
import java.awt.image.WritableRaster
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO

class VolumeData {

    private val pixels: Array<Array<IntArray>> = Array(IMG_WIDTH) { Array(IMG_HEIGHT) { IntArray(IMG_DEPTH) } }

    init {
        loadImages()
    }

    private fun loadImages() {

        var tempImage: BufferedImage
        var tempRaster: WritableRaster

        println("Loading images ...")

        for (z in 0..IMG_DEPTH_ORIG - 1) {
            val file = File(IMAGES_DIR + String.format("%03d", z + 1) + ".png")
            try {
                tempImage = ImageIO.read(file)
                tempRaster = tempImage.raster
                for (x in 0..tempImage.width - 1) {
                    for (y in 0..tempImage.height - 1) {
                        val value = tempRaster.getSample(x, y, 0)
                        pixels[x][y][z * 2] = value
                    }
                }
            } catch (ex: IOException) {
                LOGGER.log(Level.SEVERE, "Image loading failed.", ex)
                return
            }

        }
        println("Loading done.")

        println("Start interpolating done ...")
        var i = 1
        while (i < IMG_DEPTH - 1) {
            for (x in 0..IMG_WIDTH - 1) {
                for (y in 0..IMG_HEIGHT - 1) {
                    pixels[x][y][i] = (pixels[x][y][i - 1] + pixels[x][y][i + 1]) / 2
                }
            }
            i += 2
        }
        println("Interpolating done.")

    }

    val buffer: ByteBuffer
        get() {
            val size = IMG_WIDTH * IMG_HEIGHT * IMG_DEPTH * PIXELS_IN_BUFFER
            val buffer = ByteBuffer.allocate(size)
            for (z in 1..IMG_DEPTH - 1) {
                for (y in 0..IMG_HEIGHT - 1) {
                    for (x in 0..IMG_WIDTH - 1) {
                        buffer.put(scaleToByte(pixels[x][y][z]))
                    }
                }
            }
            buffer.rewind()
            return buffer
        }

    private fun scaleToByte(value: Int): Byte {
        return (255.0f * (value.toFloat() / MAX_VALUE)).toInt().toByte()
    }

    fun gradientsBuffer(): FloatBuffer {
        val size = IMG_VOXELS * CHANNELS
        val buffer = FloatBuffer.allocate(size)
        for (z in 0..IMG_DEPTH - 1) {
            for (y in 0..IMG_HEIGHT - 1) {
                for (x in 0..IMG_WIDTH - 1) {
                    buffer.put((if (x == 0 || x == IMG_WIDTH - 1) 0 else pixels[x - 1][y][z] - pixels[x + 1][y][z]).toFloat())
                    buffer.put((if (y == 0 || y == IMG_HEIGHT - 1) 0 else pixels[x][y - 1][z] - pixels[x][y + 1][z]).toFloat())
                    buffer.put((if (z == 0 || z == IMG_DEPTH - 1) 0 else pixels[x][y][z - 1] - pixels[x][y][z + 1]).toFloat())
                }
            }
        }
        buffer.rewind()
        return buffer
    }

    companion object {
        val CHANNELS = 3
        val IMG_WIDTH = 256
        val IMG_HEIGHT = 256
        val IMG_DEPTH_ORIG = 113
        val IMG_DEPTH = 2 * IMG_DEPTH_ORIG

        private val PIXELS_IN_BUFFER = 1
        private val IMG_VOXELS = IMG_WIDTH * IMG_DEPTH * IMG_WIDTH
        private val MAX_VALUE = 3272.0f
        private val IMAGES_DIR = "cthead/cthead-16bit"
        private val LOGGER = Logger.getLogger("Loading")
    }

}
