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
    
    private boolean stopped = false;
    
    public RenderThread(RenderingController controller, double[] viewMatrix, RenderOrder jobs, BufferedImage imageBuffer, int mode, int resolution, int intmode, Volume vol, TransferFunction tFunc, OpacityFunction oFunc, double[][][] alphas) {
        renderer = new RaycastRenderer(mode, intmode, vol, tFunc, oFunc, alphas);
        
        this.viewMatrix = viewMatrix;
        this.imageBuffer = imageBuffer;
        this.controller = controller;
        this.jobs = jobs;
    }

    @Override
    public Object doInBackground() {
        long startedRunningAt = System.currentTimeMillis();
        renderer.visualize(viewMatrix, imageBuffer, jobs);        
        long renderTime = System.currentTimeMillis() - startedRunningAt;
        
        if (!stopped) {
            controller.renderingDone(renderTime);
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