/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import javax.media.opengl.GL2;
import javax.swing.SwingWorker;
import volume.Volume;

public class RenderThread extends SwingWorker {
    
    private RaycastRenderer renderer;
    private GL2 gl;
    
    
    private final int resolution; // TODO remove
    
    public RenderThread(GL2 gl, int mode, int resolution, boolean trilinint, Volume vol, TransferFunction tFunc, OpacityFunction oFunc) {
        renderer = new RaycastRenderer(mode, resolution, trilinint, vol, tFunc,oFunc);
        this.gl = gl;
        
        this.resolution = resolution;
    }

    @Override
    public Object doInBackground() {
        //System.out.println("Starting computation for renderer"+resolution);
        renderer.visualize(gl);
        //System.out.println("Done with computation for renderer"+resolution);
        
        return null;
    }
    
    public void stop() {
        //System.out.println("Stopping computation for renderer"+resolution);
        renderer.stopSlicer();
    }
    
}