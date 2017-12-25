/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package viewer;

/**
 *
 * @author palasjiri
 */
public class UnitCube {

    public UnitCube() {
    }

    public UnitCube(float x, float y, float z) {
        float max = Math.max(x, Math.max(y, z));
        
        x = x/max;
        y = y/max;
        z = z/max;
        
        vertices = new float[]{
            0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, z   ,
            0.0f, y   , 0.0f,
            0.0f, y   , z   ,
            x   , 0.0f, 0.0f,
            x   , 0.0f, z   ,
            x   , y   , 0.0f,
            x   , y   , z
        };
    }
    
    
    
    public float[] vertices = {
	0.0f, 0.0f, 0.0f,
	0.0f, 0.0f, 1.0f,
	0.0f, 1.0f, 0.0f,
	0.0f, 1.0f, 1.0f,
	1.0f, 0.0f, 0.0f,
	1.0f, 0.0f, 1.0f,
	1.0f, 1.0f, 0.0f,
	1.0f, 1.0f, 1.0f
    };
    
    public static int[] indices = {
	1,5,7,
	7,3,1,
	0,2,6,
        6,4,0,
	0,1,3,
	3,2,0,
	7,5,4,
	4,6,7,
	2,3,7,
	7,6,2,
	1,0,4,
	4,5,1
    };
}
