package palasjir.viewer.render

import java.nio.ByteBuffer

class TransferFunction {
    private val func: IntArray

    init {
        func = IntArray(SIZE)
        for (i in 0 until SIZE) {
            func[i] = i
        }
    }

    val buffer: ByteBuffer
        get() {
            val rgba = ByteBuffer.allocate(SIZE * Integer.BYTES)
            for (b in func) {
                tf3(rgba, b)
            }
            rgba.rewind()
            return rgba
        }

    private fun tf1(rgba: ByteBuffer, b: Int) {
        rgba.put(b.toByte()) //r
        rgba.put(b.toByte()) //g
        rgba.put(b.toByte()) //b
        rgba.put(b.toByte()) //a
    }

    private fun tf2(rgba: ByteBuffer, b: Int) {
        rgba.put(b.toByte()) //r
        rgba.put(b.toByte()) //g
        rgba.put(b.toByte()) //b
        if (b < 50 || b > 60) {
            rgba.put(0.toByte()) //a
        } else {
            rgba.put(b.toByte())
        }
    }

    private fun tf3(rgba: ByteBuffer, b: Int) {
        when {
            b in 41..64 -> {
                rgba.put(220.toByte()) //r
                rgba.put(100.toByte()) //g
                rgba.put(0.toByte()) //b
                rgba.put(b.toByte()) //a
            }
            b > 100 -> {
                rgba.put(220.toByte()) //r
                rgba.put(220.toByte()) //g
                rgba.put(220.toByte()) //b
                rgba.put(200.toByte()) //a
            }
            else -> {
                rgba.put(0.toByte()) //r
                rgba.put(0.toByte()) //g
                rgba.put(0.toByte()) //b
                rgba.put(0.toByte())
            }
        }
    }

    companion object {
        val SIZE = 256
    }

}
