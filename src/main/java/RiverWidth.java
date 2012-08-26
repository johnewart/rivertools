import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

public class RiverWidth {

    final static Logger logger = LoggerFactory.getLogger(ImageTools.class);
    private int blackPixel;
    private final int tiffBlack = 16711680; // TIFF black
    private final int pngBlack = -16777216; // PNG black

    private File tile;
    private LinkedList<Point> pointQueue;
    private HashSet<Point> visitedPoints;
    private int width, height;
    private BufferedImage image;

    public RiverWidth(File tile) {
        IIORegistry registry = IIORegistry.getDefaultInstance();
        registry.registerServiceProvider(new com.tomgibara.imageio.impl.tiff.TIFFImageWriterSpi());
        registry.registerServiceProvider(new com.tomgibara.imageio.impl.tiff.TIFFImageReaderSpi());

        this.tile = tile;
        try {
            this.image = ImageIO.read(tile);
            this.width = this.image.getWidth();
            this.height = this.image.getHeight();
            String filetype = getFormatName(tile);

            if (filetype.equals("png")) {
                blackPixel = pngBlack;
                logger.debug("Processing a PNG file, black pixels are " + pngBlack);
            } else {
                blackPixel = tiffBlack;
                logger.debug("Processing a TIFF file, black pixels are " + tiffBlack);
            }

        } catch (IOException ioe) {
            this.image = null;
        }
    }

    private String getFormatName(Object o) {
        try {
            ImageInputStream iis = ImageIO.createImageInputStream(o);
            Iterator iter = ImageIO.getImageReaders(iis);
            if (!iter.hasNext()) {
                return null;
            }
            ImageReader reader = (ImageReader) iter.next();
            iis.close();
            return reader.getFormatName();
        } catch (IOException e) {

        }
        return null;
    }

    public BufferedImage computeWidth(float xorigin, float yorigin) {
        int startx = (int) (this.width * xorigin);
        int starty = (int) (this.height * yorigin);

        logger.debug("Starting at (" + startx + ", " + starty + ")");
        return this.computeWidth(startx, starty);
    }

    public BufferedImage computeWidth(int startx, int starty) {
        pointQueue = new LinkedList<Point>();
        visitedPoints = new HashSet<Point>();
        int maxDistance = 0;

        if (this.image != null) {
            try {
                int[][] pixels = ImageTools.convertRGBTo2DBinary(this.image, this.blackPixel);
                pixels = ImageTools.dilate(pixels);
                pixels = ImageTools.dilate(pixels);
                int[][] widthMap = new int[height][width];
                int[][] channelMap = new int[height][width];

                HashSet<Point> channelPoints = new HashSet<Point>();
                HashSet<Point> queuedPoints = new HashSet<Point>();

                Point startingPoint = new Point(startx, starty);

                /* !0 = white, 0 = black */
                // BFS to calculate shortest path to a non-black pixel
                logger.debug("Pixel: " + pixels[starty][startx]);
                logger.debug("pxl width: " + pixels[0].length + " height: " + pixels.length);

                // Not a black pixel, abort
                if (pixels[startingPoint.y][startingPoint.x] != 0) {
                    logger.debug("Not a black pixel, aborting");
                    return null;
                }

                pointQueue.offer(startingPoint);

                while (!pointQueue.isEmpty()) {
                    Point p = pointQueue.poll();
                    visitedPoints.add(p);
                    for (int xoff = -1; xoff <= 1; xoff++) {
                        for (int yoff = -1; yoff <= 1; yoff++) {
                            if (!(xoff == 0 && yoff == 0)) {
                                int nx = p.x + xoff;
                                int ny = p.y + yoff;
                                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                                    if (pixels[ny][nx] == 0) {
                                        Point np = new Point(nx, ny);
                                        if (!visitedPoints.contains(np) && !queuedPoints.contains(np)) {
                                            pointQueue.offer(np);
                                            queuedPoints.add(np);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                logger.debug("Total points: " + visitedPoints.size());

                // 2nd-pass, for each item in the set of channel points, calculate
                // distance to nearest non-black pixel
                for (Iterator<Point> it = visitedPoints.iterator(); it.hasNext(); ) {
                    Point currentPoint = (Point) it.next();
                    logger.debug("Measuring distance for " + currentPoint.toString());
                    channelMap[currentPoint.y][currentPoint.x] = 255;

                    pointQueue = new LinkedList<Point>();
                    queuedPoints = new HashSet<Point>();
                    pointQueue.offer(currentPoint);
                    queuedPoints.add(currentPoint);
                    visitedPoints = new HashSet<Point>();
                    boolean finished = false;

                    int distance = 0;

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
                                        logger.debug(currentPoint.toString() + " distance: " + distance);
                                        widthMap[currentPoint.y][currentPoint.x] = distance;
                                        maxDistance = Math.max(distance, maxDistance);
                                        finished = true;
                                    }
                                }
                            }
                        }

                    }
                }

                // 3rd pass, increase intensity of pixels so that we can see them
                double scale = 255 / maxDistance;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        widthMap[y][x] = (int) (scale * widthMap[y][x]);
                    }
                }

                BufferedImage outimage = ImageTools.convert2DIntToARGB(widthMap);
                return outimage;
            } catch (Exception e) {
                logger.debug("There was a problem: ", e);
                return null;
            }
        } else {
            logger.debug("The image is null...");
            return null;
        }

    }
}
