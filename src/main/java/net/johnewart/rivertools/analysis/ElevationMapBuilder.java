package net.johnewart.rivertools.analysis;

import com.vividsolutions.jts.geom.Point;
import net.johnewart.rivertools.core.ChannelMap;
import net.johnewart.rivertools.core.ChannelWidthMap;
import net.johnewart.rivertools.core.Simulation;
import net.johnewart.rivertools.utils.LASReader;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.imageio.geotiff.TiePoint;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

public class ElevationMapBuilder {
    private Simulation simulation;
    final static Logger LOG = LoggerFactory.getLogger(ElevationMapBuilder.class);

    public ElevationMapBuilder(Simulation simulation)
    {
        this.simulation = simulation;
    }


    public BufferedImage process()
    {
        ChannelMap channelMap = simulation.getChannelMap();
        ChannelWidthMap channelWidthMap = simulation.getChannelWidthMap();

        BufferedImage result = null;
        try {
            // TODO: this needs to be applied when loading the LiDAR files, not done here...
            // <50000> +proj=utm +zone=10 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=ft +no_defs
            String customWKT = "PROJCS[ \"UTM Zone 10, Northern Hemisphere\",\n" +
                    "  GEOGCS[\"GRS 1980(IUGG, 1980)\",\n" +
                    "    DATUM[\"unknown\"," +
                    "       SPHEROID[\"GRS80\",6378137,298.257222101]," +
                    "       TOWGS84[0,0,0,0,0,0,0]" +
                    "    ],\n" +
                    "    PRIMEM[\"Greenwich\",0],\n" +
                    "    UNIT[\"degree\",0.0174532925199433]\n" +
                    "  ],\n" +
                    "  PROJECTION[\"Transverse_Mercator\"],\n" +
                    "  PARAMETER[\"latitude_of_origin\",0],\n" +
                    "  PARAMETER[\"central_meridian\",-123],\n" +
                    "  PARAMETER[\"scale_factor\",0.9996],\n" +
                    "  PARAMETER[\"false_easting\",1640419.947506562],\n" +
                    "  PARAMETER[\"false_northing\",0],\n" +
                    "  UNIT[\"Foot (International)\",0.3048]\n" +
                    "]";

            GeoTiffReader gr = new GeoTiffReader(channelWidthMap.getImageFile(), null);
            GridCoverage2D coverage = gr.read(null);
            CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem2D();
            LASReader lasReader = null;

            // Read the first for now...
            CRSFactory factory = ReferencingFactoryFinder.getCRSFactory(null);
            try {
                CoordinateReferenceSystem srcCRS = factory.createFromWKT(customWKT);
                lasReader = new LASReader(simulation.lidar_tile_files.get(0), srcCRS);
                LOG.debug("Transforming LiDAR data from " + lasReader.getCurrentCRS().toWKT() + " to " + crs.toWKT() + "...");
                lasReader.transform(crs);
                LOG.debug("Done!");
            } catch (Exception e) {
                e.printStackTrace();
            }


            double pixelWidth = gr.getMetadata().getModelPixelScales().getScaleX();
            double pixelHeight = gr.getMetadata().getModelPixelScales().getScaleY();
            TiePoint[] tiePoints = gr.getMetadata().getModelTiePoints();
            Point2D origin = new Point2D.Double(tiePoints[0].getValueAt(3), tiePoints[0].getValueAt(4));

            int[][] channelPixels = channelMap.getPixels();
            int[][] widthPixels = channelWidthMap.getPixels();
            int[][] elevationMap = widthPixels.clone();

            // Generate locality where size -> ~6ft
            LASHashTable lasHashTable = new LASHashTable(lasReader, origin, 1000);

            Point startPoint = simulation.getStartPoint();
            Point endPoint = simulation.getEndPoint();

            int startX = (int)((startPoint.getX() - origin.getX()) / pixelWidth);
            int startY = (int)(Math.abs(startPoint.getY() - origin.getY()) / pixelHeight);
            int endX   = (int)((endPoint.getX() - origin.getX()) / pixelWidth);
            int endY   = (int)(Math.abs(endPoint.getY() - origin.getY()) / pixelHeight);
            int width  = channelPixels[0].length;
            int height = channelPixels.length;

            Set<java.awt.Point> startingPoints = new HashSet<>();
            startingPoints.add(new java.awt.Point(startX, startY));
            startingPoints.add(new java.awt.Point(endX, endY));
            Set<java.awt.Point> channelPoints = channelMap.findChannelPoints(startingPoints);

            // Perform simple DFS search to find shortest path from start to end
            Deque<java.awt.Point> pointStack = new LinkedList<>();
            HashSet<java.awt.Point> visitedPoints = new HashSet<>();
            HashSet<java.awt.Point> consideredPoints = new HashSet<>();
            LinkedList<java.awt.Point> visitOrder = new LinkedList<>();

            boolean finished = false;
            int blackPixel = channelMap.getBlackPixel();
            pointStack.add(new java.awt.Point(startX, startY));

            while(!finished && !pointStack.isEmpty()) {
                java.awt.Point p = pointStack.removeFirst();

                // Add ourselves to the list of visited places
                visitOrder.addFirst(p);
                visitedPoints.add(p);

                java.awt.Point nextPoint = null;
                float lowestCost = Float.MAX_VALUE;

                if(p.getX() == endPoint.getX() && p.getY() == endPoint.getY())
                {
                    finished = true;
                } else {

                    HashMap<Float, java.awt.Point> neighbors = new HashMap<>();
                    // Augment DFS with most-likely candidate (i.e don't go backwards first)
                    for (int xoff = -1; xoff <= 1; xoff++) {
                        for (int yoff = -1; yoff <= 1; yoff++) {
                            if (!(xoff == 0 && yoff == 0)) {
                                int nx = p.x + xoff;
                                int ny = p.y + yoff;
                                // Cost is the distance from the next point to the destination * 5
                                int dX = nx - endX;
                                int dY = ny - endY;
                                float cost = (float)(dX*dX)+(dY*dY);

                                if ( (nx >= 0 && nx < width && ny >= 0 && ny < height) &&
                                     (channelPixels[ny][nx] == 0) )
                                {
                                    java.awt.Point currentPoint = new java.awt.Point(nx, ny);
                                    neighbors.put(cost, currentPoint);
                                }
                            }
                        }
                    }


                    if(neighbors.keySet().size() == 0)
                    {
                        // Nobody else to visit, end of the line
                        Set<java.awt.Point> pointNeighbors;
                        do {
                            // Back up a bit, take ourselves off the path stack
                            visitOrder.removeFirst();
                            pointNeighbors = new HashSet();
                            // See who was last on the stack
                            java.awt.Point prevPoint = visitOrder.getFirst();
                            // Get all the neighbors
                            for (int xoff = -1; xoff <= 1; xoff++) {
                                for (int yoff = -1; yoff <= 1; yoff++) {
                                    if (!(xoff == 0 && yoff == 0)) {
                                        int nx = prevPoint.x + xoff;
                                        int ny = prevPoint.y + yoff;
                                        pointNeighbors.add(new java.awt.Point(nx, ny));
                                    }
                                }
                            }
                            // Continue while the current top's neighbors are all already one's we've
                            // visited.
                        } while(visitedPoints.containsAll(pointNeighbors));
                    } else {
                        Float[] keys = neighbors.keySet().toArray(new Float[0]);
                        Arrays.sort(keys);
                        for(int j = keys.length - 1; j >= 0; j--)
                        {
                            if(!consideredPoints.contains(neighbors.get(keys[j])))
                            {
                                pointStack.addFirst(neighbors.get(keys[j]));
                                consideredPoints.add(neighbors.get(keys[j]));
                            }
                        }
                    }
                }
            }

            if(!finished)
            {
                LOG.debug("Didn't find a path from start to finish...");
            } else {
                LOG.debug("Found path: ");
                for(java.awt.Point p : visitOrder)
                {
                    double lon = origin.getX() + (p.getX() * pixelWidth);
                    double lat = origin.getY() - (p.getY() * pixelHeight);
                    double elevation = lasHashTable.getElevation(lon, lat);
                    LOG.debug("X: " + lon + " Y: " + lat + " Z: " + elevation);
                }
            }



            // Sort all the pixels by highest intensity -> lowest intensity
            // (highest intensity translates to the farthest from boundaries)
            //
            // For each pixel that's non-black, starting with the highest
            // intensity:
            //  1. Check if a pixel within a 9x9 pixel window has has its elevation computed yet
            //  2a. If yes, skip this pixel
            //  2b. It not, calculate the elevation at this point and add it to the elevation map

           /* for(int y = 0; y < widthPixels.length; y++)
            {
                for (int x = 0; x < widthPixels[y].length; x++)
                {
                    // TODO: Replace this with some class constant / pixel check...
                    if(widthPixels[y][x] != channelWidthMap.getBlackPixel())
                    {
                        double lon = origin.getX() + (x * pixelWidth);
                        double lat = origin.getY() - (y * pixelHeight);
                        //double usgselevation = getElevation(lon, lat);
                        int hx = (int)(lon * LON_LAT_SCALE) - xOffset;
                        int hy  = (int)(lat * LON_LAT_SCALE) - yOffset;

                        LASReader.PointRecord closestRecord = null;
                        if(hx >= 0 && hx <= mapWidth && hy >= 0 && hy <= mapHeight)
                        {
                            Point2D.Double source = new Point2D.Double(lon, lat);
                            ArrayList<LASReader.PointRecord> neighbors = pointMap[hy][hx];
                            Point2D.Double closestPoint = new Point2D.Double(neighbors.get(0).getX(), neighbors.get(0).getY());

                            for(int i = 1; i < neighbors.size(); i++) {
                                Point2D.Double comparisonPoint = new Point2D.Double(neighbors.get(i).getX(), neighbors.get(i).getY());
                                if(comparisonPoint.distance(source) < closestPoint.distance(source))
                                {
                                    closestPoint = comparisonPoint;
                                    closestRecord = neighbors.get(i);
                                }
                            }

                        }

                        if(closestRecord != null)
                        {
                            double lidarelevation = closestRecord.getZ();
                            elevationMap[y][x] = (int)lidarelevation;
                        }
                    }
                }
            }
           */

        } catch (IOException ioException) {
            LOG.debug("Unable to process elevation map!");
            ioException.printStackTrace(System.err);
        }

        return result;
    }

    public LinkedList<java.awt.Point> findPath(java.awt.Point start, java.awt.Point end, int[][] graph)
    {

    }


    public Double getElevation(Double x, Double y)
    {
        try {

            String dataUrl = "http://gisdata.usgs.gov/xmlwebservices2/elevation_service.asmx" +
                             "/getElevation?X_Value=" + x +
                             "&Y_Value=" + y +
                             "&Elevation_Only=TRUE" +
                             "&Elevation_Units=FEET" +
                             "&Source_Layer=NED.CONUS_NED";
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(dataUrl);

            // normalize text representation
            doc.getDocumentElement().normalize();
            // The results are contained in a single <double> node
            NodeList listOfDoubles = doc.getElementsByTagName("double");

            if(listOfDoubles.getLength() > 0)
            {
                Node elevationNode = listOfDoubles.item(0);
                Element elevationElement = (Element)elevationNode;
                Double elevation = Double.parseDouble(elevationElement.getFirstChild().getNodeValue());
                return elevation;
            }

        } catch (IOException ioException) {

        } catch (ParserConfigurationException pce) {

        } catch (SAXException se) {

        }

        return -1.0;
    }


    // Search class for LiDAR Data
    class LASHashTable {
        private final LASReader lasReader;
        private final Point2D origin;
        // Used to map lon/lat into integer-space with some room for
        // fuzziness. With this set to 10,000 the per-pixel deviation is
        // approximately .6 feet per pixel box, at 5,000 this is ~1.2 feet
        // (6,000 ft per degree, 1/N * 6000)
        private final int LON_LAT_SCALE;
        private final int xOffset, yOffset;
        private final double minX, maxX, minY, maxY;
        private final int mapWidth, mapHeight;
        private final ArrayList<LASReader.PointRecord>[][] pointMap;

        public LASHashTable(LASReader lasReader, Point2D origin, int scale)
        {
            this.lasReader = lasReader;
            this.LON_LAT_SCALE = scale;
            this.origin = origin;

            double lasMinX = Double.MAX_VALUE;
            double lasMaxX = Double.MIN_VALUE;
            double lasMinY = Double.MAX_VALUE;
            double lasMaxY = Double.MIN_VALUE;

            // TODO: Backwards X/Y not quite sure why yet...
            for(LASReader.PointRecord pointRecord : lasReader.readAllPointRecords())
            {
                lasMinX = Math.min(lasMinX, pointRecord.getY());
                lasMaxX = Math.max(lasMaxX, pointRecord.getY());
                lasMinY = Math.min(lasMinY, pointRecord.getX());
                lasMaxY = Math.max(lasMaxY, pointRecord.getX());
            }

            minX = lasMinX;
            minY = lasMinY;
            maxX = lasMaxX;
            maxY = lasMaxY;

            // Buffer either side
            mapWidth = (int)(Math.abs(maxX - minX) * LON_LAT_SCALE) + 2;
            mapHeight = (int)(Math.abs(maxY - minY) * LON_LAT_SCALE) + 2;

            xOffset = (int)(lasMinX * LON_LAT_SCALE);
            yOffset = (int)(lasMinY * LON_LAT_SCALE);
            pointMap = new ArrayList[mapHeight][mapWidth];

            this.generate();
        }


        public Double getElevation(double lon, double lat)
        {
            int hx = (int)(lon * LON_LAT_SCALE) - xOffset;
            int hy  = (int)(lat * LON_LAT_SCALE) - yOffset;

            LASReader.PointRecord closestRecord = null;
            if(hx >= 0 && hx <= mapWidth && hy >= 0 && hy <= mapHeight)
            {
                Point2D.Double source = new Point2D.Double(lon, lat);
                ArrayList<LASReader.PointRecord> neighbors = pointMap[hy][hx];
                Point2D.Double closestPoint = new Point2D.Double(neighbors.get(0).getX(), neighbors.get(0).getY());

                for(int i = 1; i < neighbors.size(); i++) {
                    Point2D.Double comparisonPoint = new Point2D.Double(neighbors.get(i).getX(), neighbors.get(i).getY());
                    if(comparisonPoint.distance(source) < closestPoint.distance(source))
                    {
                        closestPoint = comparisonPoint;
                        closestRecord = neighbors.get(i);
                    }
                }

            }

            if(closestRecord != null)
            {
                double lidarelevation = closestRecord.getZ();
                return lidarelevation;
            } else {
                return null;
            }
        }

        private void generate()
        {
            // Build a locality-based hash table of point records where the size of the bucket is related to
            // the scale of LON_LAT_SCALE -- each bucket is appx 6,000/LON_LAT_SCALE ft^2 in size
            if (lasReader != null)
            {
                int count = 0;
                LASReader.PointRecord[] records =  lasReader.readAllPointRecords();
                for(LASReader.PointRecord pointRecord : records)
                {
                    count++;
                    // TODO: These are swapped, not sure why. Need to fix but for now want to make this work.
                    double py = pointRecord.getX();
                    double px = pointRecord.getY();

                    int x = (int)(px * LON_LAT_SCALE) - xOffset;
                    int y = (int)(py * LON_LAT_SCALE) - yOffset;
                    // If we're in range, then add the point record to the bucket
                    if(!(y < 0 || x < 0 || y >= mapHeight || x >= mapWidth))
                    {
                        if(pointMap[y][x] == null)
                        {
                            pointMap[y][x] = new ArrayList<LASReader.PointRecord>();
                        }

                        pointMap[y][x].add(pointRecord);
                    }
                }

                LOG.debug("Loaded " + count + " LiDAR points");
            }

        }

        public int getScale()
        {
            return LON_LAT_SCALE;
        }

    }

}
