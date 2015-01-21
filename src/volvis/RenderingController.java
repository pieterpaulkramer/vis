/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import datatypes.Interpolator;
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
    private int intmode = Interpolator.NEARESTNEIGHBOUR;
    private Volume volume = null;
    
    private RaycastRendererPanel tFuncPanel;
    private TransferFunction tFunc;
    private TransferFunctionEditor tfEditor;
    
    private OpacityFunction oFunc;
    private OpacityWeightEditor owEditor;
    private OpacityWeightPanel oWeightPanel;
    
    private RenderThread threadedRenderer;
    private double[][][] maintainedAlphas;

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

    public void SetIntMode(int b) {
        if(this.intmode==b)return;
        this.intmode = b;
        System.out.println(b);
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
        oFunc.addOpChangeListener(this);
        
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
    public RenderResult visualize(double[] viewMatrix, long renderingId) {
        stopRenderer();
        
        if (volume == null) {
            return null;
        }
        
        if (resolution < 5) {
            threadedRenderer = new RenderThread(this, viewMatrix, renderingId, mode, resolution, intmode, volume, tFunc, oFunc,maintainedAlphas);
            threadedRenderer.execute();
            
            RaycastRenderer localRenderer = new RaycastRenderer(mode, 5, intmode, volume, tFunc, oFunc,maintainedAlphas);
            BufferedImage image = localRenderer.visualize(viewMatrix);
            return new RenderResult(renderingId, image, volume, 5);
        } else {
            RaycastRenderer localRenderer = new RaycastRenderer(mode, 5, intmode, volume, tFunc, oFunc,maintainedAlphas);
            
            long startedRendering = System.currentTimeMillis();
            BufferedImage image = localRenderer.visualize(viewMatrix);
            long renderTime = System.currentTimeMillis() - startedRendering;
            
            updateRenderTimeLabel(renderTime);
            
            return new RenderResult(renderingId, image, volume, resolution);
        }
    }
    
    private void stopRenderer() {
        if (threadedRenderer != null && threadedRenderer.getState() == StateValue.STARTED) {
            threadedRenderer.stop();
        }
    }

    void renderingDone(RenderResult renderResult, final long renderTime) {
        drawer.renderingDone(renderResult);
        updateRenderTimeLabel(renderTime);
    }
    
    void updateRenderTimeLabel(final long renderTime) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                tFuncPanel.setSpeedLabel(renderTime + "ms");
            }
        });
    }

    void ochanged() {
        this.maintainedAlphas = new RaycastRenderer(mode, 1, intmode, volume, tFunc, oFunc,null).getAlphas();
    }
}
