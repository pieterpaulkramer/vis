/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import gui.OpacityWeightEditor;
import gui.OpacityWeightPanel;
import gui.RaycastRendererPanel;
import gui.SurfaceTFEditor;
import gui.SurfaceTFPanel;
import gui.TransferFunctionEditor;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.SwingUtilities;
import render.interpolate.Interpolator;
import render.order.CombinedOrder;
import render.order.DirectedSequentialOrder;
import render.order.RenderOrder;
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
    private final static int N_THREADS_SQ_ROOT = (int) Math.ceil(Math.sqrt(Runtime.getRuntime().availableProcessors()));

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
    
    // private SurfaceTransferFunction TODO;
    private SurfaceTFEditor stfEditor;
    private SurfaceTFPanel stfPanel;
    
    private RenderThread[] threadedRenderers;
    private volatile int n_threads_done;
    private long startedRunningAt;
    
    private BufferedImage imageBuffer;

    public RenderingController() {
        tFuncPanel = new RaycastRendererPanel(this);
        tFuncPanel.setSpeedLabel("");
        
        oWeightPanel = new OpacityWeightPanel(this);
        
        stfPanel = new SurfaceTFPanel(this);
        
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
        
        SurfaceTransferFunction stfFunc = new SurfaceTransferFunction(volume.getMaximumGradient(), volume.getMaximum());
        stfFunc.addTFChangeListener(this);
        stfEditor = new SurfaceTFEditor(stfFunc);
        stfEditor.setGradientIntenstityPlot(volume.getSurfacesPlot(100));
        stfPanel.setSTFEditor(stfEditor);
        
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
    
    public Component getSurfaceTFPanel() {
        return stfPanel;
    }

    @Override
    public void visualize(double[] viewMatrix, double zoom,double[] pan) {
        stopRendering();
        
        if (volume == null) {
            return;
        }
        
        clearBuffer();
        
        startedRunningAt = System.currentTimeMillis();
        n_threads_done = 0;
        
        List threadsJobs = DirectedSequentialOrder.getThreadedOrders(N_THREADS_SQ_ROOT, imageBuffer.getWidth());
        for (int i=0; i<threadsJobs.size(); i++) {
            threadedRenderers[i] = new RenderThread(this, viewMatrix, (RenderOrder)threadsJobs.get(i), imageBuffer, mode, intmode, volume, tFunc, oFunc, maintainedAlphas,zoom,pan);
            threadedRenderers[i].execute();
        }
    }
    
    private void stopRendering() {
        for (RenderThread tr: threadedRenderers) {
            if (tr != null) {
                tr.stop();
            }
        }
        
        n_threads_done = threadedRenderers.length;
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
        this.maintainedAlphas = new RaycastRenderer(mode, intmode, volume, tFunc, oFunc, null).computeAllAlphas();
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
                if (mode == RaycastRenderer.MIP) {
                    // MIP renders background to black (identity element for max is zero), so make the background black as well
                    imageBuffer.setRGB(x, y, 0);
                } else {
                    // Other methods render background to white (identity for multiplication is one) so make the background white
                    imageBuffer.setRGB(x, y, (255<<24) + (255<<16) + (255 << 8) + 255);
                }
            }
        }
    }
}
