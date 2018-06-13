package net.johnewart.rivertools.functions;

import net.johnewart.gearman.common.Job;
import net.johnewart.gearman.common.events.WorkEvent;
import net.johnewart.gearman.common.interfaces.GearmanFunction;
import net.johnewart.gearman.common.interfaces.GearmanWorker;
import net.johnewart.rivertools.core.ChannelMap;
import net.johnewart.rivertools.core.ChannelWidthMap;
import net.johnewart.rivertools.core.ImageMap;
import net.johnewart.rivertools.core.Simulation;
import net.johnewart.rivertools.factories.SimulationFactory;
import net.johnewart.rivertools.utils.ImageTools;
import net.johnewart.rivertools.analysis.RiverWidth;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: jewart
 * Date: 11/17/12
 * Time: 11:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class WidthFunc implements GearmanFunction {
    @Override
    public byte[] process(WorkEvent workEvent) {
        final Job job = workEvent.job;
        final GearmanWorker worker = workEvent.worker;

        byte[] data = job.getData();

        try {
            String jsonData = new String(data);
            JSONObject run_params = (JSONObject) JSONValue.parse(jsonData);
            Long simulationId = (Long)run_params.get("simulation_id");
            Simulation simulation = SimulationFactory.loadFromAPI(simulationId);
            ChannelWidthMap channelWidthMap = simulation.getChannelWidthMap();
            ChannelMap channelMap = simulation.getChannelMap();

            // This is stores as a JSON Array, parse it and convert to a set of points
            JSONArray pointsArray = (JSONArray) JSONValue.parse(channelWidthMap.channel_width_points);
            Set<Point> points = new HashSet();

            for(int i = 0; i < pointsArray.size(); i++)
            {
                JSONObject point = (JSONObject)pointsArray.get(i);
                Point p = new Point(
                        ((Long)point.get("x")).intValue(),
                        ((Long)point.get("y")).intValue()
                );
                points.add(p);
            }

            File in = new File(channelMap.filename);
            File out = new File(channelWidthMap.filename);


            System.err.println("Processing " + channelMap.filename + " starting at points (" + channelWidthMap.channel_width_points + ") and writing to " + channelWidthMap.filename + "... ");
            RiverWidth rw = new RiverWidth(channelMap, points, channelWidthMap.image_natural_width,  channelWidthMap.image_natural_height);
            System.err.println("Loaded, processing...");
            BufferedImage img = rw.computeWidth(worker);
            System.err.println("Writing output file....");
            ImageTools.writeGeoTiffWithCoordinates(channelMap.filename, img, channelWidthMap.filename);

            ImageTools.writeThumbnail(img, channelMap.getFullThumbnailPath(), channelMap.getFullThumbnailWidth());
            return channelWidthMap.filename.getBytes();
        }catch (Exception e) {
            System.err.println("Error processing width: " + e.toString());
            e.printStackTrace(System.err);
            return "Error".getBytes();
        }
    }
}
