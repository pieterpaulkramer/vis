/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package render.interpolate;

/**
 *
 * @author Helmond
 */
public class NearestNeighbourInterpolator extends Interpolator{

   

    @Override
    public double getValue(double x, double y, double z) {
        return g.getValue((int) x, (int) y, (int) z);
    }
    
}
