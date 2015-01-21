/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import datatypes.RenderResult;
import java.awt.image.BufferedImage;
import javax.swing.SwingWorker;
import volume.Volume;

public class RenderThread extends SwingWorker {
    
    private long id;
    private RaycastRenderer renderer;
    private double[] viewMatrix;
    private int resolution;
    private Volume vol;
    private RenderingController controller;
    
    private boolean stopped = false;
    
    public RenderThread(RenderingController controller, double[] viewMatrix, long renderingId, int mode, int resolution, int intmode, Volume vol, TransferFunction tFunc, OpacityFunction oFunc, double[][][] alphas) {
        renderer = new RaycastRenderer(mode, resolution, intmode, vol, tFunc, oFunc,alphas);
        
        this.vol = vol;
        this.viewMatrix = viewMatrix;
        this.id = renderingId;
        this.resolution = resolution;
        this.controller = controller;
    }

    @Override
    public Object doInBackground() {
        long startedRunningAt = System.currentTimeMillis();
        BufferedImage image = renderer.visualize(viewMatrix);        
        long renderTime = System.currentTimeMillis() - startedRunningAt;
        
        if (image != null && !stopped) {
            controller.renderingDone(new RenderResult(id, image, vol, resolution), renderTime);
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