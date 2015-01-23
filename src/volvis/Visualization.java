/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;
import javax.swing.Timer;
import util.TFChangeListener;
import util.TrackballInteractor;
import volume.Volume;

/**
 *
 * @author michel
 */
public class Visualization implements GLEventListener, TFChangeListener, ActionListener {

    GLU glu = new GLU();
    Renderer renderer;
    GLCanvas canvas;
    int winWidth, winHeight;
    double fov = 20.0;
    TrackballInteractor trackball;
    private Timer continuousDrawingTimer;
    
    public boolean renderingParametersChanged;
        
    public Visualization(GLCanvas canvas) {
        this.canvas = canvas;
        canvas.addMouseMotionListener(new MouseMotionListener()); // listens to drag events
        canvas.addMouseListener(new MousePressListener());
        canvas.addMouseWheelListener(new MouseWheelHandler());
        
        trackball = new TrackballInteractor(winWidth, winHeight);
        
        continuousDrawingTimer = new Timer(150, this);
        renderingParametersChanged = false;
    }
    
    public void setRenderer(Renderer vis) {
        renderer = vis;
    }

    public void update() {
        canvas.repaint();
    }
    
    @Override
    public void changed() {
        renderingParametersChanged = true;
        
        update();
    }

    @Override
    public void init(GLAutoDrawable drawable) {
       
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        // get the OpenGL rendering context
        GL2 gl = drawable.getGL().getGL2();
        
        if (renderingParametersChanged) {
            double[] viewMatrix = new double[4 * 4];
            gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, viewMatrix, 0);
            renderer.visualize(viewMatrix);

            if (!renderer.done()) {
                continuousDrawingTimer.start();
            }
            
            renderingParametersChanged = false;
        }
        
        BufferedImage imageBuffer = renderer.rendering();
        Volume vol = renderer.getVolume();
        
        if (imageBuffer != null && vol != null) {
            draw(gl, imageBuffer, vol);
            
            if (continuousDrawingTimer.isRunning()) {
                continuousDrawingTimer.restart();
            }
        }
    }
    
    public void draw(GL2 gl, BufferedImage image, Volume volume) {
        // set up the projection transform
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(fov, ((float) winWidth/((float) winHeight)), 0.1, 5000);
        gl.glTranslated(0, 0, -1000);

        // clear screen and set the view transform to the identity matrix
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glDepthFunc(GL2.GL_LESS);

        if (trackball.isRotating()) {
            // when dragging the mouse, keep updating the virtual trackball
            trackball.updateTransform(gl);
        }

        // multiply the current view transform (identity) with trackball transform
        gl.glMultMatrixd(trackball.getTransformationMatrix(), 0);

        drawBoundingBox(gl, volume);

        Texture texture = AWTTextureIO.newTexture(gl.getGLProfile(), image, false);

        gl.glPushAttrib(GL2.GL_LIGHTING_BIT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

        // draw rendered image as a billboard texture
        texture.enable(gl);
        texture.bind(gl);
        double halfWidth = image.getWidth() / 2.0;
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glBegin(GL2.GL_QUADS);
        gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glTexCoord2d(0.0, 0.0);
        gl.glVertex3d(-halfWidth, -halfWidth, 0.0);
        gl.glTexCoord2d(0.0, 1.0);
        gl.glVertex3d(-halfWidth, halfWidth, 0.0);
        gl.glTexCoord2d(1.0, 1.0);
        gl.glVertex3d(halfWidth, halfWidth, 0.0);
        gl.glTexCoord2d(1.0, 0.0);
        gl.glVertex3d(halfWidth, -halfWidth, 0.0);
        gl.glEnd();
        texture.disable(gl);
        texture.destroy(gl);
        gl.glPopMatrix();

        gl.glPopAttrib();

        if (gl.glGetError() > 0) {
            System.out.println("some OpenGL error: " + gl.glGetError());
        }
        
        gl.glFlush();
    }
    
    private void drawBoundingBox(GL2 gl, Volume volume) {
        gl.glPushAttrib(GL2.GL_CURRENT_BIT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glColor4d(1.0, 1.0, 1.0, 1.0);
        gl.glLineWidth(1.5f);
        gl.glEnable(GL2.GL_LINE_SMOOTH);
        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glDisable(GL2.GL_LINE_SMOOTH);
        gl.glDisable(GL2.GL_BLEND);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glPopAttrib();
    }

    // reshape handles window resize
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
        
        renderer.setWinWidth(width);
        renderer.setWinHeight(height);
            
        winWidth = width;
        winHeight = height;
        trackball.setDimensions(width, height);
        canvas.setMinimumSize(new Dimension(0,0));
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (renderer == null || renderer.done()) {
            continuousDrawingTimer.stop();
        }
        
        update();
    }
   
    class MousePressListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            trackball.setMousePos(e.getX(), e.getY());
           
            renderer.setInteractiveMode(true);
        }
       
        @Override
        public void mouseReleased(MouseEvent e) {
            renderer.setInteractiveMode(false);
            changed();
        }
    }
   
    class MouseMotionListener extends MouseMotionAdapter {
        
        @Override
        public void mouseDragged(MouseEvent e) {
             trackball.drag(e.getX(), e.getY());
             trackball.setRotating(true);
             changed();
        }
    }
    
    class MouseWheelHandler implements MouseWheelListener {

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.getWheelRotation() < 0) { // up
                fov--;
                if (fov < 2) {
                    fov = 2;
                }
            } else { // down
                fov++;
            }
            changed();
        }
    }
}
