package net.johnewart.rivertools.functions;

import com.jhlabs.image.BlurFilter;
import com.jhlabs.image.DespeckleFilter;
import net.johnewart.rivertools.ImageTools;
import net.johnewart.rivertools.RiverExtractor;
import org.gearman.common.interfaces.GearmanFunction;
import org.gearman.common.interfaces.GearmanFunctionCallback;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: jewart
 * Date: 11/17/12
 * Time: 11:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChannelFunc implements GearmanFunction {

    final static Logger logger = LoggerFactory.getLogger(ChannelFunc.class);

    public byte[] work(String function, byte[] data, GearmanFunctionCallback callback) throws Exception {
        try {
            String jsonData = new String(data);
            JSONObject run_params = (JSONObject) JSONValue.parse(jsonData);
            String geotiff_image = (String) run_params.get("geotiff_image");
            String channel_image = (String) run_params.get("channel_image");
            String tile_path = (String) run_params.get("tile_path");
            JSONArray ortho_tiles = (JSONArray) run_params.get("ortho_tiles");

            int tile_width = ((Long) run_params.get("tile_width")).intValue();
            int tile_height = ((Long) run_params.get("tile_height")).intValue();

            System.err.println("Processing job!");


            String pattern = "(\\d+)n(\\d+)e.*";
            Pattern tile_regex = Pattern.compile(pattern);

            Vector<Integer> easts = new Vector<Integer>();
            Vector<Integer> norths = new Vector<Integer>();

            for (int i = 0; i < ortho_tiles.size(); i++) {
                String tile = (String) ortho_tiles.get(i);
                Matcher m = tile_regex.matcher(tile);
                if (m.find()) {
                    norths.add(Integer.parseInt(m.group(1)));
                    easts.add(Integer.parseInt(m.group(2)));
                }

            }

            int max_north = Collections.max(norths);
            int min_north = Collections.min(norths);
            int max_east = Collections.max(easts);
            int min_east = Collections.min(easts);

            System.err.println("East: " + min_east + " -> " + max_east);
            System.err.println("North: " + min_north + " -> " + max_north);

            // create the new image, canvas size is the max. of both image sizes
            int w = (tile_width * ((max_east - min_east) + 1));
            int h = (tile_height * ((max_north - min_north) + 1));
            BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

            for (int i = 0; i < ortho_tiles.size(); i++) {
                String tile = (String) ortho_tiles.get(i);
                String ortho_tile = tile_path + "/" + tile + ".tif";
                System.err.println("Processing tile #" + i + ": " + ortho_tile);

                callback.sendStatus(i, ortho_tiles.size());

                try {
                    File in = new File(ortho_tile);
                    RiverExtractor re = new RiverExtractor(in);
                    BufferedImage img = re.extractChannels();

                    Matcher m = tile_regex.matcher(tile);
                    // TODO: Fix this, it's probably really slow.... :(
                    if (m.find()) {
                        int north = Integer.parseInt(m.group(1));
                        int east = Integer.parseInt(m.group(2));
                        int xorigin = Math.abs(min_east - east) * tile_height;
                        int yorigin = Math.abs(max_north - north) * tile_height;
                        System.out.println("Origin X: " + xorigin + " Y: " + yorigin);

                        for (int y = 0; y < img.getHeight(); y++) {
                            int pixel_y = yorigin + y;
                            for (int x = 0; x < img.getWidth(); x++) {
                                int pixel_x = xorigin + x;
                                try {
                                    Color c = new Color(img.getRGB(x, y) & 0x00ffffff);
                                    combined.setRGB(pixel_x, pixel_y, c.getRGB());
                                } catch (ArrayIndexOutOfBoundsException ex) {
                                    System.out.println("Failed to set (" + pixel_x + ", " + pixel_y + ")");
                                    return "oops".getBytes();
                                }
                            }
                        }
                    }

                    System.err.println("Done!");

                } catch (Exception e) {
                    System.err.println("Couldn't process file: " + e.toString());
                    e.printStackTrace();
                }

            }

            BlurFilter bf = new BlurFilter();

            System.err.println("Blurring 3x3...");
            bf.filter(combined, combined);

            //ImageIO.write(combined, "TIF", new File(channel_image));
            ImageTools.writeGeoTiffWithCoordinates(geotiff_image, combined, channel_image);
            return channel_image.getBytes();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return "Error".getBytes();
        }

    }
}
