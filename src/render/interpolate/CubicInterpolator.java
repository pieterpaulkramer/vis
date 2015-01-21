/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package render.interpolate;

import java.util.HashMap;

/**
 *
 * @author Helmond
 */
public class CubicInterpolator extends Interpolator {

    @Override
    public double getValue(double x, double y, double z) {
        return cubic(x, y, z);
    }

    private double cubic(double x, double y, double z) {
        return f(x, y, z);
    }

    private double cint(double a1, double a2, double a3, double a4, double x) {

        double[] ps = new double[]{a1, a2, a3, a4};
        double[] xs = getCubicVector(x);
        double dot = 0;
        for (int i = 0; i < 4; i++) {
            dot += ps[i] * xs[i];
        }
        return dot / 2;
    }

    private double[] getCubicVector(double x) {

        double x2 = x * x;
        return new double[]{
            x * ((2 - x) * x - 1),
            x2 * (3 * x - 5) + 2,
            x * ((4 - 3 * x) * x + 1),
           (x - 1) * x2
        };

    }

    private double s(int i, int j, int k) {
        return g.getValue(i, j, k);
    }

    private double t(int i, int j, double z) {
        int fz = (int) Math.floor(z);
        return cint(s(i, j, fz - 1), s(i, j, fz), s(i, j, fz + 1), s(i, j, fz + 2), z - fz);
    }

    private double u(int i, double y, double z) {
        int fy = (int) Math.floor(y);
        return cint(t(i, fy - 1, z), t(i, fy, z), t(i, fy + 1, z), t(i, fy + 2, z), y - fy);
    }

    private double f(double x, double y, double z) {
        int fx = (int) Math.floor(x);
        return cint(u(fx - 1, y, z), u(fx, y, z), u(fx + 1, y, z), u(fx + 2, y, z), x - fx);
    }

}
