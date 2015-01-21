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

}
