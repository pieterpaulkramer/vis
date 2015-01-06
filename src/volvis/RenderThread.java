/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import datatypes.RenderResult;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.SwingWorker;
import util.ImageDrawer;
import volume.Volume;

public class RenderThread extends SwingWorker {
    
    private long id;
    private RaycastRenderer renderer;
    private double[] viewMatrix;
    private int resolution;
    private Volume vol;
    private ImageDrawer drawer;
    private BufferedImage image;
    
    private boolean stopped = false;
    
    public RenderThread(ImageDrawer drawer, double[] viewMatrix, long renderingId, int mode, int resolution, boolean trilinint, Volume vol, TransferFunction tFunc, OpacityFunction oFunc) {
        renderer = new RaycastRenderer(mode, resolution, trilinint, vol, tFunc, oFunc);
        
        this.vol = vol;
        this.viewMatrix = viewMatrix;
        this.id = renderingId;
        this.resolution = resolution;
        this.drawer = drawer;
    }
    
    

    @Override
    public Object doInBackground() throws IOException {
        long t = System.currentTimeMillis();
        BufferedImage image = renderer.visualize(viewMatrix);        
        System.out.println("Execution time of renderer (resolution: "+resolution+"): "+(System.currentTimeMillis()-t)/1000.0);
        
        if (image != null && !stopped) {
            drawer.renderingDone(new RenderResult(id, image, vol, resolution));
        }
        
        return null;
    }
    
    public synchronized void stop() {
        renderer.stopSlicer();
    }
}