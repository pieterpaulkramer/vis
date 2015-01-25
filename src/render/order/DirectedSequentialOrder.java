/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package render.order;

import java.util.ArrayList;
import java.util.List;
import static render.order.RenderOrder.RESOLUTIONS;

/**
 *
 * @author Jeffrey
 */
public class DirectedSequentialOrder extends RenderOrder {

    private final int direction;//0 = north, 2 = east, 4 = south, 6 = west

    public DirectedSequentialOrder(int resolution, boolean prevRessComputed, int imageSize, int direction) {
        super(resolution, prevRessComputed, imageSize);
        this.direction = direction % 8;
    }

    @Override
    protected int[] getCoordinate(int i) {
        int[] coord;
        if (direction % 2 == 0) {//straight
            int x = i % (scaledsize);
            int y = i / (scaledsize);
            switch (direction) {
                case (0):
                    break;
                case (2): {
                    int k = x;
                    x = y;
                    y = k;
                    break;
                }
                case (4): {
                    y = scaledsize - y - 1;
                    break;
                }
                case (6): {
                    int k = x;
                    x = scaledsize - y - 1;
                    y = k;
                    break;
                }
            }
            x = transform(x);
            y = transform(y);
            coord = new int[]{x, y};
        } else {
            //layer*(layer+1)/2=(i+1)
            //layer2/2+layer/2-(i+1)=0
            //a=1/2, b=1/2, c=-(i+1)
            //layer = (-1/2+Math.sqrt(1/4+2*i))
            int layer = (int) Math.ceil((-1 / 2d + Math.sqrt(1d / 4d + 2d * (i+1))));
            int ammountOnPrevLayers = (layer * (layer - 1)) / 2;
            int numberOnLayer = i - ammountOnPrevLayers;
            int x = layer - numberOnLayer-1, y = numberOnLayer;
            switch (direction) {
                case (1): {
                    break;
                }
                case (3): {
                    break;
                }
                case (5): {
                    break;
                }
                case (7): {
                    break;
                }
            }
            x = transform(x);
            y = transform(y);
            coord = new int[]{x, y};
        }
        return coord;
    }

    public static List<CombinedOrder<DirectedSequentialOrder>> getThreadedOrders(int threads, int imageSize) {
        List<CombinedOrder<DirectedSequentialOrder>> os = new ArrayList<CombinedOrder<DirectedSequentialOrder>>();
        int cropsize = imageSize / threads;
        for (int x = 0; x < threads; x++) {
            int transx = x * cropsize;
            for (int y = 0; y < threads; y++) {
                int transy = y * cropsize;
                int[] trans = new int[]{transx, transy};
                boolean prev = false;
                CombinedOrder<DirectedSequentialOrder> co = new CombinedOrder<DirectedSequentialOrder>(cropsize);
                for (int res : RESOLUTIONS) {
                    if (res > imageSize - 1) {
                        continue;
                    }
                    co.addOrder(new DirectedSequentialOrder(res, prev, cropsize, 1));
                    prev = true;
                }
                co.setTranslation(trans);
                os.add(co);
            }
        }
        return os;
    }

}
