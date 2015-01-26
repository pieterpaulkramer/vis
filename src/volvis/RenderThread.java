/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import java.awt.image.BufferedImage;
import javax.swing.SwingWorker;
import render.order.RenderOrder;
import volume.Volume;

public class RenderThread extends SwingWorker {
    
    private RaycastRenderer renderer;
    private double[] viewMatrix;
    private RenderingController controller;
    private Image imageBuffer;
    private RenderOrder jobs;
    private double zoom;
    private double[] pan;
    
    private final Object completedLock;
    private volatile boolean stopped = false;
    
    public RenderThread(RenderingController controller, double[] viewMatrix, RenderOrder jobs, Image imageBuffer, int mode, int intmode, Volume vol, TransferFunction tFunc, OpacityFunction oFunc, SurfaceTransferFunction stfFunc, double[][][] alphas, double zoom, double[] pan) {
        renderer = new RaycastRenderer(mode, intmode, vol, tFunc, oFunc, stfFunc, alphas);
        this.zoom = zoom;
        this.viewMatrix = viewMatrix;
        this.imageBuffer = imageBuffer;
        this.controller = controller;
        this.jobs = jobs;

        this.pan = pan;

        
        completedLock = new Object();

    }

    @Override
    public Object doInBackground() {
        renderer.visualize(viewMatrix, imageBuffer, jobs, zoom,pan);        
        
        synchronized (completedLock) {
            if (!stopped) {
                controller.renderingDone();
            }
        }
        
        return null;
    }

    @Override
    protected void done() {//make thrown exceptions in threads visible
        try{
            get();
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    
    
    /**
     * Stops the renderer. It is guaranteed that when this method returns, the thread
     * will not start any new operations, such as computing the value for the next
     * pixel. Guarantees also that renderingDone() wil not be called after this
     * method returns.
     */
    public void stop() {
        renderer.stopSlicer();
        
        synchronized (completedLock) {
            stopped = true;
        }
    }
}
