package net.johnewart.rivertools.functions;

import net.johnewart.rivertools.core.Simulation;
import net.johnewart.rivertools.factories.SimulationFactory;
import net.johnewart.rivertools.utils.ImageTools;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.gearman.common.interfaces.GearmanFunction;
import org.gearman.common.interfaces.GearmanFunctionCallback;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created with IntelliJ IDEA.
 * User: jewart
 * Date: 11/17/12
 * Time: 11:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class ElevationFunc implements GearmanFunction {
    public byte[] work(String function, byte[] data, GearmanFunctionCallback callback) throws Exception {
        try {
            String jsonData = new String(data);
            JSONObject run_params = (JSONObject) JSONValue.parse(jsonData);
            Long simulationId = (Long)run_params.get("simulation_id");
            Simulation simulation = SimulationFactory.loadFromAPI(simulationId);

            return "OK".getBytes();
        }catch (Exception e) {
            System.err.println("Error processing width: " + e.toString());
            e.printStackTrace();
            return "Error".getBytes();
        }
    }


}
