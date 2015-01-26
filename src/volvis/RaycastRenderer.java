/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import render.interpolate.CubicInterpolator;
import render.interpolate.Grid;
import render.interpolate.Interpolator;
import render.interpolate.LinearInterpolator;
import render.interpolate.NearestNeighbourInterpolator;
import render.order.RenderOrder;
import render.order.Tuple;
import util.VectorMath;
import volume.Volume;

/**
 *
 * @author michel
 */
public class RaycastRenderer {

    public final static int MIP = 723623796;
    public final static int COMPOSITING = 267673489;
    public final static int OPACITYWEIGHTING = 234624534;
    public final static int SURFACES = 48494948;
    
    private final static HashMap<Integer, Interpolator> INTERPOLATORS = new HashMap<Integer, Interpolator>();

    static {
        INTERPOLATORS.put(Interpolator.NEARESTNEIGHBOUR, new NearestNeighbourInterpolator());
        INTERPOLATORS.put(Interpolator.LINEAR, new LinearInterpolator());
        INTERPOLATORS.put(Interpolator.CUBIC, new CubicInterpolator());
    }
    private int intmode;
    private int mode;
    private Volume volume;
    private TransferFunction tFunc;
    private OpacityFunction oFunc;
    private final SurfaceTransferFunction stfFunc;
    private double[][][] alphas;
    
    private final Object computationRunningLock;
    private volatile boolean computationRunning;

    public RaycastRenderer(int mode, int intmode, Volume vol, TransferFunction tFunc, OpacityFunction oFunc, SurfaceTransferFunction stfFunc, double[][][] alphas) {
        this.mode = mode;
        this.intmode = intmode;
        this.volume = vol;
        this.tFunc = tFunc;
        this.oFunc = oFunc;
        this.stfFunc = stfFunc;
        this.alphas = alphas;

        // Flag that can be set to false from the outside to stop the computations
        computationRunning = true;
        computationRunningLock = new Object();
    }

    public void visualize(double[] viewMatrix, Image buffer, RenderOrder jobs, double zoom, double[] pan) {

        // vector uVec and vVec define a plane through the origin, 
        // perpendicular to the view vector viewVec
        double[] viewVec = new double[3];
        double[] uVec = new double[3];
        double[] vVec = new double[3];
        VectorMath.setVector(viewVec, viewMatrix[2], viewMatrix[6], viewMatrix[10]);
        VectorMath.setVector(uVec, viewMatrix[0], viewMatrix[4], viewMatrix[8]);
        VectorMath.setVector(vVec, viewMatrix[1], viewMatrix[5], viewMatrix[9]);
        // image is square
        int imageCenter = buffer.getWidth() / 2;
        double[] volumeCenter = new double[3];
        VectorMath.setVector(volumeCenter, volume.getDimX() / 2, volume.getDimY() / 2, volume.getDimZ() / 2);

        vVec = VectorMath.multiply(vVec, 1 / zoom);
        uVec = VectorMath.multiply(uVec, 1 / zoom);
        // sample on a plane through the origin of the volume data
        for (Tuple<int[], Integer> pix : jobs.getAllCoordinates()) {

            Ray ray = CastRay(uVec, pix.o1[0], imageCenter, vVec, pix.o1[1], volumeCenter, viewVec, pan);
            computeColor(ray);
            synchronized (computationRunningLock) {
                if (computationRunning) {
                    for (int[] p : RenderOrder.getPixelsToFill(pix)) {
                        buffer.setRay(p[0], p[1], ray);
                    }
                } else {
                    return;
                }
            }
        }
    }

    private TFColor computeColor(Ray ray) {
        switch (mode) {
            case (MIP): {
                int max = 0;
                int idx = 0;
                for (int i = 0; i < ray.getCoordinates().size(); i++) {
                    int v = getVoxel(ray.getCoordinates().get(i));
                    if (max < v) {
                        idx = i;
                        max = v;
                    }
                }
                for (int i = 0; i < ray.getCoordinates().size(); i++) {
                    ray.addRenderResult(i == idx ? tFunc.getColor(max) : new TFColor());
                }
                break;

            }
            case (COMPOSITING): {
                for (double[] coord : ray) {
                    TFColor cur = tFunc.getColor(getVoxel(coord));
                    ray.addRenderResult(cur);
                }
                break;
            }
            case (OPACITYWEIGHTING): {
                for (double[] coord : ray) {
                    TFColor cur = tFunc.getColor(getVoxel(coord));
                    double a = getAlpha(coord);
                    cur.a = a;
                    ray.addRenderResult(cur);
                }
                break;
            }
        }
        return ray.getResult();
    }

    /**
     * Stops the renderer. It is guaranteed that the renderer does not modify
     * any pixels in the image buffer after this method returns.
     */
    public void stopSlicer() {
        synchronized (computationRunningLock) {
            computationRunning = false;
        }
    }

    // get a voxel from the volume data by nearest neighbor interpolation
    private short getVoxel(double[] coord) {

        Grid g = new Grid() {
            @Override
            public double getValue(int x, int y, int z) {
                if ((x >= 0) && (x < volume.getDimX()) && (y >= 0) && (y < volume.getDimY()) && (z >= 0) && (z < volume.getDimZ())) {
                    return volume.getVoxel(x, y, z);
                } else {
                    return 0;
                }
            }
        };
        Interpolator i = INTERPOLATORS.get(intmode);
        i.setGrid(g);
        short x = (short) i.getValue(coord[0], coord[1], coord[2]);
        short m = (short) tFunc.getMaximum();
        return x < 0 ? 0 : (x > m ? m : x);
    }

    private short getVoxel(double x, double y, double z) {
        return getVoxel(new double[]{x, y, z});
    }

    private Ray CastRay(double[] uVec, double i, int imageCenter, double[] vVec, double j, double[] volumeCenter, double[] viewVec, double[] pan) {
        return new Ray(volume, uVec, vVec, viewVec, pan, i, j, imageCenter, volumeCenter);
    }

    private double[] lGradientVector(double x, double y, double z) {
        // TODO dit op een handige manier interpoleren
        
        return new double[]{
            0.5d * (getVoxel(x + 1, y, z) - getVoxel(x - 1, y, z)),
            0.5d * (getVoxel(x, y + 1, z) - getVoxel(x, y - 1, z)),
            0.5d * (getVoxel(x, y, z + 1) - getVoxel(x, y, z - 1))
        };
    }
    
    private double getGradient(double[] coord) {
        return getGradient(coord[0], coord[1], coord[2]);
    }
    
    private double getGradient(double x, double y, double z) {
        double[] vec = lGradientVector(x, y, z);
        
        return Math.sqrt(Math.pow(vec[0], 2) + Math.pow(vec[1], 2) + Math.pow(vec[2], 2));
    }

    private double getAlpha(double[] coord) {
        return getAlpha(coord[0], coord[1], coord[2]);
    }

    private double getAlpha(double x, double y, double z) {
        Grid g = new Grid() {
            @Override
            public double getValue(int x, int y, int z) {
                if ((x >= 0) && (x < volume.getDimX()) && (y >= 0) && (y < volume.getDimY()) && (z >= 0) && (z < volume.getDimZ())) {
                    return alphas[x][y][z];
                }
                return 0;
            }
        };
        Interpolator i = INTERPOLATORS.get(intmode);
        i.setGrid(g);
        double a = i.getValue(x, y, z);
        return a < 0 ? 0 : (a > 1 ? 1 : a);
    }

    public double[][][] computeAllAlphas() {
        double[][][] alphas = new double[volume.getDimX()][volume.getDimY()][volume.getDimZ()];

        for (int x = 0; x < volume.getDimX(); x++) {
            for (int y = 0; y < volume.getDimY(); y++) {
                for (int z = 0; z < volume.getDimZ(); z++) {
                    alphas[x][y][z] = computeMultiRBAlphaLevel(x, y, z);
                }
            }
        }

        return alphas;
    }

    private double computeMultiISOAlphaLevel(int x, int y, int z) {
        double res = 1;
        for (int i = 0; i < oFunc.getControlPoints().size(); i++) {
            res *= (1 - computeSingleIsoAlphaLevel(x, y, z, oFunc.getControlPoints().get(i).width, oFunc.getControlPoints().get(i).value, oFunc.getControlPoints().get(i).alphafactor));
        }
        return 1 - res;
    }

    private double computeSingleIsoAlphaLevel(double x, double y, double z, double r, int fv, double fac) {
        if (fac == 0) {
            return 0;
        }
        double[] grad = lGradientVector(x, y, z);

        double gradl = VectorMath.length(grad);
        //gradl = 1;
        int val = getVoxel(x, y, z);
        if (val == fv && gradl == 0) {
            return fac;
        } else if (gradl > 0 && val - r * gradl <= fv && val + r * gradl >= fv) {
            return fac * (1d - ((1d / r) * (Math.abs(fv - val) / gradl)));
        } else {
            return 0;
        }
    }

    private double computeMultiRBAlphaLevel(double x, double y, double z) {
        short val = getVoxel(x, y, z);
        double[] grad = lGradientVector(x, y, z);
        double lgrad = VectorMath.length(grad);
        double av1, av2;
        double v1, v2;
        ArrayList<OpacityFunction.ControlPoint> cps = oFunc.getControlPoints();
        int i = 0;
        while (i < cps.size() && cps.get(i).value < val) {
            i++;
        }
        i--;
        if (i == -1) {
            return 0;
        }
        av1 = cps.get(i).alphafactor;
        av2 = cps.get(i + 1).alphafactor;
        v1 = cps.get(i).value;
        v2 = cps.get(i + 1).value;
        if (v1 > val) {
            throw new RuntimeException();
        }
        if (v2 < val) {
            throw new RuntimeException();
        }
        double alphabase = av2 * ((val - v1) / (v2 - v1)) + av1 * ((v2 - val) / (v2 - v1));
        return Math.min(1, alphabase * lgrad);
    }

}
