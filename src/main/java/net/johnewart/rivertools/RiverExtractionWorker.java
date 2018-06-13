package net.johnewart.rivertools;

import net.johnewart.gearman.client.NetworkGearmanWorker;
import net.johnewart.gearman.common.interfaces.GearmanWorker;
import net.johnewart.gearman.net.Connection;
import net.johnewart.rivertools.functions.*;

import java.io.*;


public class RiverExtractionWorker {

    public RiverExtractionWorker() {
        System.err.println("Starting worker!");
        try {
            Connection conn = new Connection("localhost", 4730);
            GearmanWorker worker = new NetworkGearmanWorker.Builder().withConnection(conn).build();
            worker.registerCallback("channel_image", new ChannelFunc());
            worker.registerCallback("channel_width", new WidthFunc());
            worker.registerCallback("geocode_image", new GeoTiffFunc());
            worker.registerCallback("elevation_profile", new ElevationFunc());
        } catch (IOException ioe) {
            System.err.println(ioe.toString());
        }

    }
}

