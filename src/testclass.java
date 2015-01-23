
import java.util.Arrays;
import java.util.List;
import render.order.CombinedOrder;
import render.order.RenderOrder;
import render.order.SequentialOrder;
import render.order.SpiralOrder;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Helmond
 */
public class testclass {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        CombinedOrder co = SequentialOrder.getThreadedOrders(1, 5).get(0);
        List<int[]> coords = co.getAllCoordinates();
        System.out.println(Arrays.deepToString(coords.toArray()));
        System.out.println(co.getAllCoordinates().size());
    }
    
}
