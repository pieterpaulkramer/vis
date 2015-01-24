/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import util.TFChangeListener;

/**
 *
 * @author michel
 */
public class SurfaceTransferFunction {

    private double maxIntensity, maxGradient;
    private Rectangle2D.Double domain;
    private ArrayList<TFChangeListener> listeners = new ArrayList<TFChangeListener>();
    private ArrayList<ControlRectangle> controlPoints;

    public SurfaceTransferFunction(double maxGradient, double maxIntensity) {
        this.maxIntensity = maxIntensity;
        this.maxGradient = maxGradient;
        this.domain = new Rectangle2D.Double(0, 0, maxGradient, maxIntensity);
        this.controlPoints = new ArrayList<ControlRectangle>();
    }

    public double getMaxIntensity() {
        return maxIntensity;
    }

    public double getMaxGradient() {
        return maxGradient;
    }

    public ArrayList<ControlRectangle> getControlPoints() {
        return controlPoints;
    }

    public int addControlPoint(Rectangle2D.Double area, Color color, double alphaf) {
        if (!domain.contains(area)) {
            return -1;
        }

        ControlRectangle cp = new ControlRectangle(area, color, alphaf);
        controlPoints.add(cp);
        return controlPoints.size() - 1;
    }

    public void removeControlPoint(int idx) {
        controlPoints.remove(idx);
    }

    public void updateControlPointAlpha(int idx, double a) {
        controlPoints.get(idx).alpha = a;
    }

    public void updateControlPointColor(int idx, Color color) {
        controlPoints.get(idx).color = color;
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
    
    public class ControlRectangle {
        
        public Rectangle2D.Double area;
        public Color color;
        public double alpha;
        
        public ControlRectangle(Rectangle2D.Double area, Color color, double alpha) {
            this.area = area;
            this.color = color;
            this.alpha = alpha;
        }
    }
}