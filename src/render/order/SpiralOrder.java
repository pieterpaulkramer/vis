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
public class SpiralOrder extends RenderOrder {

    public SpiralOrder(int resolution, boolean prevRessComputed, int imageSize) {
        super(resolution, prevRessComputed, imageSize);
    }

    @Override
    protected int[] getCoordinate(int i) {
        int x, y;
        //i=8*depth*(depth+1)/2
        //4depth2+4depth-i=0
        //a=4,b=4,c=-i
        //depth=(-4+Math.sqrt(16-16*-i))/8
        //depth=(Math.sqrt(1+i)-1)/2
        int depth = (int) Math.ceil((Math.sqrt(1+i)-1)/2);
        int ammountOnLevel = Math.max(1, depth * 8);
        int ammountsOnPreviousLevels = i==0?0:8 * (depth*(depth-1)) / 2;
        int numberOnLevel = i==0?0:i - ammountsOnPreviousLevels-1;
        int side = ammountOnLevel / 4;

        if (numberOnLevel < side) {
            x=0;
            y=numberOnLevel;
        } else if (numberOnLevel < 2 * side) {
            x=numberOnLevel-side;
            y=side;
        } else if (numberOnLevel < 3 * side) {
            x=side;
            y=side-(numberOnLevel-2*side);
        } else {
            y=0;
            x=side-(numberOnLevel-3*side);
        }
        x+=(scaledsize)/2-depth;
        y+=(scaledsize)/2-depth;
        x = transform(x);
        y = transform(y);
        return new int[]{x, y};
    }

    @Override
    protected int getAmmountToSkipOnFault() {
        return 0;
    }

    @Override
    protected int getMaxAllowedFaults() {
        return scaledsize*3+1;
    }
    
    
    
    public static List<CombinedOrder<SpiralOrder>> getThreadedOrders(int rootOFThreads, int imageSize)
    {
        List<CombinedOrder<SpiralOrder>> os = new ArrayList<CombinedOrder<SpiralOrder>>();
        int cropsize = imageSize/rootOFThreads;
        for(int x = 0; x< rootOFThreads; x++)
        {
            int transx = x*cropsize;
            for(int y = 0; y<rootOFThreads;y++)
            {
                int transy = y*cropsize;
                int[] trans = new int[]{transx,transy};
                boolean prev = false;
                CombinedOrder<SpiralOrder> co = new CombinedOrder<SpiralOrder>(cropsize);
                for(int res:RESOLUTIONS)
                {if(res>imageSize-1)continue;
                    co.addOrder(new SpiralOrder(res, prev, cropsize));
                    prev=true;
                }
                co.setTranslation(trans);
                os.add(co);
            }
        }
        return os;
    }

}
