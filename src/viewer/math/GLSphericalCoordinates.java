/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package viewer.math;

import com.hackoeur.jglm.Vec3;
import org.apache.commons.math3.geometry.euclidean.threed.SphericalCoordinates;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 *
 * @author palasjiri
 */
public class GLSphericalCoordinates extends SphericalCoordinates{

    public GLSphericalCoordinates(Vector3D v) {
        super(new Vector3D(v.getZ(), v.getX(), v.getY()));
    }
    
    public GLSphericalCoordinates(Vec3 v){        
        super(new Vector3D(v.getX(), v.getY(), v.getZ()));
    }

    public GLSphericalCoordinates(double radius, double theta, double phi) {
        super(radius, theta, phi);
    }
   
    @Override
    public Vector3D getCartesian() {
        Vector3D v = super.getCartesian();
        return new Vector3D(v.getY(), v.getZ(), v.getX());
    }
    
    public Vec3 getCartesianGLM(){
        Vector3D v = super.getCartesian();
        return new Vec3( (float) v.getY(), (float) v.getZ(), (float) v.getX());
    }
    
    
}
