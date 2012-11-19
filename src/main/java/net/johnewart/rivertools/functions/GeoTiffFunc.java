package net.johnewart.rivertools.functions;

import net.johnewart.rivertools.ImageTools;
import net.johnewart.rivertools.RiverWidth;
import org.gearman.common.interfaces.GearmanFunction;
import org.gearman.common.interfaces.GearmanFunctionCallback;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: jewart
 * Date: 11/17/12
 * Time: 11:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class GeoTiffFunc implements GearmanFunction {
    public byte[] work(String function, byte[] data, GearmanFunctionCallback callback) throws Exception {
        try {
            String jsonData = new String(data);
            JSONObject run_params = (JSONObject) JSONValue.parse(jsonData);
            String geotiff_image = (String) run_params.get("geotiff_image");
            String source_image = (String) run_params.get("source_image");
            String destination_image = (String) run_params.get("destination_image");
            System.err.println("Processing " + source_image + " to add geocoding from " + geotiff_image + " and writing to " + destination_image);

            BufferedImage image = ImageIO.read(new File(source_image));

            boolean success = ImageTools.writeGeoTiffWithCoordinates(geotiff_image, image, destination_image);
            System.err.println("Success: "  + success);

            return "OK".getBytes();
        }catch (Exception e) {
            System.err.println("Error proccessing width: " + e.toString());
            e.printStackTrace();
            return "Error".getBytes();
        }
    }
}
