/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import java.util.ArrayList;
import java.util.Collections;
import util.TFChangeListener;

/**
 *
 * @author michel
 */
public class OpacityFunction {

    private ArrayList<TFChangeListener> listeners = new ArrayList<TFChangeListener>();
    private RenderingController ofunclisterner;
    private short sMin, sMax;
    private ArrayList<ControlPoint> controlPoints;

    // Construct a default grey-scale transfer function over the scalar range min - max.
    // The opacity increases linearly from 0.0 to 1.0
    public OpacityFunction(short min, short max) {
        sMin = min;
        sMax = max;
        controlPoints = new ArrayList<ControlPoint>();
        controlPoints.add(new ControlPoint(min,0,0));
        controlPoints.add(new ControlPoint(max,0,0));

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

    public int addControlPoint(int value, double width, double alphaf) {
        if (value < sMin || value > sMax) {
            return -1;
        }

        ControlPoint cp = new ControlPoint(value, width, alphaf);
        int idx = 0;
        while (idx < controlPoints.size() && controlPoints.get(idx).compareTo(cp) < 0) {
            idx++;
        }


        if (controlPoints.get(idx).compareTo(cp) == 0) {
            controlPoints.set(idx, cp);
        } else {
            controlPoints.add(idx, cp);
        }

        return controlPoints.indexOf(cp);
    }

    public void removeControlPoint(int idx) {
        controlPoints.remove(idx);
    }

    public void updateControlPointScalar(int index, int s) {
        controlPoints.get(index).value = s;
        Collections.sort(controlPoints);
    }

    public void updateControlPointAlpha(int index, double a) {
        controlPoints.get(index).alphafactor = a;
    }

    public void updateControlPointWidth(int idx, double w) {
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
        ofunclisterner.ochanged();
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).changed();
        }
    }

    void addOpChangeListener(RenderingController aThis) {
        this.ofunclisterner = aThis;
    }

    public class ControlPoint implements Comparable<ControlPoint> {

        public int value;
        public double width;
        public double alphafactor;

        public ControlPoint(int v, double width, double alphafactor) {
            value = v;
            this.width = width;
            this.alphafactor = alphafactor;
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
}