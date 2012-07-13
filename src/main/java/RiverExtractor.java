import java.awt.Graphics2D;
import java.awt.Point;
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

import javax.imageio.ImageIO;
import com.sun.media.jai.codec.*;

import com.jhlabs.image.DespeckleFilter;

public class RiverExtractor { 

  private File tile; 

  public RiverExtractor(File tile)
  {
    this.tile = tile; 
    IIORegistry registry = IIORegistry.getDefaultInstance(); 
    registry.registerServiceProvider(new com.sun.media.imageioimpl.plugins.tiff.TIFFImageWriterSpi());
    registry.registerServiceProvider(new com.sun.media.imageioimpl.plugins.tiff.TIFFImageReaderSpi());
  }

  private BufferedImage deepCopy(BufferedImage bi) {
     ColorModel cm = bi.getColorModel();
     boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
     WritableRaster raster = bi.copyData(null);
     return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
  }

  private BufferedImage byteToBuffered(int[] pixels, int width, int height) throws IllegalArgumentException {
     DataBufferInt dbuf = new DataBufferInt(pixels, width * height);
     BandedSampleModel model = new BandedSampleModel(DataBuffer.TYPE_USHORT, width, height, 1);
     WritableRaster raster = Raster.createWritableRaster(model, dbuf, new Point(0,0));
     BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
     image.setData(raster);
     return image;
  }

  public BufferedImage extractChannels()
  {
    if (tile != null) 
    {
      try { 
        String fname = tile.getAbsolutePath();
        System.out.println("Opening File: " + fname);
        SeekableStream s = new FileSeekableStream(tile);
        String[] formats = ImageIO.getReaderFormatNames(); 
        for(int i = 0; i < formats.length; i++) 
        {
          System.out.println(formats[i]);
        }

        TIFFDecodeParam param = null;
        ImageDecoder dec = ImageCodec.createImageDecoder("tiff", s, param);
        System.out.println("Number of images in this TIFF: " + dec.getNumPages());
        BufferedImage bi = ImageIO.read(new File(fname));
        System.out.println("Buffered image: " + bi);
        BufferedImage dest = new BufferedImage(bi.getWidth(),bi.getHeight(),BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2 = dest.createGraphics();
        g2.drawImage(bi, 0, 0, null);

        ByteArrayOutputStream ostr = new ByteArrayOutputStream(); 
        ImageIO.write(dest, "tiff", new File("export.tif")); 
        ImageIO.write(dest, "tiff", ostr);
        int width = bi.getWidth(); 
        int height = bi.getHeight(); 
        int headersize = ostr.size() - (width*height);

        byte[] tiff_data = ostr.toByteArray();
        byte[] tiff_header = Arrays.copyOfRange(tiff_data, 0, headersize);
        byte[] data = Arrays.copyOfRange(tiff_data, headersize, tiff_data.length);
        double[] entropy = new double[data.length];
        int[] output = new int[data.length];


        System.out.println("Length: " + data.length);
        System.out.println("Width: " + bi.getWidth()); 
        System.out.println("Height: " + bi.getHeight()); 
        System.out.println("Header size: " + headersize); 


        double max_entropy = -99999; 

        for(int y = 1; y < height-1; y++) 
        {
          System.out.printf("%d done...\n", y);
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

        System.out.println("Max entropy: " + max_entropy);
        double scalefactor = 255.0 / max_entropy;
        System.out.println("Scale: " + scalefactor);
        System.out.println("E Length: " + entropy.length);

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

        BufferedImage outimage = byteToBuffered(output, width, height);
        BufferedImage filtered = deepCopy(outimage);
        DespeckleFilter df = new DespeckleFilter(); 
       
        System.out.println("Filtering..."); 
        for(int i = 1; i <= 10; i++) 
        {
          System.out.println(i + "...");
          df.filter(filtered, filtered);
        }

        return filtered; 
      } catch (IOException ioe) { 
        System.out.println("oops!");
        return null;
      }
    } else { 
      System.out.println("The tile is null...");
      return null;
    }
    
  }
}
