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
public class Ray implements Iterable<double[]>{
    
    private final ArrayList<double[]> coordinates;
    private int i = 0;

    public Ray(Volume volume, double[] uVec, double[] vVec, double[] viewVec,double[] pan, double i, double j, int imageCenter, double[] volumeCenter ) {
        int maxsamples = (int) Math.ceil(VectorMath.length(new double[]{volume.getDimX(), volume.getDimY(), volume.getDimZ()}));
        this.coordinates = new ArrayList<double[]>();
        i -= pan[0];
        j -= pan[1];
        for (int k = -maxsamples / 2; k < maxsamples / 2; k++) {
            //int index = k + maxsamples / 2;//=distance to view plane!
            double[] coord = new double[]{uVec[0] * (i - imageCenter) + vVec[0] * (j - imageCenter) + volumeCenter[0] + k * viewVec[0],
                uVec[1] * (i - imageCenter) + vVec[1] * (j - imageCenter) + volumeCenter[1] + k * viewVec[1],
                uVec[2] * (i - imageCenter) + vVec[2] * (j - imageCenter) + volumeCenter[2] + k * viewVec[2]
            };
            if(volume.getVoxel((int)coord[0], (int)coord[1], (int)coord[2],true)>5)
            {
                this.coordinates.add(coord);
            }
        }
    }
    
    
    
    public boolean hasNextCoordinate()
    {
        return i<coordinates.size();
    }
    public double[] getNextCoordinate()
    {
        return coordinates.get(++i);
    }

    @Override
    public Iterator<double[]> iterator() {
        return this.coordinates.iterator();
    }
    
}
