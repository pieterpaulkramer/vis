/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package render.order;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 *
 * @author Jeffrey
 */
public abstract class RenderOrder {

    public final static int[] RESOLUTIONS = new int[]{9, 5, 3, 1};

    protected final int resolution;
    private final boolean prevRessComputed;//If true, the pixel values returned should never be the same as with a worse resolution
    protected final int imageSize;
    protected final int dif;
    protected final int scaledsize;
    private int[] translation = new int[]{0, 0};

    public RenderOrder(int resolution, boolean prevRessComputed, int imageSize) {
        this.resolution = resolution;
        this.prevRessComputed = prevRessComputed;
        this.imageSize = imageSize;
        this.dif = (resolution - 1) / 2;
        this.scaledsize = (int) Math.floor((double) imageSize / resolution);
    }

    public List<Tuple<int[], Integer>> getAllCoordinates() {
        final int maxfails = getMaxAllowedFaults();
        final int skips = getAmmountToSkipOnFault();
        int fails = 0;
        ArrayList<Tuple<int[], Integer>> coords = new ArrayList<Tuple<int[], Integer>>();
        for (int i = 0; true; i++) {

            int[] coord = getCoordinate(i);

            if (coord[0] - dif < 0 || coord[0] + dif >= imageSize || coord[1] - dif < 0 || coord[1] + dif >= imageSize) {
                fails++;
                if (fails > maxfails) {
                    break;
                }
                i += skips;
                continue;
            }
            fails = 0;
            coord[0] += translation[0];
            coord[1] += translation[1];
            if (!isInWorseResolution(coord)) {
                coords.add(new Tuple<int[],Integer>(coord, resolution));
            }
        }
        return coords;
    }

    protected abstract int[] getCoordinate(int i);

    protected abstract int getAmmountToSkipOnFault();

    protected abstract int getMaxAllowedFaults();

    private boolean isInWorseResolution(int[] coordinate) {
        if (!prevRessComputed) {
            return false;
        }
        int nextResolution = resolution == 1 ? 3 : (resolution - 1) * 2 + 1;
        int dif2 = (nextResolution - 1) / 2;
        int x = coordinate[0] - dif2;
        int y = coordinate[1] - dif2;
        boolean b = coordinate[0] + dif2 >= imageSize || coordinate[0] - dif2 < 0
                || coordinate[1] + dif2 >= imageSize || coordinate[1] - dif2 < 0;
        return x % (nextResolution) == 0 && y % (nextResolution) == 0 && !b;
    }

    public static int[][] getPixelsToFill(Tuple<int[], Integer> pix) {
        int resolution = pix.o2;
        int[] pixel = pix.o1;
        int dif = (resolution - 1) / 2;
        int[][] pixels = new int[resolution * resolution][2];
        for (int x = -dif; x <= dif; x++) {
            int idx1 = (x + dif);
            for (int y = -dif; y <= dif; y++) {
                int idx2 = (y + dif);
                int indx = idx1 * resolution + idx2;
                pixels[indx][0] = pixel[0] + x;
                pixels[indx][1] = pixel[1] + y;
            }
        }
        return pixels;
    }

    public int getResolution() {
        return this.resolution;
    }

    public boolean isPrevRessComputed() {
        return prevRessComputed;
    }

    protected int transform(int c) {
        if (resolution == 1) {
            return c;
        }
        return c * (resolution) + (resolution - 1) / 2;
    }

    protected void setTranslation(int[] translation) {
        this.translation = translation;
    }

    protected int[] getTranslation() {
        return this.translation;
    }

    public static List<CombinedOrder<RenderOrder>> getOptimalThreadedOrders(int rootOFThreads, int imageSize) {
        List<CombinedOrder<RenderOrder>> os = new ArrayList<CombinedOrder<RenderOrder>>();
        int cropsize = imageSize / rootOFThreads;
        for (int x = 0; x < rootOFThreads; x++) {
            int transx = x * cropsize;
            for (int y = 0; y < rootOFThreads; y++) {
                int transy = y * cropsize;
                int[] trans = new int[]{transx, transy};
                boolean prev = false;
                CombinedOrder<RenderOrder> co = new CombinedOrder<RenderOrder>(cropsize);
                for (int res : RESOLUTIONS) {
                    if (res > imageSize - 1) {
                        continue;
                    }
                    if (rootOFThreads == 1) {
                        co.addOrder(new SpiralOrder(res, prev, cropsize));
                    } else if (rootOFThreads == 2) {
                        int[] val = new int[]{7, 5, 1, 3};
                        co.addOrder(new DirectedSequentialOrder(res, prev, cropsize, val[x * rootOFThreads + y]));
                    } else if (rootOFThreads == 3) {
                        int[] val = new int[]{7, 6, 5, 0, -1, 4, 1, 2, 3};
                        int indx = x * rootOFThreads + y;
                        if (indx == 4) {
                            co.addOrder(new SpiralOrder(res, prev, cropsize));
                        } else {
                            co.addOrder(new DirectedSequentialOrder(res, prev, cropsize, val[indx]));
                        }
                    } else {
                        co.addOrder(new RandomOrder(res, prev, cropsize));
                    }

                    prev = true;
                }
                co.setTranslation(trans);
                os.add(co);
            }
        }
        return os;
    }
}
