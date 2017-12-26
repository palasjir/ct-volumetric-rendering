/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package viewer;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author palasjiri
 */
public class VolumeData {

    private static final String IMAGES_DIR = "cthead/cthead-16bit";

    //data size
    public static final int IMG_WIDTH = 256;
    public static final int IMG_HEIGHT = 256;
    public static final int IMG_DEPTH = 2 * 113;
    public static final int IMG_VOXELS = IMG_WIDTH * IMG_DEPTH * IMG_WIDTH;
    public static final float MAX_VALUE = 3272.0f;

    // original volume data
    int[][][] data;

    public static final int pixelsInBuffer = 1;

    public VolumeData() {
        loadImages();
    }

    private void loadImages() {
        data = new int[IMG_WIDTH][IMG_HEIGHT][IMG_DEPTH];

        BufferedImage tempImage;
        WritableRaster tempRaster;

        System.out.println("Loading images ...");
        int value;
        File file;
        for (int z = 0; z < IMG_DEPTH / 2; z++) {
            file = new File(IMAGES_DIR + String.format("%03d", z + 1) + ".png");
            String path = file.getAbsolutePath();
            try {
                tempImage = ImageIO.read(file);
                tempRaster = tempImage.getRaster();
                for (int x = 0; x < tempImage.getWidth(); x++) {
                    for (int y = 0; y < tempImage.getHeight(); y++) {
                        value = tempRaster.getSample(x, y, 0);
                        data[x][y][z * 2] = value;
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger("Loading").log(Level.SEVERE, null, ex);
            }
        }
        System.out.println("Loading done.");

        System.out.println("Start interpolating done ...");
        for (int i = 1; i < IMG_DEPTH - 1; i += 2) {
            for (int x = 0; x < 256; x++) {
                for (int y = 0; y < 256; y++) {
                    data[x][y][i] = (data[x][y][i - 1] + data[x][y][i + 1]) / 2;
                }
            }
        }
        System.out.println("Interpolating done.");

    }

    public ByteBuffer getBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(IMG_WIDTH * IMG_HEIGHT * IMG_DEPTH * pixelsInBuffer);
        for (int z = 1; z < IMG_DEPTH; z++) {
            for (int y = 0; y < IMG_HEIGHT; y++) {
                for (int x = 0; x < IMG_WIDTH; x++) {

                    buffer.put(scaleToByte(data[x][y][z]));
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
        //we store rgb in each cell
        FloatBuffer buffer = FloatBuffer.allocate(IMG_VOXELS * 3);

        for (int z = 0; z < IMG_DEPTH; z++) {
                for (int y = 0; y < IMG_HEIGHT; y++) {
                    for (int x = 0; x < IMG_WIDTH; x++) {
                    buffer.put((float) ((x == 0 || x == IMG_WIDTH - 1) ? 0 : (data[x - 1][y][z] - data[x + 1][y][z])));
                    buffer.put((float) ((y == 0 || y == IMG_HEIGHT - 1) ? 0 : (data[x][y - 1][z] - data[x][y + 1][z])));
                    buffer.put((float) ((z == 0 || z == IMG_DEPTH - 1) ? 0 : (data[x][y][z - 1] - data[x][y][z + 1])));
                }
            }
        }
        buffer.rewind();
        return buffer;
    }

}
