package net.johnewart.rivertools.functions;

import net.johnewart.rivertools.utils.ImageTools;
import net.johnewart.rivertools.RiverWidth;
import net.johnewart.rivertools.utils.ImageTools;
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
public class WidthFunc implements GearmanFunction {
    public byte[] work(String function, byte[] data, GearmanFunctionCallback callback) throws Exception {
        try {
            String jsonData = new String(data);
            JSONObject run_params = (JSONObject) JSONValue.parse(jsonData);
            String channel_image = (String) run_params.get("channel_image");
            String channel_width_image = (String) run_params.get("channel_width_image");
            Long start_x = (Long) run_params.get("start_x");
            Long start_y = (Long) run_params.get("start_y");

            File in = new File(channel_image);
            File out = new File(channel_width_image);

            System.err.println("Processing " + channel_image + " starting at (" + start_x + "," + start_y + ") and writing to " + channel_width_image + "... ");
            RiverWidth rw = new RiverWidth(in);
            System.err.println("Loaded, processing...");
            BufferedImage img = rw.computeWidth(start_x.intValue(), start_y.intValue(), callback);
            System.err.println("Writing output file....");
            //ImageIO.write(img, "tiff", out);
            ImageTools.writeGeoTiffWithCoordinates(channel_image, img, channel_width_image);

            return channel_width_image.getBytes();
        }catch (Exception e) {
            System.err.println("Error proccessing width: " + e.toString());
            return "Error".getBytes();
        }
    }
}
