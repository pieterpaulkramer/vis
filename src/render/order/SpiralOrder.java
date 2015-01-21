/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package render.order;

/**
 *
 * @author Jeffrey
 */
public class SpiralOrder extends RenderOrder{

    public SpiralOrder(int resolution, boolean prevRessComputed, int imageSize) {
        super(resolution, prevRessComputed, imageSize);
    }

    @Override
    protected int[] getCoordinate(int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
