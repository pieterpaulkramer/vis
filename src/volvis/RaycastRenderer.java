/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import java.awt.image.BufferedImage;
import java.util.Arrays;
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
    private double[][][] alphas;

    private final Object computationRunningLock;
    private volatile boolean computationRunning;

    public RaycastRenderer(int mode, int intmode, Volume vol, TransferFunction tFunc, OpacityFunction oFunc, double[][][] alphas) {
        this.mode = mode;
        this.intmode = intmode;
        this.volume = vol;
        this.tFunc = tFunc;
        this.oFunc = oFunc;
        this.alphas = alphas;
        
        // Flag that can be set to false from the outside to stop the computations
        computationRunning = true;
        computationRunningLock = new Object();
    }
    
    
    public void visualize(double[] viewMatrix, BufferedImage buffer, RenderOrder jobs, double zoom,double[] pan) {

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
        System.out.println(zoom);
        double[] volumeCenter = new double[3];
        VectorMath.setVector(volumeCenter, volume.getDimX() / 2, volume.getDimY() / 2, volume.getDimZ() / 2);

        vVec = VectorMath.multiply(vVec, 1/zoom);
        uVec = VectorMath.multiply(uVec, 1/zoom);
        // sample on a plane through the origin of the volume data
        for (Tuple<int[],Integer> pix: jobs.getAllCoordinates()) {

            double[][] ray = CastRay(uVec, pix.o1[0], imageCenter, vVec, pix.o1[1], volumeCenter, viewVec,pan);

            TFColor voxelColor = computeColor(ray);

            // BufferedImage expects a pixel color packed as ARGB in an int
            int c_alpha = voxelColor.a <= 1.0 ? (int) Math.floor(voxelColor.a * 255) : 255;
            int c_red   = voxelColor.r <= 1.0 ? (int) Math.floor(voxelColor.r * 255) : 255;
            int c_green = voxelColor.g <= 1.0 ? (int) Math.floor(voxelColor.g * 255) : 255;
            int c_blue  = voxelColor.b <= 1.0 ? (int) Math.floor(voxelColor.b * 255) : 255;
            int pixelColor = (c_alpha << 24) | (c_red << 16) | (c_green << 8) | c_blue;
            
            synchronized (computationRunningLock) {
                if (computationRunning) {
                    for (int[] p : RenderOrder.getPixelsToFill(pix)) {
                        buffer.setRGB(p[0], p[1], pixelColor);
                    }
                } else {
                    return;
                }
            }
        }
    }

    /**
     * Stops the renderer. It is guaranteed that the renderer does not modify any
     * pixels in the image buffer after this method returns.
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

    private double[][] CastRay(double[] uVec, double i, int imageCenter, double[] vVec, double j, double[] volumeCenter, double[] viewVec, double[] pan) {
        int samples = (int) Math.ceil(VectorMath.length(new double[]{volume.getDimX(), volume.getDimY(), volume.getDimZ()}));
        double[][] pixelcoords = new double[samples][3];
        for (int k = -samples / 2; k < samples / 2; k++) {
            int index = k + samples / 2;
            pixelcoords[index][0] = uVec[0] * (i - imageCenter-pan[0]) + vVec[0] * (j - imageCenter-pan[1])
                    + volumeCenter[0] + k * viewVec[0];
            pixelcoords[index][1] = uVec[1] * (i - imageCenter-pan[0]) + vVec[1] * (j - imageCenter-pan[1])
                    + volumeCenter[1] + k * viewVec[1];
            pixelcoords[index][2] = uVec[2] * (i - imageCenter) + vVec[2] * (j - imageCenter)
                    + volumeCenter[2] + k * viewVec[2];
        }
        return pixelcoords;
    }

    private double[] lGradientVector(double x, double y, double z) {
        return new double[]{
            0.5d * (getVoxel(x + 1, y, z) - getVoxel(x - 1, y, z)),
            0.5d * (getVoxel(x, y + 1, z) - getVoxel(x, y - 1, z)),
            0.5d * (getVoxel(x, y, z + 1) - getVoxel(x, y, z - 1))
        };
    }

    private double computeSingleAlphaLevel(double x, double y, double z, double r, int fv, double fac) {
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
                    alphas[x][y][z] = computeMultiAlphaLevel(x, y, z);
                }
            }
        }
        
        return alphas;
    }
    
    public double[][][][] computeAllGradients() {
        double[][][][] gradients = new double[volume.getDimX()][volume.getDimY()][volume.getDimZ()][3];
        
        for (int x = 0; x < volume.getDimX(); x++) {
            for (int y = 0; y < volume.getDimY(); y++) {
                for (int z = 0; z < volume.getDimZ(); z++) {
                    gradients[x][y][z] = new double[]{
                        0.5d * (volume.getVoxel(x + 1, y, z, true) - volume.getVoxel(x - 1, y, z, true)),
                        0.5d * (volume.getVoxel(x, y + 1, z, true) - volume.getVoxel(x, y - 1, z, true)),
                        0.5d * (volume.getVoxel(x, y, z + 1, true) - volume.getVoxel(x, y, z - 1, true))
                    };
                }
            }
        }
        
        return gradients;
    }

    private double computeMultiAlphaLevel(int x, int y, int z) {
        double res = 1;
        for (int i = 0; i < oFunc.getControlPoints().size(); i++) {
            res *= (1 - computeSingleAlphaLevel(x, y, z, oFunc.getControlPoints().get(i).width, oFunc.getControlPoints().get(i).value, oFunc.getControlPoints().get(i).alphafactor));
        }
        return 1 - res;
    }

    private TFColor computeColor(double[][] ray) {
        switch (mode) {
            case (MIP): {
                int max = 0;

                for (double[] coord : ray) {
                    max = Math.max(max, getVoxel(coord));
                }
                return tFunc.getColor(max);
            }
            case (COMPOSITING): {
                TFColor color = new TFColor();
                for (double[] coord : ray) {
                    TFColor cur = tFunc.getColor(getVoxel(coord));
                    color.layer(cur);
                }
                return color;
            }
            case (OPACITYWEIGHTING): {
                TFColor color = new TFColor();
                for (double[] coord : ray) {
                    TFColor cur = tFunc.getColor(getVoxel(coord));
                    double a = getAlpha(coord);
                    cur.a = a;
                    color.layer(cur);
                }
                return color;
            }
            default: {
                return tFunc.getColor(0);
            }
        }
    }

}