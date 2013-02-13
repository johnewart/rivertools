package net.johnewart.rivertools.functions;

import com.jhlabs.image.BlurFilter;
import net.johnewart.rivertools.core.Simulation;
import net.johnewart.rivertools.factories.SimulationFactory;
import net.johnewart.rivertools.utils.ImageTools;
import net.johnewart.rivertools.analysis.RiverExtractor;
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
public class AerialFunc implements GearmanFunction {

    final static Logger logger = LoggerFactory.getLogger(AerialFunc.class);

    public byte[] work(String function, byte[] data, GearmanFunctionCallback callback) throws Exception {
        try {
            String jsonData = new String(data);
            JSONObject run_params = (JSONObject) JSONValue.parse(jsonData);
            Long simulationId = (Long)run_params.get("simulation_id");
            Simulation simulation = SimulationFactory.loadFromAPI(simulationId);

            return "OK".getBytes();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return "Error".getBytes();
        }

    }
}
