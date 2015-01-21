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
public abstract class Interpolator {

    public static final int NEARESTNEIGHBOUR = 34895;
    public static final int LINEAR = 45789342;
    public static final int CUBIC = 3794545;
    protected Grid g;

    public abstract double getValue(double x, double y, double z);

    public void setGrid(Grid g)
    {
        this.g=g;
    }
    

}
