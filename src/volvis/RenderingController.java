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
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker.StateValue;
import render.interpolate.Interpolator;
import render.order.CombinedOrder;
import render.order.SpiralOrder;
import util.TFChangeListener;
import volume.Volume;

/**
 *
 * @author michel
 */
public class RenderingController extends Renderer implements TFChangeListener {
    
    // The actual amount of threads used to perform the visualization is the
    // square of N_THREADS_SQ_ROOT
    private final static int N_THREADS_SQ_ROOT = 2;

    private int mode = RaycastRenderer.MIP;
    private int intmode = Interpolator.NEARESTNEIGHBOUR;
    private Volume volume = null;
    private double[][][] maintainedAlphas;
    
    private RaycastRendererPanel tFuncPanel;
    private TransferFunction tFunc;
    private TransferFunctionEditor tfEditor;
    
    private OpacityFunction oFunc;
    private OpacityWeightEditor owEditor;
    private OpacityWeightPanel oWeightPanel;
    
    private RenderThread[] threadedRenderers;
    private int n_threads_done;
    private long startedRunningAt;
    
    private BufferedImage imageBuffer;

    public RenderingController() {
        tFuncPanel = new RaycastRendererPanel(this);
        tFuncPanel.setSpeedLabel("");
        
        oWeightPanel = new OpacityWeightPanel(this);
        
        threadedRenderers = new RenderThread[N_THREADS_SQ_ROOT * N_THREADS_SQ_ROOT];
        n_threads_done = N_THREADS_SQ_ROOT * N_THREADS_SQ_ROOT;
    }

    public void setMode(int mode) {
        if(this.mode==mode)return;
        this.mode = mode;
        changed();
    }

    public void setResolution(int res) {
        // No longer relevant
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
        changed();
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
        
        clearBuffer();
        
        startedRunningAt = System.currentTimeMillis();
        n_threads_done = 0;
        
        List<CombinedOrder<SpiralOrder>> threadsJobs = SpiralOrder.getThreadedOrders(N_THREADS_SQ_ROOT, imageBuffer.getWidth());
        for (int i=0; i<N_THREADS_SQ_ROOT*N_THREADS_SQ_ROOT; i++) {
            threadedRenderers[i] = new RenderThread(this, viewMatrix, threadsJobs.get(i), imageBuffer, mode, intmode, volume, tFunc, oFunc, maintainedAlphas);
            threadedRenderers[i].execute();
        }
    }
    
    private void stopRenderer() {
        for (RenderThread tr: threadedRenderers) {
            if (tr != null && tr.getState() == StateValue.STARTED) {
                tr.stop();
                n_threads_done += 1;
            }
        }
    }

    void renderingDone() {
        n_threads_done += 1;
        
        if (done()) {
            long renderTime = System.currentTimeMillis() - startedRunningAt;
            updateRenderTimeLabel(renderTime);
        }
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
        this.maintainedAlphas = new RaycastRenderer(mode, intmode, volume, tFunc, oFunc,null).getAlphas();
        
    }

    @Override
    public BufferedImage rendering() {
        return imageBuffer;
    }

    @Override
    public boolean done() {
        return n_threads_done == threadedRenderers.length;
    }

    @Override
    public Volume getVolume() {
        return volume;
    }
    
    private void clearBuffer() {
        for (int x=0; x<imageBuffer.getWidth(); x++) {
            for (int y=0; y<imageBuffer.getHeight(); y++) {
                imageBuffer.setRGB(x, y, 0);
            }
        }
    }
}
