import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.*;
import java.util.Arrays;

import javax.imageio.*;
import javax.media.jai.widget.ScrollingImagePanel;
import javax.media.jai.NullOpImage;
import javax.media.jai.OpImage;

import com.jhlabs.image.*;

import com.sun.media.jai.codec.SeekableStream;
import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.TIFFDecodeParam;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.ImageCodec;

public class RiverExtractor { 

  private String tilePath, outputPath; 

  public RiverExtractor(String tilePath, String outputPath)
  {
    this.tilePath = tilePath; 
    this.outputPath = outputPath; 
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

  public void extractChannels(String tilename)
  {
      try { 
        File file = new File(this.tilePath + "/" + tilename);
        SeekableStream s = new FileSeekableStream(file);

        TIFFDecodeParam param = null;

        ImageDecoder dec = ImageCodec.createImageDecoder("tiff", s, param);

        System.out.println("Number of images in this TIFF: " + dec.getNumPages());
        BufferedImage bi = ImageIO.read(file);
        BufferedImage dest = new BufferedImage(bi.getWidth(),bi.getHeight(),BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2 = dest.createGraphics();
        g2.drawImage(bi, 0, 0, null);

        /*
        ColorConvertOp op = new ColorConvertOp(
                          bi.getColorModel().getColorSpace(),
                          dest.getColorModel().getColorSpace(),null);
        op.filter(bi,dest);
        */

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

        ImageIO.write(outimage, "tiff", new File("entropy.tif")); 
        ImageIO.write(filtered, "tiff", new File("entropy-filtered.tif")); 

      } catch (IOException ioe) { 
        System.out.println("oops!");
      }
    
  }
}
