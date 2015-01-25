/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package render.order;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static render.order.RenderOrder.RESOLUTIONS;

/**
 *
 * @author Helmond
 */
public class RandomOrder extends RenderOrder{

    public RandomOrder(int resolution, boolean prevRessComputed, int imageSize) {
        super(resolution, prevRessComputed, imageSize);
    }
    
    private final ArrayList<int[]> pixels = new ArrayList<int[]>();

    @Override
    protected int[] getCoordinate(int i) {
        if(pixels.isEmpty())
        {
            generatePixels();
        }
        if(i>=pixels.size())return new int[]{-1,-1};
        //System.out.println(i + " " + resolution + " " + Arrays.toString(pixels.get(i)));
        return pixels.get(i);
    }

    private void generatePixels() {
        for(int i = 0; i < scaledsize;i++)for(int j = 0; j<scaledsize;j++)pixels.add(new int[]{transform(i),transform(j)});
        Collections.shuffle(pixels);
    }

    @Override
    protected int getAmmountToSkipOnFault() {
        return 0;
    }

    @Override
    protected int getMaxAllowedFaults() {
        return 0;
    }
    
    
    
    
    
    public static List<CombinedOrder<RandomOrder>> getThreadedOrders(int threads, int imageSize)
    {
        List<CombinedOrder<RandomOrder>> os = new ArrayList<CombinedOrder<RandomOrder>>();
        int cropsize = imageSize/threads;
        for(int x = 0; x< threads; x++)
        {
            int transx = x*cropsize;
            for(int y = 0; y<threads;y++)
            {
                int transy = y*cropsize;
                int[] trans = new int[]{transx,transy};
                boolean prev = false;
                CombinedOrder<RandomOrder> co = new CombinedOrder<RandomOrder>(cropsize);
                for(int res:RESOLUTIONS)
                {
                    if(res>imageSize-1)continue;
                    co.addOrder(new RandomOrder(res, prev, cropsize));
                    prev=true;
                }
                co.setTranslation(trans);
                os.add(co);
            }
        }
        
        return os;
    }
    
}
