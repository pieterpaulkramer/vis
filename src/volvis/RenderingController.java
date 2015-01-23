/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import gui.OpacityWeightEditor;
import gui.OpacityWeightPanel;
import gui.RaycastRendererPanel;
import gui.TransferFunctionEditor;
import java.awt.Component;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker.StateValue;
import render.interpolate.Interpolator;
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
    
    private boolean done;
    private BufferedImage imageBuffer;

    public RenderingController() {
        tFuncPanel = new RaycastRendererPanel(this);
        tFuncPanel.setSpeedLabel("");
        
        oWeightPanel = new OpacityWeightPanel(this);
        
        done = true;
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
        
        // set up image for storing the resulting rendering
        // the image width and height are equal to the length of the volume diagonal
        int imageSize = (int) Math.floor(Math.sqrt(vol.getDimX() * vol.getDimX() + vol.getDimY() * vol.getDimY() + vol.getDimZ() * vol.getDimZ()));
        if (imageSize % 2 != 0) {
            imageSize = imageSize + 1;
        }
        
        imageBuffer = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
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
    public void visualize(double[] viewMatrix) {
        stopRenderer();
        
        if (volume == null) {
            return;
        }
        
        if (resolution < 5) {
            threadedRenderer = new RenderThread(this, viewMatrix, imageBuffer, mode, resolution, intmode, volume, tFunc, oFunc,maintainedAlphas);
            threadedRenderer.execute();
            done = false;
            
            RaycastRenderer localRenderer = new RaycastRenderer(mode, 5, intmode, volume, tFunc, oFunc,maintainedAlphas);
            localRenderer.visualize(viewMatrix, imageBuffer);
        } else {
            RaycastRenderer localRenderer = new RaycastRenderer(mode, 5, intmode, volume, tFunc, oFunc,maintainedAlphas);
            
            long startedRendering = System.currentTimeMillis();
            localRenderer.visualize(viewMatrix, imageBuffer);
            long renderTime = System.currentTimeMillis() - startedRendering;
            
            updateRenderTimeLabel(renderTime);
        }
    }
    
    private void stopRenderer() {
        if (threadedRenderer != null && threadedRenderer.getState() == StateValue.STARTED) {
            threadedRenderer.stop();
            done = true;
        }
    }

    void renderingDone(final long renderTime) {
        done = true;
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

    @Override
    public BufferedImage rendering() {
        return imageBuffer;
    }

    @Override
    public boolean done() {
        return done;
    }

    @Override
    public Volume getVolume() {
        return volume;
    }
}
