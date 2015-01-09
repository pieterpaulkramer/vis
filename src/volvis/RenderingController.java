/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import datatypes.RenderResult;
import gui.OpacityWeightEditor;
import gui.OpacityWeightPanel;
import gui.RaycastRendererPanel;
import gui.TransferFunctionEditor;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker.StateValue;
import util.ImageDrawer;
import util.TFChangeListener;
import volume.Volume;

/**
 *
 * @author michel
 */
public class RenderingController extends Renderer implements TFChangeListener {

    private int mode = RaycastRenderer.MIP;
    private int resolution = 1;
    private boolean trilinint = false;
    private Volume volume = null;
    
    private RaycastRendererPanel tFuncPanel;
    private TransferFunction tFunc;
    private TransferFunctionEditor tfEditor;
    
    private OpacityFunction oFunc;
    private OpacityWeightEditor owEditor;
    private OpacityWeightPanel oWeightPanel;
    
    private RenderThread threadedRenderer;

    private ImageDrawer drawer;

    public RenderingController(ImageDrawer drawer) {
        tFuncPanel = new RaycastRendererPanel(this);
        tFuncPanel.setSpeedLabel("");
        
        oWeightPanel = new OpacityWeightPanel(this);
        this.drawer = drawer;
    }

    public void setMode(int mode) {
        if(this.mode==mode)return;
        this.mode = mode;
        changed();
    }

    public void setResolution(int res) {
        if(this.resolution==res)return;
        this.resolution = res;
        changed();
    }

    public void setTriLinInt(boolean b) {
        if(this.trilinint==b)return;
        this.trilinint = b;
        changed();
    }

    public void setVolume(Volume vol) {
        volume = vol;
        
        tFunc = new TransferFunction(volume.getMinimum(), volume.getMaximum());
        tFunc.addTFChangeListener(this);
        
        tfEditor = new TransferFunctionEditor(tFunc, volume.getHistogram());
        tFuncPanel.setTransferFunctionEditor(tfEditor);
        
        oFunc = new OpacityFunction(volume.getMinimum(), volume.getMaximum());
        oFunc.addTFChangeListener(this);
        
        owEditor = new OpacityWeightEditor(oFunc, volume.getHistogram());
        oWeightPanel.setOpacityWeightEditor(owEditor);
    }

    @Override
    public void changed() {
        for (TFChangeListener listener : listeners) {
            listener.changed();
        }
    }

    public RaycastRendererPanel getTFuncPanel() {
        return tFuncPanel;
    }
    
    
    public Component getOWeightPanel() {
        return oWeightPanel;
    }

    @Override
    public void visualize(double[] viewMatrix, long renderingId) {
        stopRenderer();
        
        if (volume == null) {
            return;
        }
        
        if (resolution < 5) {
            threadedRenderer = new RenderThread(this, viewMatrix, renderingId, mode, resolution, trilinint, volume, tFunc, oFunc);
            threadedRenderer.execute();
            
            RaycastRenderer localRenderer = new RaycastRenderer(mode, 5, trilinint, volume, tFunc, oFunc);
            BufferedImage image = localRenderer.visualize(viewMatrix);
            drawer.renderingDone(new RenderResult(renderingId, image, volume, resolution));
        } else {
            RaycastRenderer localRenderer = new RaycastRenderer(mode, 5, trilinint, volume, tFunc, oFunc);
            
            long startedRendering = System.currentTimeMillis();
            BufferedImage image = localRenderer.visualize(viewMatrix);
            long renderTime = System.currentTimeMillis() - startedRendering;
            
            renderingDone(new RenderResult(renderingId, image, volume, resolution), renderTime);
        }
    }
    
    private void stopRenderer() {
        if (threadedRenderer != null && threadedRenderer.getState() == StateValue.STARTED) {
            threadedRenderer.stop();
        }
    }

    void renderingDone(RenderResult renderResult, final long renderTime) {
        drawer.renderingDone(renderResult);
        
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                tFuncPanel.setSpeedLabel(renderTime + "ms");
            }
        });
    }
}
