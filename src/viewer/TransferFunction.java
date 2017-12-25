/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package viewer;

import java.nio.ByteBuffer;

/**
 *
 * @author palasjiri
 */
public class TransferFunction {

    int[] func;

    public TransferFunction() {

        func = new int[256];

        for (int i = 0; i < 256; i++) {
            func[i] = i;
        }

    }

    public ByteBuffer getBuffer() {
        // create rgba buffer
        ByteBuffer rgba = ByteBuffer.allocate(256 * 4);

        // currently there goes just grayscale
        for (int b : func) {
            tf3(rgba, b);

        }

        rgba.rewind();
        return rgba;
    }

    private void tf1(ByteBuffer rgba, int b) {
        rgba.put((byte) b); //r
        rgba.put((byte) b); //g
        rgba.put((byte) b); //b
        rgba.put((byte) b); //a
    }
    
    private void tf2(ByteBuffer rgba, int b){
        rgba.put((byte) b); //r
        rgba.put((byte) b); //g
        rgba.put((byte) b); //b
        if(b < 50 || b > 60 ){
            rgba.put((byte) 0); //a
        }else{
            rgba.put((byte) b);
        }
    }
    
    private void tf3(ByteBuffer rgba, int b){
        
        if(b > 40 && b < 65){
            rgba.put((byte) 220); //r
            rgba.put((byte) 100); //g
            rgba.put((byte) 0); //b
            rgba.put((byte) b); //a
        }else if(b > 100){
            rgba.put((byte) 220); //r
            rgba.put((byte) 220); //g
            rgba.put((byte) 220); //b
            rgba.put((byte) 200); //a
        }else{
            rgba.put((byte) 0); //r
            rgba.put((byte) 0); //g
            rgba.put((byte) 0); //b
            rgba.put((byte) 0);
        }
    }

}
