package net.johnewart.rivertools.factories.tests;

import net.johnewart.rivertools.core.AerialMap;
import net.johnewart.rivertools.core.ChannelMap;
import net.johnewart.rivertools.core.ChannelWidthMap;
import net.johnewart.rivertools.core.Simulation;
import net.johnewart.rivertools.factories.SimulationFactory;
import net.johnewart.rivertools.utils.LASReader;
import org.geotools.referencing.CRS;
import org.junit.Before;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: jewart
 * Date: 12/29/12
 * Time: 8:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimulationTest {
    @Before
    public void initialize()
    {
    }

    @Test
    public void readsLASFile() throws Exception {
        Simulation simulation = SimulationFactory.loadFromAPI(37);
        ChannelWidthMap channelWidthMap = simulation.getChannelWidthMap();
        AerialMap aerialMap = simulation.getAerialMap();
        ChannelMap channelMap = simulation.getChannelMap();
    }
}
