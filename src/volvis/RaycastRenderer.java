/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import render.interpolate.CubicInterpolator;
import render.interpolate.Grid;
import render.interpolate.Interpolator;
import render.interpolate.LinearInterpolator;
import render.interpolate.NearestNeighbourInterpolator;
import render.order.RenderOrder;
import render.order.SpiralOrder;
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

    private boolean computationRunning;

    public RaycastRenderer(int mode, int intmode, Volume vol, TransferFunction tFunc, OpacityFunction oFunc, double[][][] alphas) {
        this.mode = mode;
        this.intmode = intmode;
        this.volume = vol;
        this.tFunc = tFunc;
        this.oFunc = oFunc;
        if (alphas == null) {
            this.alphas = new double[vol.getDimX()][vol.getDimY()][vol.getDimZ()];
            computeAllAlphas();
        } else {
            this.alphas = alphas;
        }
    }

    public double[][][] getAlphas() {
        return alphas;
    }

    private void slicer(double[] viewMatrix, BufferedImage buffer, RenderOrder jobs) {
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

        // Flag that can be set to false from the outside to stop the computations
        computationRunning = true;

        // sample on a plane through the origin of the volume data
        for (int[] pix: jobs.getAllCoordinates()) {
            if (!computationRunning) {
                return;
            }
            
            double[][] ray = CastRay(uVec, pix[0], imageCenter, vVec, pix[1], volumeCenter, viewVec);
            TFColor voxelColor = computeColor(ray);

            // BufferedImage expects a pixel color packed as ARGB in an int
            int c_alpha = voxelColor.a <= 1.0 ? (int) Math.floor(voxelColor.a * 255) : 255;
            int c_red   = voxelColor.r <= 1.0 ? (int) Math.floor(voxelColor.r * 255) : 255;
            int c_green = voxelColor.g <= 1.0 ? (int) Math.floor(voxelColor.g * 255) : 255;
            int c_blue  = voxelColor.b <= 1.0 ? (int) Math.floor(voxelColor.b * 255) : 255;
            int pixelColor = (c_alpha << 24) | (c_red << 16) | (c_green << 8) | c_blue;
            
            for (int[] p : jobs.getPixelsToFill(pix)) {
                buffer.setRGB(p[0], p[1], pixelColor);
            }
        }
    }

    void stopSlicer() {
        computationRunning = false;
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

    private double[][] CastRay(double[] uVec, double i, int imageCenter, double[] vVec, double j, double[] volumeCenter, double[] viewVec) {
        int samples = (int) Math.ceil(VectorMath.length(new double[]{volume.getDimX(), volume.getDimY(), volume.getDimZ()}));
        double[][] pixelcoords = new double[samples][3];
        for (int k = -samples / 2; k < samples / 2; k++) {
            int index = k + samples / 2;
            pixelcoords[index][0] = uVec[0] * (i - imageCenter) + vVec[0] * (j - imageCenter)
                    + volumeCenter[0] + k * viewVec[0];
            pixelcoords[index][1] = uVec[1] * (i - imageCenter) + vVec[1] * (j - imageCenter)
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

    private void computeAllAlphas() {
        for (int x = 0; x < volume.getDimX(); x++) {

            for (int y = 0; y < volume.getDimY(); y++) {

                for (int z = 0; z < volume.getDimZ(); z++) {
                    alphas[x][y][z] = computeMultiAlphaLevel(x, y, z);
                }
            }
        }
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

    public void visualize(double[] viewMatrix, BufferedImage buffer, RenderOrder jobs) {
        slicer(viewMatrix, buffer, jobs);
    }
}