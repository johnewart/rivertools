package net.johnewart.rivertools.core;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import net.johnewart.rivertools.factories.AerialMapFactory;
import net.johnewart.rivertools.factories.ChannelMapFactory;
import net.johnewart.rivertools.factories.ChannelWidthMapFactory;
import org.codehaus.jackson.annotate.JsonProperty;
import org.geotools.geometry.jts.JTSFactoryFinder;

import java.util.List;

/**
 * Butcher-ish way to get data from RiverSim for the Simulation without having to write a proper class...
 */
public class Simulation {
    @JsonProperty
    public Long id;

    @JsonProperty
    public List<String> ortho_tile_files;

    @JsonProperty
    public List<String> lidar_tile_files;

    @JsonProperty
    public Long aerial_map_id;

    @JsonProperty
    public Long channel_width_map_id;

    @JsonProperty
    public Long channel_map_id;


    @JsonProperty
    public Long aerialmap_height;

    @JsonProperty
    public Long aerialmap_width;

    @JsonProperty
    public String bbox;

    @JsonProperty
    public boolean channel_tile_job_complete;

    @JsonProperty
    public String channel_tile_job_handle;

    @JsonProperty
    public boolean channel_width_job_complete;

    @JsonProperty
    public String channel_width_job_handle;

    @JsonProperty
    public Long channel_width_natural_height;

    @JsonProperty
    public Long channel_width_natural_width;

    @JsonProperty
    public String channel_width_points;

    @JsonProperty
    public Float channel_width_x_origin;

    @JsonProperty
    public Float channel_width_y_origin;

    @JsonProperty
    public String description;

    @JsonProperty
    public boolean elevation_map_job_complete;

    @JsonProperty
    public String elevation_map_job_handle;

    @JsonProperty
    public Float end_elevation;

    @JsonProperty
    public String end_point;

    @JsonProperty
    public String name;

    @JsonProperty
    public String region;

    @JsonProperty
    public String resource_uri;

    @JsonProperty
    public Float start_elevation;

    @JsonProperty
    public String start_point;

    private ChannelWidthMap channelWidthMap;
    private ChannelMap channelMap;
    private AerialMap aerialMap;
    private GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
    private WKTReader reader = new WKTReader(geometryFactory);

    public ChannelWidthMap getChannelWidthMap() {
        if(channelWidthMap == null)
        {
            channelWidthMap = ChannelWidthMapFactory.loadFromAPI(this.channel_width_map_id);
        }

        return channelWidthMap;
    }

    public ChannelMap getChannelMap() {
        if(channelMap == null)
        {
            channelMap = ChannelMapFactory.loadFromAPI(this.channel_map_id);
        }

        return channelMap;
    }

    public AerialMap getAerialMap() {
        if(aerialMap == null)
        {
           aerialMap = AerialMapFactory.loadFromAPI(this.aerial_map_id);
        }

        return aerialMap;
    }

    public Point getEndPoint() {
        try {
            Point startPoint = (com.vividsolutions.jts.geom.Point) reader.read(end_point);
            System.err.println("End: " + startPoint);
            return startPoint;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Point getStartPoint() {
        try {
            Point startPoint = (com.vividsolutions.jts.geom.Point) reader.read(start_point);
            System.err.println("Start: " + startPoint);
            return startPoint;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}
