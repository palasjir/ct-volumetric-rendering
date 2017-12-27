package palasjir.viewer.coordinates;

import com.hackoeur.jglm.Mat4;
import com.hackoeur.jglm.Matrices;
import com.hackoeur.jglm.Vec3;

import java.awt.Point;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import palasjir.viewer.coordinates.GLSphericalCoordinates;


public class Camera {

    private final Vec3 up;
    private final Vec3 at;

    private float aspectRatio;
    private float fovy;
    private float near;
    private float far;
    private float zoomfactor = 1;

    private Mat4 view;
    private Mat4 projection;
    private GLSphericalCoordinates eye;

    public Camera(double zoom) {
        Vector3D eyeCart = new Vector3D(0, 0, zoom);
        at = new Vec3(0, 0, 0);
        up = new Vec3(0, 1, 0);
        eye = new GLSphericalCoordinates(eyeCart);
        updateView();
    }

    public void drag(Point start, Point end) {
        if (start != null && end != null) {

            int dx = start.x - end.x;
            int dy = start.y - end.y;

            if (dx != 0 || dy != 0) {

                double theta = eye.getTheta() + Math.toRadians(dx);
                double phi = eye.getPhi() + Math.toRadians(dy);

                // top lock
                if (dy < 0 && phi < 0) {
                    phi = Math.toRadians(0.000000001);
                }

                // bottom lock
                if (phi > Math.PI) {
                    phi = Math.toRadians(179.999999999);
                }

                eye = new GLSphericalCoordinates(eye.getR(), theta, phi);
            }
        }

        updateView();
    }

    private void updateView() {
        view = Matrices.lookAt(eye.getCartesianGLM(), at, up);
    }


    public void zoom(double distance) {
        zoomfactor += (float) distance * 0.01f;
        projection = Matrices.perspective(fovy * zoomfactor, aspectRatio, near, far);
        updateView();
    }

    public void setupProjection(float fovy, float aspectRatio, float near, float far) {
        this.aspectRatio = aspectRatio;
        this.fovy = fovy;
        this.near = near;
        this.far = far;
        projection = Matrices.perspective(fovy, aspectRatio, near, far);
    }

    public Mat4 getViewMatrix() {
        return view;
    }

    public Mat4 getProjectionMatrix() {
        return projection;
    }

    public void setPosition(Vec3 eye) {
        this.eye = new GLSphericalCoordinates(eye);
        updateView();
    }

    public Vec3 getPosition() {
        return eye.getCartesianGLM();
    }

    public Vec3 getViewDirection() {
        return at.subtract(eye.getCartesianGLM()).getUnitVector();
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    public Mat4 mvp(Mat4 model) {
        return new Mat4(projection.multiply(view).multiply(model));
    }

}
