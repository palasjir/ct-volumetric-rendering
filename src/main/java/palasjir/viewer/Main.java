package palasjir.viewer;


import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {

    private static final int CANVAS_WIDTH = 500;
    private static final int CANVAS_HEIGHT = 500;
    private static final int FPS = 25;

    public static void main(String[] args) {

        // Run the GUI codes in the event-dispatching thread for thread safety
        SwingUtilities.invokeLater(() -> {

            // Create the OpenGL rendering canvas
            final Dimension dimension = new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT);
            final GLCanvas canvas = new VisualizationPanel(dimension);

            // Create a animator that drives canvas' display() at the specified FPS.
            final FPSAnimator animator = new FPSAnimator(canvas, FPS, true);

            // Create the top-level container
            final JFrame frame = new JFrame(); // Swing's JFrame or AWT's Frame
            frame.getContentPane().add(canvas);
            final Thread closingThread = new Thread(() -> {
                if (animator.isStarted()) animator.stop();
                System.exit(0);
            });
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    closingThread.start();
                }
            });
            frame.setTitle("CT Viewer");
            frame.pack();
            frame.setVisible(true);
            animator.start(); // start the animation loop
        });

    }

}
