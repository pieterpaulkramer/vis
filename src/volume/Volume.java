/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volume;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import render.interpolate.Grid;
import render.interpolate.Interpolator;


/**
 *
 * @author michel
 */
public class Volume {
    
    private int dimX, dimY, dimZ;
    private short[] data;
    private int[] histogram;
    private double maxGradientSq;

    public Volume(int xd, int yd, int zd) {
        maxGradientSq = -1;
        
        data = new short[xd * yd * zd];
        
        dimX = xd;
        dimY = yd;
        dimZ = zd;
    }

    public Volume(File file) {
        maxGradientSq = -1;
        
        try {
            VolumeIO reader = new VolumeIO(file);
            dimX = reader.getXDim();
            dimY = reader.getYDim();
            dimZ = reader.getZDim();
            data = reader.getData().clone();
            computeHistogram();
        } catch (IOException ex) {
            System.out.println("IO exception");
        }

    }

    public short getVoxel(int x, int y, int z) {
        return data[x + dimX * (y + dimY * z)];
    }
    
    /**
     * Like getVoxel(x, y, z), but returns 0 if x, y, z is not in the domain of this volume.
     */
    public short getVoxel(int x, int y, int z, boolean zero_if_missing) {
        if (zero_if_missing && (x < 0 || x >= getDimX() || y < 0 || y >= getDimY() || z < 0 || z >= getDimZ())) {
            return 0;
        } else {
            return getVoxel(x, y, z);
        }
    }

    public void setVoxel(int x, int y, int z, short value) {
        data[x + dimX * (y + dimY * z)] = value;
    }

    public void setVoxel(int i, short value) {
        data[i] = value;
    }

    public short getVoxel(int i) {
        return data[i];
    }

    public int getDimX() {
        return dimX;
    }

    public int getDimY() {
        return dimY;
    }

    public int getDimZ() {
        return dimZ;
    }

    public short getMinimum() {
        short minimum = data[0];
        for (int i = 0; i < data.length; i++) {
            minimum = data[i] < minimum ? data[i] : minimum;
        }
        return minimum;
    }

    public short getMaximum() {
        short maximum = data[0];
        for (int i = 0; i < data.length; i++) {
            maximum = data[i] > maximum ? data[i] : maximum;
        }
        return maximum;
    }
    
    public double getMaximumGradient() {
        computeMaximumGradientSq();
        return Math.pow(maxGradientSq, 0.5);
    }
    
    private void computeMaximumGradientSq() {
        if (maxGradientSq < 0) {
            for (int x=0; x<getDimX(); x++) {
                for (int y=0; y<getDimY(); y++) {
                    for (int z=0; z<getDimZ(); z++) {
                        maxGradientSq = Math.max(maxGradientSq,
                                Math.pow(0.5 * (getVoxel(x + 1, y, z, true) - getVoxel(x - 1, y, z, true)), 2) +
                                Math.pow(0.5 * (getVoxel(x, y + 1, z, true) - getVoxel(x, y - 1, z, true)), 2) +
                                Math.pow(0.5 * (getVoxel(x, y, z + 1, true) - getVoxel(x, y, z - 1, true)), 2));
                    }
                }
            }
        }
    }

    public int[] getHistogram() {
        return histogram;
    }

    private void computeHistogram() {
        histogram = new int[getMaximum() + 1];
        for (int i = 0; i < data.length; i++) {
            histogram[data[i]]++;
        }
    }
    
    public int[][] getSurfacesPlot(int gradientbins) {        
        int[][] plot = new int[getMaximum() + 1][gradientbins];
        
        computeMaximumGradientSq();
        double gradientBinWidth = Math.pow(maxGradientSq, 0.5) / (gradientbins-1);
        
        for (int x=0; x<getDimX(); x++) {
            for (int y=0; y<getDimY(); y++) {
                for (int z=0; z<getDimZ(); z++) {
                    double gradient = Math.pow(
                            Math.pow(0.5 * (getVoxel(x + 1, y, z, true) - getVoxel(x - 1, y, z, true)), 2) +
                            Math.pow(0.5 * (getVoxel(x, y + 1, z, true) - getVoxel(x, y - 1, z, true)), 2) +
                            Math.pow(0.5 * (getVoxel(x, y, z + 1, true) - getVoxel(x, y, z - 1, true)), 2), 0.5);
                    
                    plot[getVoxel(x,y,z)][(int)(gradient / gradientBinWidth)] += 1;
                }
            }
        }
        
        return plot;
    }
    
    public static Volume enhanceVolume(final Volume v, Interpolator ip, int scale)
    {
        Volume nv = new Volume(v.getDimX()*scale,v.getDimY()*scale,v.getDimZ()*scale);
        Grid g = new Grid(){

            @Override
            public double getValue(int x, int y, int z) {
                return v.getVoxel(x, y, z, true);
            }
        };
        ip.setGrid(g);
        double ds = 1d/scale;
        for(int x = 0; x<v.getDimX()*scale;x++)
        {
            for(int y = 0; y<v.getDimY()*scale;y++)
            {
                for(int z = 0; z<v.getDimZ()*scale;z++)
                {
                    short val = (short)ip.getValue(x*ds, y*ds, z*ds);
                    nv.setVoxel(x, y, z, val);
                }
            }
        }
        return nv;
    }
}
