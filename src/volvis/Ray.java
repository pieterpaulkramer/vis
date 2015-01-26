/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import java.util.ArrayList;
import java.util.Iterator;
import util.VectorMath;
import volume.Volume;

/**
 *
 * @author Helmond
 */
public class Ray implements Iterable<double[]> {

    private final ArrayList<double[]> coordinates;
    private final ArrayList<TFColor> renderResult;//the unlayered colors of this ray
    private final TFColor result = new TFColor();

    public Ray(Volume volume, double[] uVec, double[] vVec, double[] viewVec, double[] pan, double i, double j, int imageCenter, double[] volumeCenter) {
        int maxsamples = (int) Math.ceil(VectorMath.length(new double[]{volume.getDimX(), volume.getDimY(), volume.getDimZ()}));
        this.coordinates = new ArrayList<double[]>();
        this.renderResult = new ArrayList<TFColor>();
        i -= pan[0];
        j -= pan[1];
        for (int k = -maxsamples / 2; k < maxsamples / 2; k++) {
            int index = k + maxsamples / 2;//=distance to view plane!
            double[] coord = new double[]{uVec[0] * (i - imageCenter) + vVec[0] * (j - imageCenter) + volumeCenter[0] + k * viewVec[0],
                uVec[1] * (i - imageCenter) + vVec[1] * (j - imageCenter) + volumeCenter[1] + k * viewVec[1],
                uVec[2] * (i - imageCenter) + vVec[2] * (j - imageCenter) + volumeCenter[2] + k * viewVec[2],
                index};
            if (volume.getVoxel((int) coord[0], (int) coord[1], (int) coord[2], true) > 5) {
                this.coordinates.add(coord);
            }
        }
    }

    /**
     * Constructor for the 3 plane-view.
     */
    public Ray(Volume volume, double[] uVec, double[] vVec, double[] viewVec, double i, double j, int imageCenter, double[] volumeCenter, int[] intersectionpoint) {
        int maxsamples = (int) Math.ceil(VectorMath.length(new double[]{volume.getDimX(), volume.getDimY(), volume.getDimZ()}));
        this.coordinates = new ArrayList<double[]>();
        this.renderResult = new ArrayList<TFColor>();
        for (int k = -maxsamples / 2; k < maxsamples / 2; k++) {
            int index = k + maxsamples / 2;//=distance to view plane!
            double[] coord = new double[]{uVec[0] * (i - imageCenter) + vVec[0] * (j - imageCenter) + volumeCenter[0] + k * viewVec[0],
                uVec[1] * (i - imageCenter) + vVec[1] * (j - imageCenter) + volumeCenter[1] + k * viewVec[1],
                uVec[2] * (i - imageCenter) + vVec[2] * (j - imageCenter) + volumeCenter[2] + k * viewVec[2],
                index};
            if (volume.getVoxel((int) coord[0], (int) coord[1], (int) coord[2], true) > 5
                    && (((int) coord[0] == intersectionpoint[0])
                    || ((int) coord[1] == intersectionpoint[1])
                    || ((int) coord[2] == intersectionpoint[2]))) {
                this.coordinates.add(coord);
            }
        }
    }

    public ArrayList<double[]> getCoordinates() {
        return this.coordinates;
    }

    /**
     * Adds the given color to the result ray
     * Assumes that the results are given in the order of the coordinates of this.coordinates
     * Updates the result
     * @param c 
     */
    public void addRenderResult(TFColor c) {
        this.renderResult.add(c);
        result.layer(c);
    }

    /**
     * Returns the result of this ray as an ARGB integer
     * @return 
     */
    public TFColor getResult() {
        return result;
    }

    public int getIntResult() {
        TFColor col = getResult();
        int c_alpha = col.a <= 1.0 ? (int) Math.floor(col.a * 255) : 255;
        int c_red = col.r <= 1.0 ? (int) Math.floor(col.r * 255) : 255;
        int c_green = col.g <= 1.0 ? (int) Math.floor(col.g * 255) : 255;
        int c_blue = col.b <= 1.0 ? (int) Math.floor(col.b * 255) : 255;
        int pixelColor = (c_alpha << 24) | (c_red << 16) | (c_green << 8) | c_blue;
        return pixelColor;
    }
    
    public double[] getProminentVoxelCoordinate()
    {
        double maxAlpha = 0;
        for(int i = this.renderResult.size()-1;i>=0;i--)
        {
            maxAlpha = Math.max(maxAlpha, this.renderResult.get(i).a);
        }
        return null;
    }

    @Override
    public Iterator<double[]> iterator() {
        return this.coordinates.iterator();
    }

}
