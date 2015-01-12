/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import volvis.OpacityFunction;
import volvis.TFColor;
import volvis.TransferFunction;

/**
 *
 * @author michel
 */
public class OpacityWeightView extends javax.swing.JPanel {

    private OpacityFunction ofunc;
    private final int DOTSIZE = 8;
    private int selected;
    private Point dragStart;
    private OpacityWeightEditor editor;
    private int[] histogram;

    /**
     * Creates new form TransferFunctionView
     */
    public OpacityWeightView(OpacityFunction ofunc, int[] histogram, OpacityWeightEditor ed) {
        initComponents();
        this.ofunc = ofunc;
        this.editor = ed;
        this.histogram = histogram;
        addMouseMotionListener(new ControlPointHandler());
        addMouseListener(new SelectionHandler());
    }

    @Override
    public void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;

        int w = this.getWidth();
        int h = this.getHeight();
        int range = ofunc.getMaximum() - ofunc.getMinimum();
        int min = ofunc.getMinimum();

        g2.setColor(Color.white);
        g2.fillRect(0, 0, w, h);

        //draw histogram
        int nrBins = histogram.length;
        int maxBinHeigth = 0;
        for (int i = 5; i < nrBins; i++) {
            maxBinHeigth = histogram[i] > maxBinHeigth ? histogram[i] : maxBinHeigth;//THE FIRST datapoint is usually noise or empty space, which we have a lot.
        }//ignore that , so the rest of the histogram is drawn more clearly
        double binWidth = (double) w / (double) nrBins;
        g2.setColor(Color.lightGray);
        double scalingFactor = (double) h / (double) maxBinHeigth;
        for (int i = 0; i < nrBins; i++) {
            g2.fill(new Rectangle2D.Double(i * binWidth, h - scalingFactor * histogram[i], binWidth, scalingFactor * histogram[i]));
        }

        int xprev = -1;
        for (int i = 0; i < ofunc.getControlPoints().size(); i++) {
            int val = ofunc.getControlPoints().get(i).value;
            double width = ofunc.getControlPoints().get(i).width;
            double al = ofunc.getControlPoints().get(i).alphafactor;
            //System.out.println("s = " + s);
            double t1 = (double) (val - width - min) / (double) range;
            double t2 = (double) (val - min) / (double) range;
            double t3 = (double) (val + width - min) / (double) range;
            
            double hf = 1-al;
            //System.out.println("t = " + t);
            int xpos1 = (int) (t1 * w);
            int xpos2 = (int) (t2 * w);
            int xpos3 = (int) (t3 * w);
            int ypos = (int) (hf*h);
            //System.out.println("x = " + xpos + "; y = " + ypos);
            g2.setColor(Color.black);
            g2.fillOval(xpos2 - DOTSIZE / 2, ypos-DOTSIZE/2, DOTSIZE, DOTSIZE);

            g2.drawLine(xpos1, h, xpos2, ypos);
            g2.drawLine(xpos2, ypos, xpos3, h);
            if (xprev > -1) {
                g2.drawLine(xpos1, h, xprev, h);
            }
            xprev = xpos3;
        }
    }

    private Ellipse2D getControlPointArea(OpacityFunction.ControlPoint cp) {
        int w = this.getWidth();
        int h = this.getHeight();
        int range = ofunc.getMaximum() - ofunc.getMinimum();
        int min = ofunc.getMinimum();

        int val = cp.value;
        
        double t = (double) (val - min) / (double) range;
        int xpos = (int) (t * w);
        int ypos = (int)(h*(1-cp.alphafactor));
        Ellipse2D bounds = new Ellipse2D.Double(xpos - DOTSIZE / 2, ypos - DOTSIZE / 2, DOTSIZE, DOTSIZE);
        return bounds;
    }

    private class ControlPointHandler extends MouseMotionAdapter {
        

        
        @Override
        public void mouseMoved(MouseEvent e) {
            boolean inside = false;
            for (int i = 0; i < ofunc.getControlPoints().size(); i++) {
                inside = inside || getControlPointArea(ofunc.getControlPoints().get(i)).contains(e.getPoint());
            }
            if (inside) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            } else {
                setCursor(Cursor.getDefaultCursor());
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (selected < 0) {
                return;
            }
            Point dragEnd = e.getPoint();
            if (dragEnd.y < 0) {
                dragEnd.y = 0;
            }
            if (dragEnd.y > getHeight()) {
                dragEnd.y = getHeight();
            }

            double w = getWidth();
            double h = getHeight();
            int range = ofunc.getMaximum() - ofunc.getMinimum();
            int min = ofunc.getMinimum();
            double na = 1d-dragEnd.y/h;
            double t = dragEnd.x / w;
            double s = ((t * range) + min);
            if (SwingUtilities.isLeftMouseButton(e)) {
                ofunc.updateControlPointScalar(selected, (int)s);
                ofunc.updateControlPointAlpha(selected, na);
                double wid = ofunc.getControlPoints().get(selected).width;
                editor.setSelectedInfo(selected, (int)s,wid);
            } else if (SwingUtilities.isMiddleMouseButton(e)) {
                ofunc.updateControlPointWidth(selected, Math.abs(s - ofunc.getControlPoints().get(selected).value)/10d);
                int val = ofunc.getControlPoints().get(selected).value;
                double wid = ofunc.getControlPoints().get(selected).width;
                editor.setSelectedInfo(selected, val,wid);
            }
             
            repaint();

        }
    }

    private class SelectionHandler extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            ArrayList<OpacityFunction.ControlPoint> controlPoints = ofunc.getControlPoints();
            boolean inside = false;
            int idx = 0;
            while (!inside && idx < controlPoints.size()) {
                inside = inside || getControlPointArea(controlPoints.get(idx)).contains(e.getPoint());
                if (inside) {
                    break;
                } else {
                    idx++;
                }
            }
            if (inside) {
                if (e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON2) {
                    selected = idx;
                    //System.out.println("selected: " + controlPoints.get(selected).toString());
                    OpacityFunction.ControlPoint cp = controlPoints.get(selected);
                    editor.setSelectedInfo(selected, cp.value, cp.width);
                    dragStart = e.getPoint();
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    
                        ofunc.removeControlPoint(idx);   
                        dragStart = e.getPoint();
                    
                }
            } else {
                Point pos = e.getPoint();
                if (pos.x >= 0 && pos.x < getWidth() && pos.y >= 0 && pos.y < (getHeight())) {
                    double w = getWidth();
                    double h = getHeight();
                    int range = ofunc.getMaximum() - ofunc.getMinimum();
                    int min = ofunc.getMinimum();
                    double t = pos.x / w;
                    int s = (int) ((t * range) + min);
                    //System.out.println("s = " + s);
                    double a = (h - pos.y) / h;
                    //System.out.println("a = " + a);
                    selected = ofunc.addControlPoint(s, 1,0.9d);
                    OpacityFunction.ControlPoint cp = controlPoints.get(selected);
                    editor.setSelectedInfo(selected, cp.value, cp.width);
                    dragStart = e.getPoint();
                    repaint();
                }
            }

        }

        @Override
        public void mouseReleased(MouseEvent e) {
            repaint();
            ofunc.changed();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
