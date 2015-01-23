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
 * @author Jeffrey
 */
public class SequentialOrder extends RenderOrder {

    public SequentialOrder(int resolution, boolean prevRessComputed, int imageSize) {
        super(resolution, prevRessComputed, imageSize);

    }

    @Override
    protected int[] getCoordinate(int i) {
        int x = i % (scaledsize);
        int y = i / (scaledsize);
        x = transform(x);
        y = transform(y);
        int[] coord = new int[]{x, y};
        return coord;
    }
    
        
    public static List<CombinedOrder<SequentialOrder>> getThreadedOrders(int threads, int imageSize)
    {
        List<CombinedOrder<SequentialOrder>> os = new ArrayList<CombinedOrder<SequentialOrder>>();
        int cropsize = imageSize/threads;
        for(int x = 0; x< threads; x++)
        {
            int transx = x*cropsize;
            for(int y = 0; y<threads;y++)
            {
                int transy = y*cropsize;
                int[] trans = new int[]{transx,transy};
                boolean prev = false;
                CombinedOrder<SequentialOrder> co = new CombinedOrder<SequentialOrder>(cropsize);
                for(int res:RESOLUTIONS)
                {
                    co.addOrder(new SequentialOrder(res, prev, cropsize));
                    prev=true;
                }
                co.setTranslation(trans);
                os.add(co);
            }
        }
        return os;
    }

}
