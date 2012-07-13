import java.io.*; 
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

public class Extractor {
	public static void main(String[] args)
	{
		if(args.length >= 2) 
		{
			if (args[0] != "worker")
			{
				try{
					File in = new File(args[0]);
					File out = new File(args[1]);
          System.out.println("In: " + in + " Out: " + out);
					RiverExtractor re = new RiverExtractor(in);
					BufferedImage img = re.extractChannels();
					ImageIO.write(img, "tiff", out);  
				
				} catch (Exception e) { 
					System.out.println("Couldn't process file: " + e.toString()); 
          e.printStackTrace();
				}
			}
			
		} else { 
			System.out.println("You need to provide a tile file name");
		}
	}
}
