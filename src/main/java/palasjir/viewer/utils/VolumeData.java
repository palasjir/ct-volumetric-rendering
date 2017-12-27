package palasjir.viewer.utils;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class VolumeData {

    public static final int IMG_WIDTH = 256;
    public static final int IMG_HEIGHT = 256;
    public static final int IMG_DEPTH_ORIG = 113;
    public static final int IMG_DEPTH = 2 * IMG_DEPTH_ORIG;

    private static final int PIXELS_IN_BUFFER = 1;
    private static final int IMG_VOXELS = IMG_WIDTH * IMG_DEPTH * IMG_WIDTH;
    private static final float MAX_VALUE = 3272.0f;
    private static final String IMAGES_DIR = "cthead/cthead-16bit";
    private static final Logger LOGGER = Logger.getLogger("Loading");

    private int[][][] pixels;

    public VolumeData() {
        loadImages();
    }

    private void loadImages() {
        pixels = new int[IMG_WIDTH][IMG_HEIGHT][IMG_DEPTH];

        BufferedImage tempImage;
        WritableRaster tempRaster;

        System.out.println("Loading images ...");

        for (int z = 0; z < IMG_DEPTH_ORIG; z++) {
            File file = new File(IMAGES_DIR + String.format("%03d", z + 1) + ".png");
            try {
                tempImage = ImageIO.read(file);
                tempRaster = tempImage.getRaster();
                for (int x = 0; x < tempImage.getWidth(); x++) {
                    for (int y = 0; y < tempImage.getHeight(); y++) {
                        int value = tempRaster.getSample(x, y, 0);
                        pixels[x][y][z * 2] = value;
                    }
                }
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Image loading failed.", ex);
                return;
            }
        }
        System.out.println("Loading done.");

        System.out.println("Start interpolating done ...");
        for (int i = 1; i < IMG_DEPTH - 1; i += 2) {
            for (int x = 0; x < IMG_WIDTH; x++) {
                for (int y = 0; y < IMG_HEIGHT; y++) {
                    pixels[x][y][i] = (pixels[x][y][i - 1] + pixels[x][y][i + 1]) / 2;
                }
            }
        }
        System.out.println("Interpolating done.");

    }

    public ByteBuffer getBuffer() {
        int size = IMG_WIDTH * IMG_HEIGHT * IMG_DEPTH * PIXELS_IN_BUFFER;
        ByteBuffer buffer = ByteBuffer.allocate(size);
        for (int z = 1; z < IMG_DEPTH; z++) {
            for (int y = 0; y < IMG_HEIGHT; y++) {
                for (int x = 0; x < IMG_WIDTH; x++) {
                    buffer.put(scaleToByte(pixels[x][y][z]));
                }
            }
        }
        buffer.rewind();
        return buffer;
    }

    private byte scaleToByte(int value) {
        return (byte) ((int) (255.0f * ((float) value / MAX_VALUE)));
    }

    public FloatBuffer gradientsBuffer() {
        int CHANNELS = 3;
        int size = IMG_VOXELS * CHANNELS;
        FloatBuffer buffer = FloatBuffer.allocate(size);
        for (int z = 0; z < IMG_DEPTH; z++) {
            for (int y = 0; y < IMG_HEIGHT; y++) {
                for (int x = 0; x < IMG_WIDTH; x++) {
                    buffer.put((float) ((x == 0 || x == IMG_WIDTH - 1) ? 0 : (pixels[x - 1][y][z] - pixels[x + 1][y][z])));
                    buffer.put((float) ((y == 0 || y == IMG_HEIGHT - 1) ? 0 : (pixels[x][y - 1][z] - pixels[x][y + 1][z])));
                    buffer.put((float) ((z == 0 || z == IMG_DEPTH - 1) ? 0 : (pixels[x][y][z - 1] - pixels[x][y][z + 1])));
                }
            }
        }
        buffer.rewind();
        return buffer;
    }

}
