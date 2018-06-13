package net.johnewart.rivertools.core;

import net.johnewart.rivertools.utils.ImageTools;
import org.codehaus.jackson.annotate.JsonProperty;
import org.gearman.common.interfaces.GearmanFunctionCallback;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: jewart
 * Date: 12/29/12
 * Time: 9:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChannelMap extends ImageMap {


    @JsonProperty
    public Long id;

    @JsonProperty
    public boolean job_complete;

    @JsonProperty
    public String job_handle;

    @JsonProperty
    public String resource_uri;

    private Set<Point> channelPoints;

    public Set<Point> findChannelPoints(Set<java.awt.Point> startingPoints)
    {
        // Ensure our image data is loaded.
        load();

        if(channelPoints == null)
        {
            this.dirty = true;
            channelPoints = new HashSet<>();

            LinkedList<Point> pointQueue = new LinkedList<>();
            HashSet<Point> visitedPoints = new HashSet<>();
            System.err.println("Image width: " + pixels[0].length + " height: " + pixels.length);

            System.err.println("Dilating.");
            pixels = ImageTools.dilate(pixels);
            System.err.println("Dilating.");
            pixels = ImageTools.dilate(pixels);

            try {
                BufferedImage outimage =
                        ImageTools.convert2DIntToARGB(pixels);
                ImageIO.write(outimage, "TIF", new File("/tmp/bwchannels.png"));
                System.err.println("Wrote /tmp/bwchannels.jpg");
            } catch (IOException e) {
                e.printStackTrace();
            }

            for(java.awt.Point startingPoint : startingPoints)
            {

                System.err.println("Considering: " + startingPoint.x + "," + startingPoint.y);
                // Check to see if we already have a width for this point
                // if we do, no sense in starting here, it's already been calculated
                // The point here is to make sure all the points in the channel
                // get calculated, even if there's some visual artifact preventing
                // the channel being connected.
                if(!channelPoints.contains(startingPoint))
                {
                    /* !0 = white, 0 = black */
                    // BFS to calculate shortest path to a non-black pixel
                    System.err.println("Pixel: " + pixels[startingPoint.y][startingPoint.x]);

                    // Not a black pixel, abort
                    if (pixels[startingPoint.y][startingPoint.x] != 0) {
                        System.err.println("Not a black pixel, skipping");
                        //return null;
                    } else {
                        pointQueue.offer(startingPoint);
                    }


                    while (!pointQueue.isEmpty()) {
                        Point p = pointQueue.poll();
                        channelPoints.add(p);
                        for (int xoff = -1; xoff <= 1; xoff++) {
                            for (int yoff = -1; yoff <= 1; yoff++) {
                                if (!(xoff == 0 && yoff == 0)) {
                                    int nx = p.x + xoff;
                                    int ny = p.y + yoff;
                                    if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                                        if (pixels[ny][nx] == 0) {
                                            Point np = new Point(nx, ny);
                                            if (!channelPoints.contains(np) && !visitedPoints.contains(np)) {
                                                pointQueue.offer(np);
                                                visitedPoints.add(np);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return channelPoints;
    }

    // TODO: remove the callback from here, makes it a little kludgy - status variable / thread?
    public int[][] computeWidthMap(Set<Point> channelPoints, GearmanWorker worker)
    {
        dirty = true;

        int maxDistance = 0;
        HashMap<Point, Integer> widthMap = new HashMap<>();
        System.err.println("Total points to process: " + channelPoints.size());

        // 2nd-pass, for each item in the set of channel points, calculate
        // distance to nearest non-black pixel
        int completeCount = 0;

        for (Iterator<Point> it = channelPoints.iterator(); it.hasNext(); ) {
           Point currentPoint = it.next();

           if(completeCount++ % 1000 == 0)
           {
               System.err.println(completeCount + "/" + channelPoints.size());
               if(callback != null)
                   callback.sendStatus(completeCount, channelPoints.size());
           }


           LinkedList<Point> pointQueue = new LinkedList<>();
           HashSet<Point> queuedPoints = new HashSet<>();
           pointQueue.offer(currentPoint);
           queuedPoints.add(currentPoint);
           HashSet<Point> visitedPoints = new HashSet<>();
           boolean finished = false;

           int distance;

           while (!pointQueue.isEmpty() && !finished) {
               Point p = pointQueue.poll();
               visitedPoints.add(p);
               for (int xoff = -1; xoff <= 1; xoff++) {
                   for (int yoff = -1; yoff <= 1; yoff++) {
                       int nx = p.x + xoff;
                       int ny = p.y + yoff;
                       if (!(xoff == 0 && yoff == 0) && nx < width && nx >= 0 && ny < height && ny >= 0) {
                           Point np = new Point(nx, ny);
                           if (pixels[ny][nx] == 0) {
                               if (!visitedPoints.contains(np) && !queuedPoints.contains(np)) {
                                   pointQueue.offer(np);
                                   queuedPoints.add(np);
                               }
                           } else {
                               double dist = Math.sqrt((Math.pow((p.x - currentPoint.x), 2)) + (Math.pow((p.y - currentPoint.y), 2)));
                               distance = (int) dist;
                               //System.err.println(currentPoint.toString() + " distance: " + distance);
                               widthMap.put(currentPoint, distance);
                               maxDistance = Math.max(distance, maxDistance);
                               finished = true;
                           }
                       }
                   }
               }

           }
        }

        // Set all pixels to black (re-use array)
        for(int y = 0; y < height; y++)
        {
           for(int x = 0; x < width; x++)
           {
               pixels[y][x] = 0;
           }
        }

        // 3rd pass, increase intensity of pixels so that we can see them
        // And store them in the array
        double scale = 255 / maxDistance;
        for(Point point : widthMap.keySet())
        {
           pixels[point.y][point.x] = (int)(widthMap.get(point) * scale);
        }

        return pixels;
    }
}
