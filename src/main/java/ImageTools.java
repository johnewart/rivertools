import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.*;


public class ImageTools {
    final static Logger logger = LoggerFactory.getLogger(ImageTools.class);

    public static BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    public static BufferedImage byteToBuffered(int[] pixels, int width, int height) throws IllegalArgumentException {
        DataBufferInt dbuf = new DataBufferInt(pixels, width * height);
        BandedSampleModel model = new BandedSampleModel(DataBuffer.TYPE_USHORT, width, height, 1);
        WritableRaster raster = Raster.createWritableRaster(model, dbuf, new Point(0, 0));
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
        image.setData(raster);
        return image;
    }

    // Dilate by 1 in O(n^2) time
    public static int[][] dilate(int[][] image) {
        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[i].length; j++) {
                if (image[i][j] == 1) {
                    if (i > 0 && image[i - 1][j] == 0) image[i - 1][j] = 2;
                    if (j > 0 && image[i][j - 1] == 0) image[i][j - 1] = 2;
                    if (i + 1 < image.length && image[i + 1][j] == 0) image[i + 1][j] = 2;
                    if (j + 1 < image[i].length && image[i][j + 1] == 0) image[i][j + 1] = 2;
                }
            }
        }

        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[i].length; j++) {
                if (image[i][j] == 2) {
                    image[i][j] = 1;
                }
            }
        }

        return image;
    }

    public static BufferedImage convert2DIntToARGB(int[][] pixels) {
        int w = pixels[0].length;
        int h = pixels.length;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int intensity = pixels[y][x];
                Color c = new Color(intensity, intensity, intensity, 255);
                image.setRGB(x, y, c.getRGB());
            }
        }

        return image;
    }

    public static int[][] convertBWTo2D(BufferedImage image) {
        final byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        final int width = image.getWidth();
        final int height = image.getHeight();

        logger.debug("Width: " + width);
        logger.debug("Height: " + height);
        logger.debug("Pixels: " + pixels.length + " vs: " + width * height);

        int[][] result = new int[height][width];

        for (int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel++) {
            result[row][col] = (int) pixels[pixel];
            col++;
            if (col == width) {
                col = 0;
                row++;
            }
        }

        return result;

    }

    public static int[][] convertRGBTo2D(BufferedImage image) {
        final byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        final int width = image.getWidth();
        final int height = image.getHeight();
        final boolean hasAlphaChannel = image.getAlphaRaster() != null;

        int[][] result = new int[height][width];
        if (hasAlphaChannel) {
            logger.debug("Alpha channel image...");
            final int pixelLength = 4;
            for (int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel += pixelLength) {
                int argb = 0;
                argb += (((int) pixels[pixel] & 0xff) << 24); // alpha
                argb += ((int) pixels[pixel + 1] & 0xff); // blue
                argb += (((int) pixels[pixel + 2] & 0xff) << 8); // green
                argb += (((int) pixels[pixel + 3] & 0xff) << 16); // red
                result[row][col] = argb;
                col++;

                if (col == width) {
                    col = 0;
                    row++;
                }
            }
        } else {
            final int pixelLength = 3;
            logger.debug("Non-alpha channel image...");
            for (int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel += pixelLength) {
                int argb = 0;
                argb += -16777216; // 255 alpha
                argb += ((int) pixels[pixel] & 0xff); // blue
                argb += (((int) pixels[pixel + 1] & 0xff) << 8); // green
                argb += (((int) pixels[pixel + 2] & 0xff) << 16); // red
                result[row][col] = argb;
                col++;

                if (col == width) {
                    col = 0;
                    row++;
                }
            }
        }
        return result;
    }

    public static int[][] convertRGBTo2DBinary(BufferedImage image, int blackPixel) {
        int[][] result = ImageTools.convertRGBTo2D(image);

        for (int row = 0; row < result.length; row++) {
            for (int col = 0; col < result[0].length; col++) {
                if (result[row][col] == blackPixel) {
                    result[row][col] = 0;
                } else {
                    result[row][col] = 1;
                }
            }
        }

        return result;
    }

}

