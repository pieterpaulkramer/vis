package volvis;

import java.awt.image.BufferedImage;

/**
 *
 * @author Helmond
 */
public class Image {
    
    private final BufferedImage image;
    private final Ray[][] rays;
    
    public Image(int size)
    {
        this.image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        rays = new Ray[size][size];
    }
    
    public int getWidth()
    {
        return this.image.getWidth();
    }
    
    public int getHeight()
    {
        return this.image.getHeight();
    }
    
    public void setRay(int x, int y, Ray r)
    {
        this.rays[x][y] = r;
        this.image.setRGB(x, y, r.getIntResult());
    }

    public BufferedImage getImage() {
        return image;
    }
    
    void clearBlack() {
        for(int x = 0; x<image.getWidth();x++)for(int y = 0; y < image.getHeight();y++)image.setRGB(x, y, 0);
    }

    void clearWhite() {
        for(int x = 0; x<image.getWidth();x++)for(int y = 0; y < image.getHeight();y++)image.setRGB(x, y, (255 << 24) + (255 << 16) + (255 << 8) + 255);
    }
    
    public double[] getProminentVoxelCoordinate(int x, int y)
    {
        return this.rays[x][y].getProminentVoxelCoordinate();
    }
    
}
