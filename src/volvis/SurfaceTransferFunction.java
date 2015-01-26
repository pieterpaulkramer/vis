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
    private ArrayList<ControlRectangle> controlRectangles;

    public SurfaceTransferFunction(double maxIntensity, double maxGradient) {
        this.maxIntensity = maxIntensity;
        this.maxGradient = maxGradient;
        this.domain = new Rectangle2D.Double(0, 0, maxIntensity, maxGradient);
        this.controlRectangles = new ArrayList<ControlRectangle>();
    }

    public double getMaxIntensity() {
        return maxIntensity;
    }

    public double getMaxGradient() {
        return maxGradient;
    }

    public ArrayList<ControlRectangle> getControlRectangles() {
        return controlRectangles;
    }
    
    public ControlRectangle getControlRectangle(int idx) {
        return controlRectangles.get(idx);
    }

    public int addControlRectangle(Rectangle2D.Double area, Color color, double alphaf) {
        if (!domain.contains(area)) {
            throw new RuntimeException("Area not in domain");
        }

        ControlRectangle cp = new ControlRectangle(area, color, alphaf);
        controlRectangles.add(cp);
        
        changed();
        
        return controlRectangles.size() - 1;
    }

    public void removeControlRectangle(int idx) {
        controlRectangles.remove(idx);
        changed();
    }

    public void updateControlRectangleAlpha(int idx, double a) {
        if (controlRectangles.get(idx).alpha == a) return;
        
        controlRectangles.get(idx).alpha = a;
        changed();
    }

    public void updateControlRectangleColor(int idx, Color color) {
        controlRectangles.get(idx).color = color;
        changed();
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
        
        public Color getColorWithAlpha() {
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha*255));
        }
    }
}