package net.johnewart.rivertools.utils.tests;

import net.johnewart.rivertools.utils.LASReader;
import net.johnewart.rivertools.utils.QuadTree;
import org.junit.Before;
import org.junit.Test;

import java.awt.geom.Point2D;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * User: jewart
 * Date: 1/19/13
 * Time: 11:23 AM
 */
public class QuadTreeTest {
    private LASReader lasFile;

    @Before
    public void initialize()
    {
       String testfile = getClass().getClassLoader().getResource("srs.las").getFile();
       lasFile = new LASReader(testfile);
    }

    @Test
    public void storesAndFindsNearestThingsInSameNode() throws Exception {
        Point2D.Double topRight = new Point2D.Double(0,500);
        Point2D.Double bottomLeft = new Point2D.Double(500,0);

        QuadTree<String> quadTree = new QuadTree<String>(topRight, bottomLeft);

        // Points in space
        Point2D.Double p1 = new Point2D.Double(100, 100);
        Point2D.Double p2 = new Point2D.Double(50, 100);
        Point2D.Double p3 = new Point2D.Double(200, 100);
        Point2D.Double p4 = new Point2D.Double(300, 400);
        Point2D.Double p5 = new Point2D.Double(1, 4);


        quadTree.insert(p1, "Hello from " + p1.toString());
        quadTree.insert(p2, "Hello from " + p2.toString());
        quadTree.insert(p3, "Hello from " + p3.toString());
        quadTree.insert(p4, "Hello from " + p4.toString());
        quadTree.insert(p5, "Hello from " + p5.toString());

        String result = quadTree.findNearestTo(new Point2D.Double(0,0));
        assertEquals(result, "Hello from " + p5.toString());
    }
    @Test
    public void storesAndFindsNearestThingsInNeighborNodes() throws Exception {
        Point2D.Double topRight = new Point2D.Double(0,100);
        Point2D.Double bottomLeft = new Point2D.Double(100,0);

        QuadTree<String> quadTree = new QuadTree<String>(topRight, bottomLeft);

        // Points in space, clustered in northwest but not in the northwest-most quadrant
        // northwest-most will be (0,100) (25,75)
        // with neighbors of (25,100)(50,75), (25,75)(50,50), (0, 75)(25,50)
        Point2D.Double p1 = new Point2D.Double(26, 90);
        Point2D.Double p2 = new Point2D.Double(10, 50);
        Point2D.Double p3 = new Point2D.Double(30, 80);
        Point2D.Double p4 = new Point2D.Double(100, 400);
        Point2D.Double p5 = new Point2D.Double(1, 4);


        quadTree.insert(p1, "Hello from " + p1.toString());
        quadTree.insert(p2, "Hello from " + p2.toString());
        quadTree.insert(p3, "Hello from " + p3.toString());
        quadTree.insert(p4, "Hello from " + p4.toString());
        quadTree.insert(p5, "Hello from " + p5.toString());

        // Look for something in the far-northwest, should find a point in one of the neighboring nodes
        String result = quadTree.findNearestTo(new Point2D.Double(10,90));
        // Nearest should be (26,90) (16 units away on the x-axis) which is p1
        // p2 has the same x but is 40 units away on the y-axis
        assertEquals(result, "Hello from " + p1.toString());

        // Try something a little closer to p2 / p3, p3 should be closer (d = 20.4 vs 26)
        result = quadTree.findNearestTo(new Point2D.Double(10,76));
        assertEquals(result, "Hello from " + p3.toString());
    }

}
