package net.johnewart.rivertools.analysis.tests;

import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import net.johnewart.rivertools.analysis.ElevationMapBuilder;
import net.johnewart.rivertools.core.AerialMap;
import net.johnewart.rivertools.core.ChannelMap;
import net.johnewart.rivertools.core.ChannelWidthMap;
import net.johnewart.rivertools.core.Simulation;
import net.johnewart.rivertools.factories.SimulationFactory;
import net.johnewart.rivertools.utils.ImageTools;
import org.junit.Before;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: jewart
 * Date: 12/29/12
 * Time: 10:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class ElevationMapTest {
    List<String> lidarTileList = ImmutableList.of("/Users/jewart/Dev/0716n0440e5k.las");
    Simulation simulation = mock(Simulation.class);
    ChannelWidthMap channelWidthMap = new ChannelWidthMap();
    ChannelMap channelMap = new ChannelMap();
    AerialMap aerialMap = mock(AerialMap.class);

    @Before
    public void initialize() {
        simulation.lidar_tile_files = lidarTileList;
        simulation.start_point = "POINT (-120.7119331161800062 37.4108944808790014)";
        simulation.end_point = "POINT (-120.7273075382500025 37.4101327335189993)";

        GeometryFactory gf = new GeometryFactory();
        Point startPoint = gf.createPoint( new Coordinate(-120.7119331161800062,37.4108944808790014 ) );
        Point endPoint = gf.createPoint( new Coordinate(-120.7273075382500025,37.4101327335189993) );

        when(simulation.getChannelWidthMap()).thenReturn(channelWidthMap);
        when(simulation.getChannelMap()).thenReturn(channelMap);
        when(simulation.getAerialMap()).thenReturn(aerialMap);
        when(simulation.getStartPoint()).thenReturn(startPoint);
        when(simulation.getEndPoint()).thenReturn(endPoint);

        aerialMap.setFilename("/Users/jewart/Dev/aerial_map.tiff");
        channelMap.setFilename("/Users/jewart/Dev/channel_map.tiff");
        channelWidthMap.setFilename("/Users/jewart/Dev/channel_width_map.tiff");
    }


    @Test
    public void calculatesElevation() throws Exception {
        ElevationMapBuilder elevationMapBuilder = new ElevationMapBuilder(simulation);
        Double elevation = elevationMapBuilder.getElevation(-120.3456,37.2345);
        assertThat("Elevation is correctly fetched from the USGS",
                elevation,
                is(212.110809766714));
    }

    @Test
    public void loadsLidarData() throws Exception {


        ElevationMapBuilder elevationMapBuilder = new ElevationMapBuilder(simulation);
        BufferedImage elevationMap = elevationMapBuilder.process();
        //ImageTools.writeGeoTiffWithCoordinates("input.tif", elevationMap, "output.tif");

        System.out.println("Loaded.");

    }
}
