package net.johnewart.rivertools.analysis;

import net.johnewart.gearman.common.interfaces.GearmanWorker;
import net.johnewart.rivertools.core.ChannelMap;
import net.johnewart.rivertools.utils.ImageTools;
import org.gearman.common.interfaces.GearmanFunctionCallback;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Set;


public class RiverWidth {


    private Set<Point> startingPoints;
    private ChannelMap channelMap;

    public RiverWidth(ChannelMap channelMap, Set<Point> points, Long naturalWidth, Long naturalHeight) {
        this.channelMap = channelMap;

        for(Point point : points)
        {
            point.x = (int)(((float)point.x / naturalWidth) * channelMap.getImage().getWidth());
            point.y = (int)(((float)point.y / naturalHeight) * channelMap.getImage().getHeight());
        }

        startingPoints = points;
    }

    public BufferedImage computeWidth(GearmanWorker worker) {

        if (this.channelMap != null) {
            try {

                Set<Point> channelPoints = channelMap.findChannelPoints(startingPoints);

                // TODO: Optimize this w/ threads?
                // If any of the points are found to not be in the set of points
                // to process, then they are a disjointed set and can be
                // processed in parallel
                BufferedImage outimage =
                        ImageTools.convert2DIntToARGB(channelMap.computeWidthMap(channelPoints, worker));

                return outimage;
            } catch (Exception e) {
                System.err.println("There was a problem: ");
                e.printStackTrace();
                return null;
            }
        } else {
            System.err.println("The image is null...");
            return null;
        }

    }
}
