/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

/**
 *
 * @author michel
 */
public class TFColor {
    public double r, g, b, a;

    public TFColor() {
        r = g = b = a = 1.0;
    }
    
    public TFColor(double red, double green, double blue, double alpha) {
        r = red;
        g = green;
        b = blue;
        a = alpha;
    }
    
    @Override
    public String toString() {
        String text = "(" + r + ", " + g + ", " + b + ", " + a + ")";
        return text;
    }

    void layer(TFColor c) {
        r = c.r*c.a+ r*a*(1-c.a);
        g = c.g*c.a+ g*a*(1-c.a);
        b = c.b*c.a+ b*a*(1-c.a);
        a = c.a+ a*(1-c.a);
        
    }
}
