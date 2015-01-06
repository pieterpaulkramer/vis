/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.media.opengl.GL2;
import javax.swing.SwingWorker;
import util.ImageDrawer;
import volume.Volume;

public class RenderThread extends SwingWorker {
    
    private RaycastRenderer renderer;
    private GL2 gl;
    private int resolution;
    private Volume vol;
    private ImageDrawer drawer;
    private BufferedImage image;
    
    private boolean stopped = false;
    
    public RenderThread(ImageDrawer drawer, GL2 gl, int mode, int resolution, boolean trilinint, Volume vol, TransferFunction tFunc, OpacityFunction oFunc) {
        renderer = new RaycastRenderer(mode, resolution, trilinint, vol, tFunc, oFunc);
        
        this.vol = vol;
        this.gl = gl;
        this.resolution = resolution;
        this.drawer = drawer;
    }
    
    

    @Override
    public Object doInBackground() throws IOException {
        System.out.println("Running renderer for thread "+resolution);
        
        double[] viewMatrix = new double[4 * 4];
        gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, viewMatrix, 0);
        image = renderer.visualize(viewMatrix);        
        return null;
    }

    @Override
    protected void done() {
        if (image != null && !stopped) {
            String filename = "image" + System.currentTimeMillis()+".jpg";
            File f = new File(filename);
            try {
                ImageIO.write(image, "jpg", f);
            } catch (IOException ex) {
                Logger.getLogger(RenderThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            System.out.println("Drawing renderer for thread "+resolution);
            drawer.draw(gl, image, vol);
        }
        System.out.println("Finished renderer for thread "+resolution);
    }
    
    
    
    public void stop() {
        System.out.println("Stopping renderer for thread "+resolution);
        renderer.stopSlicer();
        
        stopped = true;
    }
    
}