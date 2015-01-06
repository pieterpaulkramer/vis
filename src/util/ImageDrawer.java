/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.awt.image.BufferedImage;
import javax.media.opengl.GL2;
import volume.Volume;

/**
 *
 * @author Pieter-Paul
 */
public interface ImageDrawer {
    
    public void draw(GL2 gl, BufferedImage image, Volume volume);
    
}
