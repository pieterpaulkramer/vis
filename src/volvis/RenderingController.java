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
import java.util.ArrayList;
import java.util.List;
import javax.media.opengl.GL2;
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
    private double[] viewMatrix = new double[4 * 4];
    
    private RenderThread quickPreviewRenderer;
    private RenderThread slowPreviewRenderer;
    private RenderThread realRenderer;

    public RenderingController() {
        tFuncPanel = new RaycastRendererPanel(this);
        tFuncPanel.setSpeedLabel("0");
    }

    public void setMode(int mode) {
        this.mode = mode;
        changed();
    }

    public void setResolution(int res) {
        this.resolution = res;
        changed();
    }

    public void setTriLinInt(boolean b) {
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
    public void visualize(GL2 gl) {
        stopRenderers();
        
        if (volume == null) {
            return;
        }
        
        if (resolution < 5) {
            quickPreviewRenderer = new RenderThread(gl, mode, 5, trilinint, volume, tFunc,oFunc);
            quickPreviewRenderer.doInBackground();
        }
        if (resolution < 3) {
            slowPreviewRenderer = new RenderThread(gl, mode, 3, trilinint, volume, tFunc,oFunc);
            slowPreviewRenderer.doInBackground();
        }
        realRenderer = new RenderThread(gl, mode, resolution, trilinint, volume, tFunc,oFunc);
        realRenderer.doInBackground();
    }
    
    private void stopRenderers() {
        for (RenderThread t: new RenderThread[] {quickPreviewRenderer, slowPreviewRenderer, realRenderer}) {
            if (t != null) {
                t.stop();
            }
        }
    }

}
