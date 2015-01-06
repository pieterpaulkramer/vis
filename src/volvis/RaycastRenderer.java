/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import gui.RaycastRendererPanel;
import gui.TransferFunctionEditor;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.media.opengl.GL2;
import util.TFChangeListener;
import util.VectorMath;
import volume.Volume;

/**
 *
 * @author michel
 */
public class RaycastRenderer extends Renderer implements TFChangeListener {

    public final static int MIP = 723623796;
    public final static int COMPOSITING = 267673489;
    public final static int OPACITYWEIGHTING = 234624534;

    private int mode = MIP;
    private int resolution = 5;
    private boolean trilinint = false;
    private Volume volume = null;
    private final List<int[]> values = new ArrayList<int[]>();
    RaycastRendererPanel panel;
    TransferFunction tFunc;
    TransferFunctionEditor tfEditor;

    public RaycastRenderer() {
        panel = new RaycastRendererPanel(this);
        panel.setSpeedLabel("0");
    }

    public void setMode(int mode) {
        this.mode = mode;
        changed();
    }

    public void setResolution(int res) {
        this.resolution = res;
        changed();
    }

    public void setTriLinInt(boolean b) {
        this.trilinint = b;
        changed();
    }

    public void setVolume(Volume vol) {
        volume = vol;

        // set up image for storing the resulting rendering
        // the image width and height are equal to the length of the volume diagonal
        int imageSize = (int) Math.floor(Math.sqrt(vol.getDimX() * vol.getDimX() + vol.getDimY() * vol.getDimY()
                + vol.getDimZ() * vol.getDimZ()));
        if (imageSize % 2 != 0) {
            imageSize = imageSize + 1;
        }
        image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        tFunc = new TransferFunction(volume.getMinimum(), volume.getMaximum());
        tFunc.addTFChangeListener(this);
        tfEditor = new TransferFunctionEditor(tFunc, volume.getHistogram());
        panel.setTransferFunctionEditor(tfEditor);

    }

    @Override
    public void changed() {
        for (TFChangeListener listener : listeners) {
            listener.changed();
        }
    }

    public RaycastRendererPanel getPanel() {
        return panel;
    }

    // get a voxel from the volume data by nearest neighbor interpolation
    short getVoxel(double[] coord) {
        if (trilinint) {// for now there is no noticeable diffrence between NN and TL inerpolation, so we choos NN due to it being faster.
            int x = (int) Math.round(coord[0]);
            int y = (int) Math.round(coord[1]);
            int z = (int) Math.round(coord[2]);

            if ((x >= 0) && (x < volume.getDimX()) && (y >= 0) && (y < volume.getDimY())
                    && (z >= 0) && (z < volume.getDimZ())) {
                return volume.getVoxel(x, y, z);
            } else {
                return 0;
            }
        } else {//trilinear interpolation
            int x1 = (int) Math.floor(coord[0]);
            int y1 = (int) Math.floor(coord[1]);
            int z1 = (int) Math.floor(coord[2]);

            if ((x1 >= 0) && (x1 < volume.getDimX() - 1) && (y1 >= 0) && (y1 < volume.getDimY() - 1)
                    && (z1 >= 0) && (z1 < volume.getDimZ() - 1)) {
                double xf = coord[0] - x1;
                double yf = coord[1] - y1;
                double zf = coord[2] - z1;
                double value = 0;
                value += volume.getVoxel(x1, y1, z1) * (1d - xf) * (1d - yf) * (1d - zf);
                value += volume.getVoxel(x1 + 1, y1, z1) * (xf) * (1d - yf) * (1d - zf);
                value += volume.getVoxel(x1, y1 + 1, z1) * (1d - xf) * (yf) * (1d - zf);
                value += volume.getVoxel(x1 + 1, y1 + 1, z1) * (xf) * (yf) * (1d - zf);
                value += volume.getVoxel(x1, y1, z1 + 1) * (1d - xf) * (1d - yf) * (zf);
                value += volume.getVoxel(x1 + 1, y1, z1 + 1) * (xf) * (1d - yf) * (zf);
                value += volume.getVoxel(x1, y1 + 1, z1 + 1) * (1d - xf) * (yf) * (zf);
                value += volume.getVoxel(x1 + 1, y1 + 1, z1 + 1) * (xf) * (yf) * (zf);
                return (short) Math.round(value);
            }

            return 0;
        }
    }

    short getVoxel(double x, double y, double z) {
        return getVoxel(new double[]{x, y, z});
    }

    void slicer(double[] viewMatrix) {

        /* clear image
         for (int j = 0; j < image.getHeight(); j++) {
         for (int i = 0; i < image.getWidth(); i++) {
         image.setRGB(i, j, 0);
         }
         }//*/
        // vector uVec and vVec define a plane through the origin, 
        // perpendicular to the view vector viewVec
        double[] viewVec = new double[3];
        double[] uVec = new double[3];
        double[] vVec = new double[3];
        VectorMath.setVector(viewVec, viewMatrix[2], viewMatrix[6], viewMatrix[10]);
        VectorMath.setVector(uVec, viewMatrix[0], viewMatrix[4], viewMatrix[8]);
        VectorMath.setVector(vVec, viewMatrix[1], viewMatrix[5], viewMatrix[9]);

        // image is square
        int imageCenter = image.getWidth() / 2;

        double[] volumeCenter = new double[3];
        VectorMath.setVector(volumeCenter, volume.getDimX() / 2, volume.getDimY() / 2, volume.getDimZ() / 2);

        // sample on a plane through the origin of the volume data
        for (int j = 0; j < image.getHeight() / resolution; j++) {
            for (int i = 0; i < image.getWidth() / resolution; i++) {
                int castx = i * resolution + (resolution - 1) / 2;
                int casty = j * resolution + (resolution - 1) / 2;
                double[][] ray = CastRay(uVec, castx, imageCenter, vVec, casty, volumeCenter, viewVec);
                TFColor voxelColor = computeColor(ray);

                // BufferedImage expects a pixel color packed as ARGB in an int
                int c_alpha = voxelColor.a <= 1.0 ? (int) Math.floor(voxelColor.a * 255) : 255;
                int c_red = voxelColor.r <= 1.0 ? (int) Math.floor(voxelColor.r * 255) : 255;
                int c_green = voxelColor.g <= 1.0 ? (int) Math.floor(voxelColor.g * 255) : 255;
                int c_blue = voxelColor.b <= 1.0 ? (int) Math.floor(voxelColor.b * 255) : 255;
                int pixelColor = (c_alpha << 24) | (c_red << 16) | (c_green << 8) | c_blue;
                for (int r = 0; r < resolution; r++) {
                    for (int r2 = 0; r2 < resolution; r2++) {
                        image.setRGB(i * resolution + r, j * resolution + r2, pixelColor);
                    }
                }
            }
        }
    }

    private double[][] CastRay(double[] uVec, int i, int imageCenter, double[] vVec, int j, double[] volumeCenter, double[] viewVec) {
        int samples = (int) Math.ceil(VectorMath.length(new double[]{volume.getDimX(), volume.getDimY(), volume.getDimZ()}));
        double[][] pixelcoords = new double[samples][3];
        for (int k = -samples / 2; k < samples / 2; k++) {
            int index = k + samples / 2;
            pixelcoords[index][0] = uVec[0] * (i - imageCenter) + vVec[0] * (j - imageCenter)
                    + volumeCenter[0] + k * viewVec[0];
            pixelcoords[index][1] = uVec[1] * (i - imageCenter) + vVec[1] * (j - imageCenter)
                    + volumeCenter[1] + k * viewVec[1];
            pixelcoords[index][2] = uVec[2] * (i - imageCenter) + vVec[2] * (j - imageCenter)
                    + volumeCenter[2] + k * viewVec[2];
        }
        return pixelcoords;
    }

    private double[] lGradientVector(double x, double y, double z) {
        return new double[]{
            0.5d * (getVoxel(x + 1, y, z) - getVoxel(x - 1, y, z)),
            0.5d * (getVoxel(x, y + 1, z) - getVoxel(x, y - 1, z)),
            0.5d * (getVoxel(x, y, z + 1) - getVoxel(x, y, z - 1))
        };
    }

    private double computeSingleAlphaLevel(double x, double y, double z, int r, int fv) {
        double[] grad = lGradientVector(x, y, z);
        double gradl = VectorMath.length(grad);
        int val = getVoxel(x, y, z);
        if (val == fv && gradl == 0) {
            return 1;
        } else if (gradl > 0 && val - r * gradl <= fv && val + r * gradl >= fv) {
            return 1d - (1d / r) * (Math.abs(fv - val) / gradl);
        } else {
            return 0;
        }
    }

    private double computeMultiAlphaLevel(double x, double y, double z, int[] rs, int[] fvs) {
        double res = 1;
        for (int i = 0; i < rs.length; i++) {
            res *= (1 - computeSingleAlphaLevel(x, y, z, rs[i], fvs[i]));
        }
        return 1 - res;
    }

    private double computeMultiAlphaLevel(double x, double y, double z, List<int[]> fvnrs) {
        double res = 1;
        for (int i = 0; i < fvnrs.size(); i++) {
            res *= (1 - computeSingleAlphaLevel(x, y, z, fvnrs.get(i)[0], fvnrs.get(i)[1]));
        }
        return 1 - res;
    }

    private double computeMultiAlphaLevel(double[] coord, List<int[]> fvnrs) {
        return computeMultiAlphaLevel(coord[0], coord[1], coord[2], fvnrs);
    }

    private TFColor computeColor(double[] coord) {
        switch (mode) {
            case (MIP): {
                return tFunc.getColor(getVoxel(coord));
            }
            case (COMPOSITING): {
                return tFunc.getColor(getVoxel(coord));
            }
            case (OPACITYWEIGHTING): {
                TFColor col = tFunc.getColor(getVoxel(coord));
                //TODO shading
                return col;
            }
            default: {
                return null;
            }
        }
    }

    private TFColor computeColor(double[][] ray) {
        switch (mode) {
            case (MIP): {
                int max = 0;

                for (double[] coord : ray) {
                    max = Math.max(max, getVoxel(coord));
                }
                return tFunc.getColor(max);
            }
            case (COMPOSITING): {
                TFColor color = new TFColor();
                for (double[] coord : ray) {
                    TFColor cur = tFunc.getColor(getVoxel(coord));
                    color.layer(cur);
                }
                return color;
            }
            case (OPACITYWEIGHTING): {
                TFColor color = new TFColor();
                for (double[] coord : ray) {
                    TFColor cur = computeColor(coord);
                    double a = computeMultiAlphaLevel(coord, values);
                    cur.a = a;
                    color.layer(cur);
                }
                return color;
            }
            default: {
                return tFunc.getColor(0);
            }
        }

    }

    private void drawBoundingBox(GL2 gl) {
        gl.glPushAttrib(GL2.GL_CURRENT_BIT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glColor4d(1.0, 1.0, 1.0, 1.0);
        gl.glLineWidth(1.5f);
        gl.glEnable(GL2.GL_LINE_SMOOTH);
        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glDisable(GL2.GL_LINE_SMOOTH);
        gl.glDisable(GL2.GL_BLEND);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glPopAttrib();

    }

    @Override
    public void visualize(GL2 gl) {

        if (volume == null) {
            return;
        }

        drawBoundingBox(gl);

        gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, viewMatrix, 0);

        long startTime = System.currentTimeMillis();
        slicer(viewMatrix);
        long endTime = System.currentTimeMillis();
        double runningTime = (endTime - startTime);
        panel.setSpeedLabel(Double.toString(runningTime));

        Texture texture = AWTTextureIO.newTexture(gl.getGLProfile(), image, false);

        gl.glPushAttrib(GL2.GL_LIGHTING_BIT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

        // draw rendered image as a billboard texture
        texture.enable(gl);
        texture.bind(gl);
        double halfWidth = image.getWidth() / 2.0;
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glBegin(GL2.GL_QUADS);
        gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glTexCoord2d(0.0, 0.0);
        gl.glVertex3d(-halfWidth, -halfWidth, 0.0);
        gl.glTexCoord2d(0.0, 1.0);
        gl.glVertex3d(-halfWidth, halfWidth, 0.0);
        gl.glTexCoord2d(1.0, 1.0);
        gl.glVertex3d(halfWidth, halfWidth, 0.0);
        gl.glTexCoord2d(1.0, 0.0);
        gl.glVertex3d(halfWidth, -halfWidth, 0.0);
        gl.glEnd();
        texture.disable(gl);
        texture.destroy(gl);
        gl.glPopMatrix();

        gl.glPopAttrib();

        if (gl.glGetError() > 0) {
            System.out.println("some OpenGL error: " + gl.glGetError());
        }

    }
    private BufferedImage image;
    private double[] viewMatrix = new double[4 * 4];

}
