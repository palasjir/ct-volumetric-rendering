package palasjir.viewer.ui;

import palasjir.viewer.coordinates.Camera;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

public class VisualisationMouseInputAdapter extends MouseInputAdapter {

    private Point start = null;
    private Point end = null;
    private Camera camera;

    public VisualisationMouseInputAdapter(Camera camera) {
        this.camera = camera;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            start = e.getPoint();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        end = e.getPoint();
        camera.drag(start, end);
        start = new Point(end);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        start = null;
        end = null;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        camera.zoom(e.getWheelRotation());
    }

}
