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
public abstract class RenderOrder {

    public final static int[] RESOLUTIONS = new int[]{33, 17, 9, 5, 3, 1};

    protected final int resolution;
    private final boolean prevRessComputed;//If true, the pixel values returned should never be the same as with a worse resolution
    protected final int imageSize;
    protected final int dif;
    protected final int scaledsize;

    public RenderOrder(int resolution, boolean prevRessComputed, int imageSize) {
        this.resolution = resolution;
        this.prevRessComputed = prevRessComputed;
        this.imageSize = imageSize;
        this.dif = (resolution - 1) / 2;
        this.scaledsize = (int) Math.floor((double) imageSize / resolution);;
    }

    public List<int[]> getAllCoordinates() {
        ArrayList<int[]> coords = new ArrayList<int[]>();
        for (int i = 0; true; i++) {
            int[] coord = getCoordinate(i);
            if (coord[0]-dif < 0 || coord[0]+dif>=imageSize || coord[1]-dif<0 || coord[1]+dif>=imageSize) {
                break;
            }
            if (!isInWorseResolution(coord)) {
                coords.add(coord);
            }
        }
        return coords;
    }

    protected abstract int[] getCoordinate(int i);

    private boolean isInWorseResolution(int[] coordinate) {
        if (!prevRessComputed) {
            return false;
        }
        int nextResolution = resolution == 1 ? 3 : (resolution - 1) * 2 + 1;
        int dif = (nextResolution - 1) / 2;
        int x = coordinate[0] - dif;
        int y = coordinate[1] - dif;
        return x % nextResolution == 0 && y % nextResolution == 0;
    }

    public int[][] getPixelsToFill(int[] pixel) {
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
    
    protected int transform(int c)
    {
        return c*resolution+dif;
    }
    

}
