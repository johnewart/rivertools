package net.johnewart.rivertools.utils;

import java.awt.*;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.factory.Hints;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.Envelope2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.media.jai.Interpolation;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ScaleDescriptor;

import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageTools {

    final static String THUMBNAIL_FORMAT = "png";
    final static Logger LOG = LoggerFactory.getLogger(ImageTools.class);

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

    public static boolean writeGeoTiffWithCoordinates(String imageWithCoordinates, BufferedImage imageToWrite, String outputFileName)
    {

        IIORegistry registry = IIORegistry.getDefaultInstance();
        registry.registerServiceProvider(new com.tomgibara.imageio.impl.tiff.TIFFImageWriterSpi());
        registry.registerServiceProvider(new com.tomgibara.imageio.impl.tiff.TIFFImageReaderSpi());

        try {
            System.out.println("Getting coordinates from: " + imageWithCoordinates + "...");
            /*GeoTiffReader gr = new GeoTiffReader(new File(imageWithCoordinates), null);
            GridCoverage2D coverage = gr.read(null);
            CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem2D();
            Envelope2D env = coverage.getEnvelope2D();
            */

            File imageFile = new File(imageWithCoordinates);

            AbstractGridFormat format = GridFormatFinder.findFormat(imageFile);
            AbstractGridCoverage2DReader reader = format.getReader(imageFile);
            GridCoverage2D coverage = reader.read(null);
            Envelope2D env = coverage.getEnvelope2D();

            GridCoverageFactory factory = new GridCoverageFactory(new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE));

            GridCoverage2D gridCoverage = factory.create("27", imageToWrite, env);
            System.out.println("Writing: " + outputFileName + "...");
            String tmpfilename = outputFileName + ".tmp";

            File destinationImage = new File(outputFileName);
            File tempImage = new File(tmpfilename);

            GeoTiffWriter gw = new GeoTiffWriter(tempImage);
            gw.write(gridCoverage, null);

            if(destinationImage.exists())
            {
                destinationImage.delete();
            }

            tempImage.renameTo(destinationImage);

            return true;
        } catch (IOException ioe) {
            System.err.println("Problem writing out the image: " + ioe.toString());
            ioe.printStackTrace();
            return false;
        }
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

        System.err.println("Width: " + width);
        System.err.println("Height: " + height);
        System.err.println("Pixels: " + pixels.length + " vs: " + width * height);

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

        // TODO: This doesn't seem quite right...
        int[][] result = new int[height][width];
        if (hasAlphaChannel) {
            System.err.println("Alpha channel image...");
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
            System.err.println("Non-alpha channel image...");
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
                int rgb = image.getRGB(col, row);
                Color c = new Color(rgb);
                if (c.getRed() < 20) {
                    result[row][col] = 0;
                } else {
                    result[row][col] = 255;
                }
            }
        }

        return result;
    }

    public static void writeThumbnail(BufferedImage source, String dest, int dstWidth)
    {
        float xScale = (float) dstWidth / (float)source.getWidth();
        int dstHeight = (int)(source.getHeight() * xScale);
        float yScale = (float)dstHeight / (float) source.getHeight();


        try {
            LOG.debug("Writing thumbnail: " + dest + " of size: " + dstWidth + "x" + dstHeight);
            File outputFile = new File(dest);
            RenderedOp renderedOp = ScaleDescriptor.create(source, new Float(xScale), new Float(yScale),
                    new Float(0.0f), new Float(0.0f), Interpolation.getInstance(Interpolation.INTERP_BICUBIC), null);
            ImageIO.write(renderedOp.getAsBufferedImage(), THUMBNAIL_FORMAT, outputFile);
            LOG.debug("Done!");

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

}

