package net.johnewart.rivertools.utils.tests;

import net.johnewart.rivertools.utils.LASReader;
import org.geotools.referencing.CRS;
import org.junit.Before;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: jewart
 * Date: 12/9/12
 * Time: 1:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class LASReaderTest {

    private LASReader lasFile;

    @Before
    public void initialize()
    {
        String testfile = getClass().getClassLoader().getResource("srs.las").getFile();
        lasFile = new LASReader(testfile);
    }

    @Test
    public void readsLASFile() throws Exception {
        System.err.println("Min X: " + lasFile.getMinX());
        System.err.println("Min Y: " + lasFile.getMinY());
        System.err.println("Min Z: " + lasFile.getMinZ());
        for(LASReader.PointRecord pr : lasFile.readAllPointRecords())
        {
            System.err.println(pr.getX() + "," + pr.getY() + "," + pr.getZ());
        }

        lasFile.transform(CRS.decode("EPSG:4326"));
    }
}
