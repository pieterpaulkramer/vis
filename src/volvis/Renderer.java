/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import util.TFChangeListener;
import volume.Volume;

/**
 *
 * @author michel
 */
public abstract class Renderer {
     int winWidth, winHeight;
    boolean visible = false;
    boolean interactiveMode = false;
    ArrayList<TFChangeListener> listeners = new ArrayList<TFChangeListener>();

    public Renderer() {
        
    }

    public void setInteractiveMode(boolean flag) {
        interactiveMode = flag;
    }
    
    public void setWinWidth(int w) {
        winWidth = w;
    }

    public void setWinHeight(int h) {
        winHeight = h;
    }

    public int getWinWidth() {
        return winWidth;
    }

    public int getWinHeight() {
        return winHeight;
    }

    public void setVisible(boolean flag) {
        visible = flag;
    }

    public boolean getVisible() {
        return visible;
    }

    public void addTFChangeListener(TFChangeListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }
    
    public abstract void visualize(double[] viewMatrix, double zoom,double[] pan);
    
    public abstract Image rendering();
    
    public abstract boolean done();

    public abstract Volume getVolume();
}
