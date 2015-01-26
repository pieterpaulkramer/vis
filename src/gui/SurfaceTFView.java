/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import volvis.SurfaceTransferFunction;
import volvis.SurfaceTransferFunction.ControlRectangle;

/**
 *
 * @author michel
 */
public class SurfaceTFView extends javax.swing.JPanel {

    private SurfaceTransferFunction stfFunc;
    private SurfaceTFEditor editor;
    private ControlPointHandler mouseListener;
    
    private BufferedImage plotRendering;
    private int plotWidth;
    private int plotHeight;
    private double plotMaxGradient;
    private int plotMaxPoints;

    /**
     * Creates new form TransferFunctionView
     */
    public SurfaceTFView(SurfaceTransferFunction stfFunc, SurfaceTFEditor ed) {
        initComponents();
        this.stfFunc = stfFunc;
        this.editor = ed;
        this.mouseListener = new ControlPointHandler();
        
        addMouseMotionListener(this.mouseListener);
        addMouseListener(this.mouseListener);
    }
    
    public void drawPlot(int[][] plot, double maximumGradient) {
        plotMaxPoints = 0;
        for (int x=0; x<plot.length; x++) {
            for (int y=plot[0].length-1; y>=0; y--) {
                // TODO: Dit normaliseren niet voor de totale afbeelding maar lokaal
                // (per horizontale strip?) zodat je vage stroken hoger in de afbeelding
                // ook nog kan zien. Of: deze stroken sterker tekenen als je er met je muis
                // in de buurt bent.
                plotMaxPoints = Math.max(plotMaxPoints, plot[x][y]);
            }
        }
        
        plotWidth = plot.length;
        plotHeight = plot[0].length;
        plotMaxGradient = maximumGradient;
        plotRendering = new BufferedImage(plotWidth, plotHeight, BufferedImage.TYPE_INT_ARGB);
        
        int alpha_mul_factor = plotMaxPoints < 255 ? 255 / plotMaxPoints : 1;

        // Draw points
        for (int x=0; x<plotWidth; x++) {
            for (int y=0; y<plotHeight; y++) {
                int alpha = Math.min(255, plot[x][y]*alpha_mul_factor);
                plotRendering.setRGB(x, plotHeight-y-1, alpha << 24); // Black with a certain gradient
            }
        }
        
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Color.white);
        g2.fillRect(0, 0, getWidth(), getHeight());
        
        // Draw the actual plot
        g2.drawImage(this.plotRendering, 0, 0, getWidth(), getHeight(), null);
        
        // Draw the control rectangles
        for (ControlRectangle rect: stfFunc.getControlRectangles()) {
            Rectangle screenRect = screenRectFromValueRect(rect.area);
            
            g2.setColor(rect.getColorWithAlpha());
            g2.fillRect(screenRect.x, screenRect.y, screenRect.width, screenRect.height);
            
            g2.setColor(Color.black);
            g2.setStroke(new BasicStroke(1));
            g2.drawRect(screenRect.x, screenRect.y, screenRect.width, screenRect.height);
        }
    }
    
    protected Rectangle screenRectFromValueRect(Rectangle2D.Double r) {
        Point.Double topLeft = new Point.Double(r.x, r.y);
        Point.Double bottomRight = new Point.Double(r.x + r.width, r.y + r.height);
        
        // Note that because value rectangles are inverted in y-directions, the
        // relative positions of the points wrt eachother change.
        
        Point bottomLeft = screenPointFromValuePoint(topLeft);
        Point topRight = screenPointFromValuePoint(bottomRight);
        
        return new Rectangle(
                bottomLeft.x,
                topRight.y,
                topRight.x - bottomLeft.x,
                bottomLeft.y - topRight.y
        );
    }
    
    protected Rectangle2D.Double valueRectFromScreenRect(Rectangle r) {
        Point topLeft = new Point(r.x, r.y);
        Point bottomRight = new Point(r.x + r.width, r.y + r.height);
        
        // Note that because value rectangles are inverted in y-directions, the
        // relative positions of the points wrt eachother change.
        
        Point.Double bottomLeft = valuePointFromScreenPoint(topLeft);
        Point.Double topRight = valuePointFromScreenPoint(bottomRight);
        
        return new Rectangle2D.Double(
                bottomLeft.x,
                topRight.y,
                topRight.x - bottomLeft.x,
                bottomLeft.y - topRight.y
        );
    }
    
    protected Point.Double valuePointFromScreenPoint(Point p) {
        return new Point.Double(
                plotWidth * p.x / (double) getWidth(),
                plotMaxGradient * (1 - (p.y / (double) getHeight()))
        );
    }
    
    protected Point screenPointFromValuePoint(Point.Double p) {
        return new Point(
                (int) (getWidth() * p.x / plotWidth),
                (int) (getHeight() * (1 - (p.y / plotMaxGradient)))
        );
    }
    
    protected int createControlRectangle(Rectangle rectangleBeingCreated) {
        int idx = stfFunc.addControlRectangle(valueRectFromScreenRect(rectangleBeingCreated), Color.yellow, 0.5);
        repaint();
        return idx;
    }

    private class ControlPointHandler implements MouseMotionListener, MouseListener {
        
        private boolean creatingRectangle;
        private Point creatingRectangleStart;
        public Rectangle rectangleBeingCreated;
        
        @Override
        public void mouseMoved(MouseEvent e) {
            boolean inside = false;
            for (ControlRectangle p: stfFunc.getControlRectangles()) {
                if (screenRectFromValueRect(p.area).contains(e.getPoint())) {
                    inside = true;
                    break;
                }
            }
            if (inside) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else {
                setCursor(Cursor.getDefaultCursor());
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (!creatingRectangle) {
                return;
            }
            
            Point dragStart = creatingRectangleStart;
            Point dragEnd = e.getPoint();
            
            int x;
            int y;
            int width;
            int height;
            
            if (dragStart.x < dragEnd.x) {
                x = dragStart.x;
                width = Math.min(dragEnd.x, getWidth()) - x;
            } else {
                x = Math.max(dragEnd.x, 0);
                width = dragStart.x - dragEnd.x;
            }
            
            if (dragStart.y < dragEnd.y) {
                y = dragStart.y;
                height = Math.min(dragEnd.y, getHeight()) - y;
            } else {
                y = Math.max(dragEnd.y, 0);
                height = dragStart.y - y;
            }
            
            rectangleBeingCreated = new Rectangle(x, y, width, height);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            ArrayList<ControlRectangle> controlPoints = stfFunc.getControlRectangles();
            boolean inside = false;
            int idx;
            for (idx=0; idx<controlPoints.size(); idx++) {
                if (screenRectFromValueRect(controlPoints.get(idx).area).contains(e.getPoint())) {
                    inside = true;
                    break;
                }
            }
            
            if (creatingRectangle) {
                // This corresponds to the user dragging to create a rectangle and then
                // clicking with the right mouse button. We are expected in this case to
                // stop creating a rectangle.
                creatingRectangle = false;
            } else if (!inside && (e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON2)) {
                Point pos = e.getPoint();
                if (pos.x >= 0 && pos.x < getWidth() && pos.y >= 0 && pos.y < getHeight()) {
                    creatingRectangle = true;
                    creatingRectangleStart = e.getPoint();
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (creatingRectangle && rectangleBeingCreated != null && rectangleBeingCreated.width > 0 && rectangleBeingCreated.height > 0) {
                int idx = createControlRectangle(rectangleBeingCreated);
                editor.setSelectedInfo(idx);
            }
            
            creatingRectangle = false;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            ArrayList<ControlRectangle> controlPoints = stfFunc.getControlRectangles();
            boolean inside = false;
            int idx;
            for (idx=0; idx<controlPoints.size(); idx++) {
                if (screenRectFromValueRect(controlPoints.get(idx).area).contains(e.getPoint())) {
                    inside = true;
                    break;
                }
            }
            
            if (inside) {
                if (e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON2) {
                    editor.setSelectedInfo(idx);
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    stfFunc.removeControlRectangle(idx);
                    repaint();
                }
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) { }

        @Override
        public void mouseExited(MouseEvent e) { }
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
