import org.gearman.Gearman;
import org.gearman.GearmanFunction;
import org.gearman.GearmanFunctionCallback;
import org.gearman.GearmanServer;
import org.gearman.GearmanWorker;
import org.json.simple.*;
import java.io.*; 
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class RiverExtractionWorker { 
  public RiverExtractionWorker() {
      Gearman gearman = Gearman.createGearman();
      GearmanServer server = gearman.createGearmanServer( "localhost", 4730);
      GearmanWorker worker = gearman.createGearmanWorker();
      worker.addFunction("channel_image", new ChannelFunc());
      worker.addServer(server);
  }
}

class ChannelFunc implements GearmanFunction {

  public byte[] work(String function, byte[] data, GearmanFunctionCallback callback) throws Exception {
    String jsonData = new String(data);
    JSONObject run_params = (JSONObject)JSONValue.parse(jsonData);
    String geotiff_image = (String)run_params.get("geotiff_image");
    String channel_image = (String)run_params.get("channel_image");
    JSONArray ortho_tiles = (JSONArray)run_params.get("ortho_tiles");
    System.out.println("Processing job!");

    for(int i = 0; i < ortho_tiles.size(); i++)
    {
        String tile = (String)ortho_tiles.get(i);
        System.out.println("Processing tile #" + i + ": " + tile);
        String ortho_tile = tile + ".tif";
        String channel_tile =  tile + "-channels.tif";
        try{
          File in = new File(ortho_tile);
          File out = new File(channel_tile);
          System.out.println("In: " + in + " Out: " + out);
          RiverExtractor re = new RiverExtractor(in);
          BufferedImage img = re.extractChannels();
          ImageIO.write(img, "tiff", out);

        } catch (Exception e) {
          System.out.println("Couldn't process file: " + e.toString());
          e.printStackTrace();
        }

    }

    return data;

  } 
}
