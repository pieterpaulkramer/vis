/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import java.awt.Color;
import java.util.ArrayList;
import util.TFChangeListener;

/**
 *
 * @author michel
 */
public class OpacityFunction {

    private ArrayList<TFChangeListener> listeners = new ArrayList<TFChangeListener>();

    // Construct a default grey-scale transfer function over the scalar range min - max.
    // The opacity increases linearly from 0.0 to 1.0
    public OpacityFunction(short min, short max) {
        sMin = min;
        sMax = max;
        sRange = sMax - sMin;
        controlPoints = new ArrayList<ControlPoint>();
    }

    public int getMinimum() {
        return sMin;
    }

    public int getMaximum() {
        return sMax;
    }

    public ArrayList<ControlPoint> getControlPoints() {
        return controlPoints;
    }

    public int addControlPoint(int value, int width) {
        if (value < sMin || value > sMax) {
            return -1;
        }
       

        ControlPoint cp = new ControlPoint(value, width);
        controlPoints.add(cp);
        return controlPoints.size()-1;
    }

    public void removeControlPoint(int idx) {
        controlPoints.remove(idx);
    }

    public void updateControlPointScalar(int index, int s) {
        controlPoints.get(index).value = s;
    }

    public void updateControlPointWidth(int idx, int w) {
        ControlPoint cp = controlPoints.get(idx);
        cp.width = w;
    }

    // Add a changelistener, which will be notified is the transfer function changes
    public void addTFChangeListener(TFChangeListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }

    // notify the change listeners
    public void changed() {
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).changed();
        }
    }



    public class ControlPoint implements Comparable<ControlPoint> {

        public int value;
        public int width;

        public ControlPoint(int v, int width) {
            value = v;
            this.width = width;
        }

        @Override
        public int compareTo(ControlPoint t) {
            return (value < t.value ? -1 : (value == t.value ? 0 : 1));
        }

        @Override
        public String toString() {
            return "(" + value + ") -> " + width;
        }
    }
    private short sMin, sMax;
    private int sRange;
    private TFColor[] LUT;
    private int LUTsize = 4095;
    private ArrayList<ControlPoint> controlPoints;
}
