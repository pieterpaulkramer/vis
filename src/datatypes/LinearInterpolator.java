/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datatypes;

/**
 *
 * @author Helmond
 */
public class LinearInterpolator extends Interpolator{


    @Override
    public double getValue(double x, double y, double z) {
        int x1 = (int) Math.floor(x);
        int y1 = (int) Math.floor(y);
        int z1 = (int) Math.floor(z);
        double xf = x - x1;
        double yf = y - y1;
        double zf = z - z1;
        double value = 0;
        value += g.getValue(x1, y1, z1) * (1d - xf) * (1d - yf) * (1d - zf);
        value += g.getValue(x1 + 1, y1, z1) * (xf) * (1d - yf) * (1d - zf);
        value += g.getValue(x1, y1 + 1, z1) * (1d - xf) * (yf) * (1d - zf);
        value += g.getValue(x1 + 1, y1 + 1, z1) * (xf) * (yf) * (1d - zf);
        value += g.getValue(x1, y1, z1 + 1) * (1d - xf) * (1d - yf) * (zf);
        value += g.getValue(x1 + 1, y1, z1 + 1) * (xf) * (1d - yf) * (zf);
        value += g.getValue(x1, y1 + 1, z1 + 1) * (1d - xf) * (yf) * (zf);
        value += g.getValue(x1 + 1, y1 + 1, z1 + 1) * (xf) * (yf) * (zf);
        return value;
    }
    
}
