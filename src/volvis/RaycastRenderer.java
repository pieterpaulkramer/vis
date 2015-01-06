/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import java.awt.image.BufferedImage;
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

    private int mode;
    private int resolution;
    private boolean trilinint;
    private Volume volume;
    private TransferFunction tFunc;
    private OpacityFunction oFunc;
    private BufferedImage image;
    private boolean computationRunning;

    public RaycastRenderer(int mode, int resolution, boolean trilinint, Volume vol, TransferFunction tFunc, OpacityFunction oFunc) {
        this.mode = mode;
        this.resolution = resolution;
        this.trilinint = trilinint;
        this.volume = vol;
        this.tFunc = tFunc;
        this.oFunc = oFunc;

        // set up image for storing the resulting rendering
        // the image width and height are equal to the length of the volume diagonal
        int imageSize = (int) Math.floor(Math.sqrt(vol.getDimX() * vol.getDimX() + vol.getDimY() * vol.getDimY() + vol.getDimZ() * vol.getDimZ()));
        if (imageSize % 2 != 0) {
            imageSize = imageSize + 1;
        }
        
        image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
    }

    private boolean slicer(double[] viewMatrix) {
        // vector uVec and vVec define a plane through the origin, 
        // perpendicular to the view vector viewVec
        double[] viewVec = new double[3];
        double[] uVec = new double[3];
        double[] vVec = new double[3];
        VectorMath.setVector(viewVec, viewMatrix[2], viewMatrix[6], viewMatrix[10]);
        VectorMath.setVector(uVec, viewMatrix[0], viewMatrix[4], viewMatrix[8]);
        VectorMath.setVector(vVec, viewMatrix[1], viewMatrix[5], viewMatrix[9]);

        // image is square
        int imageCenter = image.getWidth() / 2;

        double[] volumeCenter = new double[3];
        VectorMath.setVector(volumeCenter, volume.getDimX() / 2, volume.getDimY() / 2, volume.getDimZ() / 2);
        
        // Flag that can be set to false from the outside to stop the computations
        computationRunning = true;

        // sample on a plane through the origin of the volume data
        for (int j = 0; j < image.getHeight() / resolution; j++) {
            for (int i = 0; i < image.getWidth() / resolution; i++) {
                if (!computationRunning) {
                    return false;
                }
                
                int castx = i * resolution + (resolution - 1) / 2;
                int casty = j * resolution + (resolution - 1) / 2;
                double[][] ray = CastRay(uVec, castx, imageCenter, vVec, casty, volumeCenter, viewVec);
                TFColor voxelColor = computeColor(ray);

                // BufferedImage expects a pixel color packed as ARGB in an int
                int c_alpha = voxelColor.a <= 1.0 ? (int) Math.floor(voxelColor.a * 255) : 255;
                int c_red = voxelColor.r <= 1.0 ? (int) Math.floor(voxelColor.r * 255) : 255;
                int c_green = voxelColor.g <= 1.0 ? (int) Math.floor(voxelColor.g * 255) : 255;
                int c_blue = voxelColor.b <= 1.0 ? (int) Math.floor(voxelColor.b * 255) : 255;
                int pixelColor = (c_alpha << 24) | (c_red << 16) | (c_green << 8) | c_blue;
                for (int r = 0; r < resolution; r++) {
                    for (int r2 = 0; r2 < resolution; r2++) {
                        image.setRGB(i * resolution + r, j * resolution + r2, pixelColor);
                    }
                }
            }
        }

        return true;
    }
    
    void stopSlicer() {
        computationRunning = false;
    }
    
        // get a voxel from the volume data by nearest neighbor interpolation
    private short getVoxel(double[] coord) {
        if (trilinint) {// for now there is no noticeable diffrence between NN and TL inerpolation, so we choos NN due to it being faster.
            int x = (int) Math.round(coord[0]);
            int y = (int) Math.round(coord[1]);
            int z = (int) Math.round(coord[2]);

            if ((x >= 0) && (x < volume.getDimX()) && (y >= 0) && (y < volume.getDimY()) && (z >= 0) && (z < volume.getDimZ())) {
                return volume.getVoxel(x, y, z);
            } else {
                return 0;
            }
        } else {//trilinear interpolation
            int x1 = (int) Math.floor(coord[0]);
            int y1 = (int) Math.floor(coord[1]);
            int z1 = (int) Math.floor(coord[2]);

            if ((x1 >= 0) && (x1 < volume.getDimX() - 1) && (y1 >= 0) && (y1 < volume.getDimY() - 1) && (z1 >= 0) && (z1 < volume.getDimZ() - 1)) {
                double xf = coord[0] - x1;
                double yf = coord[1] - y1;
                double zf = coord[2] - z1;
                double value = 0;
                value += volume.getVoxel(x1, y1, z1) * (1d - xf) * (1d - yf) * (1d - zf);
                value += volume.getVoxel(x1 + 1, y1, z1) * (xf) * (1d - yf) * (1d - zf);
                value += volume.getVoxel(x1, y1 + 1, z1) * (1d - xf) * (yf) * (1d - zf);
                value += volume.getVoxel(x1 + 1, y1 + 1, z1) * (xf) * (yf) * (1d - zf);
                value += volume.getVoxel(x1, y1, z1 + 1) * (1d - xf) * (1d - yf) * (zf);
                value += volume.getVoxel(x1 + 1, y1, z1 + 1) * (xf) * (1d - yf) * (zf);
                value += volume.getVoxel(x1, y1 + 1, z1 + 1) * (1d - xf) * (yf) * (zf);
                value += volume.getVoxel(x1 + 1, y1 + 1, z1 + 1) * (xf) * (yf) * (zf);
                return (short) Math.round(value);
            }

            return 0;
        }
    }

    private short getVoxel(double x, double y, double z) {
        return getVoxel(new double[]{x, y, z});
    }

    private double[][] CastRay(double[] uVec, int i, int imageCenter, double[] vVec, int j, double[] volumeCenter, double[] viewVec) {
        int samples = 256;
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

    private double computeSingleAlphaLevel(double x, double y, double z, int r, int fv) {
        double[] grad = lGradientVector(x, y, z);
        double gradl = VectorMath.length(grad);
        int val = getVoxel(x, y, z);
        if (val == fv && gradl == 0) {
            return 1;
        } else if (gradl > 0 && val - r * gradl <= fv && val + r * gradl >= fv) {
            return 1d - (1d / r) * (Math.abs(fv - val) / gradl);
        } else {
            return 0;
        }
    }

    
    private double computeMultiAlphaLevel(double x, double y, double z) {
        double res = 1;
        for(int i = 0; i < oFunc.getControlPoints().size();i++) {
            res *= (1-computeSingleAlphaLevel(x, y, z, oFunc.getControlPoints().get(i).value, oFunc.getControlPoints().get(i).width));
        }
        return 1-res;
    }
    
    private double computeMultiAlphaLevel(double[] xyz)
    {
        return computeMultiAlphaLevel(xyz[0],xyz[1],xyz[2]);
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
                for (double[] coord:ray) {
                    TFColor cur = tFunc.getColor(getVoxel(coord));
                    color.layer(cur);
                }
                return color;
            }
            case (OPACITYWEIGHTING): {
               TFColor color = new TFColor();
                for (double[] coord:ray) {
                    TFColor cur = tFunc.getColor(getVoxel(coord));
                    double a = computeMultiAlphaLevel(coord);
                    cur.a=a;
                    color.layer(cur);
                }
                return color;
            }
            default: {
                return tFunc.getColor(0);
            }
        }
    }

    public BufferedImage visualize(double[] viewMatrix) {
        boolean slicerFinished = slicer(viewMatrix);
        
        if (slicerFinished) {
            return image;
        } else {
            return null;
        }
    }
}