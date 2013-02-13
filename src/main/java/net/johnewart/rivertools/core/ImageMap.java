package net.johnewart.rivertools.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.johnewart.rivertools.utils.ImageTools;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * User: jewart
 * Date: 1/20/13
 * Time: 3:12 PM
 */
public class ImageMap {
    @JsonProperty
    public String filename;

    @JsonProperty
    public String full_thumbnail_path;

    @JsonProperty
    public Integer full_thumbnail_width;

    // 2-D array of pixels in the image
    @JsonIgnore
    protected int[][] pixels;

    @JsonIgnore
    protected int width;

    @JsonIgnore
    protected int height;

    @JsonIgnore
    protected File imageFile;

    @JsonIgnore
    protected BufferedImage image;

    @JsonIgnore
    protected int blackPixel;

    // Has this been tinkered with?
    @JsonIgnore
    protected boolean dirty = false;

    @JsonIgnore
    final int tiffBlack = 16711680; // TIFF black

    @JsonIgnore
    final int pngBlack = -16777216; // PNG black

    public int[][] getPixels()
    {
        load();
        return pixels;
    }

    public void load()
    {
        load(false);
    }

    public void load(boolean force)
    {
        if(image == null || force)
        {
            IIORegistry registry = IIORegistry.getDefaultInstance();
            registry.registerServiceProvider(new com.tomgibara.imageio.impl.tiff.TIFFImageWriterSpi());
            registry.registerServiceProvider(new com.tomgibara.imageio.impl.tiff.TIFFImageReaderSpi());

            imageFile = new File(this.filename);
            String filetype = getFormatName(imageFile);

            try {
                this.image = ImageIO.read(imageFile);
                this.width = this.image.getWidth();
                this.height = this.image.getHeight();
                System.err.println("Width: " + width + " Height: " + height);

                if (filetype.equals("png")) {
                    blackPixel = pngBlack;
                    System.err.println("Processing a PNG file, black pixels are " + pngBlack);
                } else {
                    blackPixel = tiffBlack;
                    System.err.println("Processing a TIFF file, black pixels are " + tiffBlack);
                }
            } catch (IOException ioe) {
                this.image = null;
            }

            pixels = ImageTools.convertRGBTo2DBinary(this.image, this.blackPixel);
        }
    }

    protected String getFormatName(Object o) {
        try {
            ImageInputStream iis = ImageIO.createImageInputStream(o);
            Iterator iter = ImageIO.getImageReaders(iis);
            if (!iter.hasNext()) {
                return null;
            }
            ImageReader reader = (ImageReader) iter.next();
            iis.close();
            return reader.getFormatName();
        } catch (IOException e) {

        }
        return null;
    }

    public boolean isDirty() {
        return dirty;
    }

    public BufferedImage getImage() {
        load();
        return image;
    }

    public File getImageFile() {
        load();
        return imageFile;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    @JsonIgnore
    public String getFullThumbnailPath() {
        return full_thumbnail_path;
    }

    @JsonIgnore
    public Integer getFullThumbnailWidth() {
        return full_thumbnail_width;
    }

    @JsonIgnore
    public int getBlackPixel() {
        return blackPixel;
    }
}
