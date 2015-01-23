/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package render.order;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Helmond
 */
public class CombinedOrder<O extends RenderOrder> extends RenderOrder {

    private final List<O> orders = new ArrayList<O>();

    public CombinedOrder(int imageSize) {
        super(1, true, imageSize);
    }

    public boolean addOrder(O o) {
        if (orders.isEmpty() && o.isPrevRessComputed()) {
            return false;
        }
        if (orders.isEmpty()) {
            o.setTranslation(this.getTranslation());
            orders.add(o);
            return true;
        } else {
            if (orders.get(orders.size() - 1).getResolution() != (o.getResolution() - 1) * 2 + 1
                    && !(o.getResolution()==1 && orders.get(orders.size() - 1).getResolution()==3)) {
                return false;
            }
            o.setTranslation(this.getTranslation());
            orders.add(o);
            return true;
        }
    }

    @Override
    protected void setTranslation(int[] translation) {
        super.setTranslation(translation);
        for(O o:this.orders)
        {
            o.setTranslation(translation);
        }
    }
    
    

    @Override
    public List<int[]> getAllCoordinates() {
        ArrayList<int[]> res = new ArrayList<int[]>();
        for(RenderOrder o:orders)
        {
            res.addAll(o.getAllCoordinates());
        }
        return res;
    }

    @Override
    protected int[] getCoordinate(int i) {
        throw new UnsupportedOperationException("not used by this class"); //To change body of generated methods, choose Tools | Templates.
    }

    private int getResolutionOfPixel(int[] pix) {
        for(int res:RESOLUTIONS)
        {
            int dif2 = (res-1)/2;
            int x = pix[0]-dif2;
            int y = pix[1]-dif2;
            if(x%res==0 && y%res==0)return res;//TOTO? more efficient?
        }
        return 1;
    }

    @Override
    public int[][] getPixelsToFill(int[] pixel) {
        return CombinedOrder.getPixelsToFill(pixel, getResolutionOfPixel(pixel));
    }

}
