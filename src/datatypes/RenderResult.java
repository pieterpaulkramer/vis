/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package datatypes;

import java.awt.image.BufferedImage;
import volume.Volume;

/**
 *
 * @author Pieter-Paul
 */
public class RenderResult {
    
    private final long id;
    private final BufferedImage image;
    private final Volume volume;
    private final int resolution;
    
    public RenderResult(long id, BufferedImage image, Volume volume, int resolution) {
        this.id = id;
        this.image = image;
        this.volume = volume;
        this.resolution = resolution;
    }
    
    public long getId() {
        return id;
    }
    
    public int getResolution() {
        return resolution;
    }
    
    public BufferedImage getImage() {
        return image;
    }

    public Volume getVolume() {
        return volume;
    }
}
