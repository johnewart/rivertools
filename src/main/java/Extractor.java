import java.io.*; 
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import com.sun.media.jai.codec.*;

import com.tomgibara.imageio.tiff.*; 

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureWriter;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.geometry.MismatchedDimensionException;
import org.geotools.referencing.CRS;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.FactoryException;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.opengis.geometry.Envelope;
import org.geotools.geometry.Envelope2D;

public class Extractor {
	public static void main(String[] args)
	{
		try { 
			// Load up TIFF reader / writer
			IIORegistry registry = IIORegistry.getDefaultInstance(); 
			registry.registerServiceProvider(new com.tomgibara.imageio.impl.tiff.TIFFImageWriterSpi());
			registry.registerServiceProvider(new com.tomgibara.imageio.impl.tiff.TIFFImageReaderSpi());

			String coordsfilename = args[0];	
			String infilename = args[1];
			String outfilename = args[2];

			System.out.println("Getting coordinates from: " + coordsfilename + "...");
			GeoTiffReader gr = new GeoTiffReader(new File(coordsfilename), null);
			GridCoverage2D coverage = gr.read(null);
			CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem2D();
			Envelope2D env = coverage.getEnvelope2D();

			System.out.println("Opening: " + infilename + "...");
			BufferedImage img = ImageIO.read(new File(infilename));

			GridCoverageFactory fac = CoverageFactoryFinder.getGridCoverageFactory(null);
			GridCoverage2D gridCoverage = fac.create("Foobar", img, env);
			
			System.out.println("Writing: " + outfilename + "...");
			GeoTiffWriter gw = new GeoTiffWriter(new File(outfilename));
			gw.write(gridCoverage, null);

		} catch (Exception e) { 
			System.out.println(e.toString());
		}

	}
	public static void rain(String[] args)
	{
		if(args.length >= 2) 
		{
			if (!"worker".equals(args[0]))
			{
				if ("width".equals(args[0]))
				{
					try { 
						File in = new File(args[1]);
						File out = new File(args[2]);
						int naturalWidth = Integer.parseInt(args[3]); 
						int naturalHeight = Integer.parseInt(args[4]);
						int startx = Integer.parseInt(args[5]);
						int starty =	Integer.parseInt(args[6]);
						float xorigin = (float)startx / (float)naturalWidth; 
						float yorigin = (float)starty / (float)naturalHeight; 
						RiverWidth rw = new RiverWidth(in);
						BufferedImage img = rw.computeWidth(xorigin, yorigin);
						ImageIO.write(img, "tiff" ,out);
					} catch (Exception e) { 
						System.out.println("Couldn't process file: " + e.toString()); 
						e.printStackTrace();
					}
				} else {
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
			} 
			
		} else { 
			if("worker".equals(args[0]))
			{
				RiverExtractionWorker worker = new RiverExtractionWorker();
			} else {
				System.out.println("You need to provide a tile file name");
			}
		}
	}
}
