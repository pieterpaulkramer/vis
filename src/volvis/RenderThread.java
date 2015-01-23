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
    private BufferedImage imageBuffer;
    private RenderOrder jobs;
    private double zoom;
    private double[] pan;
    
    private boolean stopped = false;
    
    public RenderThread(RenderingController controller, double[] viewMatrix, RenderOrder jobs, BufferedImage imageBuffer, int mode, int intmode, Volume vol, TransferFunction tFunc, OpacityFunction oFunc, double[][][] alphas, double zoom, double[] pan) {
        renderer = new RaycastRenderer(mode, intmode, vol, tFunc, oFunc, alphas);
        this.zoom = zoom;
        this.viewMatrix = viewMatrix;
        this.imageBuffer = imageBuffer;
        this.controller = controller;
        this.jobs = jobs;
        this.pan = pan;
    }

    @Override
    public Object doInBackground() {
        renderer.visualize(viewMatrix, imageBuffer, jobs, zoom,pan);        
        
        if (!stopped) {
            controller.renderingDone();
        }
        
        return null;
    }
    
    /**
     * Stops the renderer. It is guaranteed that when this method returns, the thread
     * will not start any new operations, such as computing the value for the next
     * pixel
     */
    public void stop() {
        renderer.stopSlicer();
        stopped = true;
    }
}