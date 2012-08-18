import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Color;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.sun.media.jai.codec.*;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;

import com.tomgibara.imageio.tiff.*; 

import com.jhlabs.image.DespeckleFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RiverExtractor { 

  final static Logger logger = LoggerFactory.getLogger(RiverExtractor.class);
	private File tile; 

  public RiverExtractor(File tile)
  {
    this.tile = tile; 
    IIORegistry registry = IIORegistry.getDefaultInstance(); 
    registry.registerServiceProvider(new com.tomgibara.imageio.impl.tiff.TIFFImageWriterSpi());
    registry.registerServiceProvider(new com.tomgibara.imageio.impl.tiff.TIFFImageReaderSpi());
  }

  public BufferedImage extractChannels()
  {
    if (tile != null) 
    {
      try { 
        String fname = tile.getAbsolutePath();
        logger.debug("Opening File: " + fname);
        SeekableStream s = new FileSeekableStream(tile);

        BufferedImage bi = ImageIO.read(new File(fname));
        BufferedImage dest = new BufferedImage(bi.getWidth(),bi.getHeight(),BufferedImage.TYPE_BYTE_GRAY);

				// Generate a grayscale image that doesn't require an AWT display
				for(int y = 0; y < bi.getHeight(); y++){
	        for(int x = 0; x < bi.getWidth(); x++){
						Color c = new Color(bi.getRGB(x,y) & 0x00ffffff);
						dest.setRGB(x, y, c.getRGB());
					}
				}

        ByteArrayOutputStream ostr = new ByteArrayOutputStream(); 
        ImageIO.write(dest, "tiff", ostr);
        int width = bi.getWidth(); 
        int height = bi.getHeight(); 
        int headersize = ostr.size() - (width*height);

        byte[] tiff_data = ostr.toByteArray();
        byte[] tiff_header = Arrays.copyOfRange(tiff_data, 0, headersize);
        byte[] data = Arrays.copyOfRange(tiff_data, headersize, tiff_data.length);
        double[] entropy = new double[data.length];
        int[] output = new int[data.length];


        logger.debug("Length: " + data.length);
        logger.debug("Width: " + bi.getWidth()); 
        logger.debug("Height: " + bi.getHeight()); 
        logger.debug("Header size: " + headersize); 


        double max_entropy = -99999; 

        for(int y = 1; y < height-1; y++) 
        {
          for (int x = 1; x < width-1; x++) 
          {
            int offset = y * width + x;
            double[] hist = new double[256];

            for (int yoff = -1; yoff <= 1; yoff++) 
            {
              for (int xoff = -1; xoff <= 1; xoff++) 
              {
                int px = data[offset + (width * yoff) + xoff] + 128; 
                hist[px] += 1; 
              }
            } 

            for (int yoff = -1; yoff <= 1; yoff++) 
            {
              for (int xoff = -1; xoff <= 1; xoff++) 
              {
                int px = data[offset + (width * yoff) + xoff] + 128; 
                hist[px] /= 9; 
              }
            } 

            double h = 0; 
            for(double pXi : hist)
            {
              if (pXi > 0)
              {
                h -= (pXi) * (Math.log(pXi) / Math.log(2));
              }
            }

            entropy[offset] = h; 

            if (h > max_entropy)
            { max_entropy = h; }

          }
        }

        logger.debug("Max entropy: " + max_entropy);
        double scalefactor = 255.0 / max_entropy;
        logger.debug("Scale: " + scalefactor);
        logger.debug("E Length: " + entropy.length);

        double point; 
        for( int i = 0; i < entropy.length;  i++) 
        {
          point = entropy[i];
          int outputvalue = (int)(point * scalefactor);
          if (outputvalue > 128)
          {
            output[i]  = 65535; 
          } else { 
            output[i] = 0; 
          }

        }

				/* Iterate over each pixel again, checking the NxN grid around it
				 * and change the color to that of the majority of the box
				 *
				 * cellwidth = N
				 */

				int cellwidth = 5;
				int inset = cellwidth / 2;
				double threshold = 0.25;
				double hithreshold = ((cellwidth * cellwidth)* (1-threshold)) * 65535;
				double lothreshold = ((cellwidth * cellwidth)* (threshold)) * 65535;
				for (int x = inset; x < bi.getWidth() - inset; x++) 
				{
					for (int y = inset; y < bi.getHeight() - inset; y++) 
					{
            int offset = y * width + x;
						long sum = 0;
						for (int yoff = -inset; yoff <= inset; yoff++) 
            {
              for (int xoff = -inset; xoff <= inset; xoff++) 
              {
                sum += output[offset + (width * yoff) + xoff];
              }
            } 

						if(output[offset] == 0 && sum >= hithreshold)
						{
							output[offset] = 65535;
						} 
						
						if(output[offset] == 65535 && sum <= lothreshold) {
							output[offset] = 0;
						}
					}
				}

        BufferedImage outimage = ImageTools.byteToBuffered(output, width, height);
        //BufferedImage filtered = ImageTools.deepCopy(outimage);

				int w = outimage.getWidth();
				int h = outimage.getHeight();
				BufferedImage filtered = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

				for(int row = 0; row < h; row++)
				{
					for(int col = 0; col < w; col++) 
					{
						Color c = new Color(outimage.getRGB(col,row) & 0x00ffffff);
						filtered.setRGB(col, row, c.getRGB());
					}
				}


				/*
        DespeckleFilter df = new DespeckleFilter(); 
       
        logger.debug("Filtering..."); 
        for(int i = 1; i <= 10; i++) 
        {
          logger.debug(i + "...");
          df.filter(filtered, filtered);
        }*/

        return filtered; 
      } catch (IOException ioe) { 
        logger.debug("Error processing tile: " + ioe.toString());
        return null;
      }
    } else { 
      logger.debug("The tile is null...");
      return null;
    }
    
  }
}
