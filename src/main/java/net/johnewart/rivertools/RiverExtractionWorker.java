package net.johnewart.rivertools;

import net.johnewart.rivertools.functions.*;
import org.gearman.client.GearmanServer;
import org.gearman.client.GearmanWorker;
import org.gearman.common.GearmanFactory;

import java.io.*;


public class RiverExtractionWorker {

    public RiverExtractionWorker() {
        System.err.println("Starting worker!");
        try {
            GearmanFactory gearmanFactory = new GearmanFactory();
            GearmanServer server = gearmanFactory.createGearmanServer("127.0.0.1", 4730);
            GearmanWorker worker = gearmanFactory.createGearmanWorker();
            worker.addFunction("channel_image", new ChannelFunc());
            worker.addFunction("channel_width", new WidthFunc());
            worker.addFunction("geocode_image", new GeoTiffFunc());
            worker.addFunction("elevation_profile", new ElevationFunc());
            worker.addServer(server);
        } catch (IOException ioe) {
            System.err.println(ioe.toString());
        }

    }
}

