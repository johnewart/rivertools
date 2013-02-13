package net.johnewart.rivertools.factories;

import net.johnewart.rivertools.core.ChannelWidthMap;

/**
 * Created with IntelliJ IDEA.
 * User: jewart
 * Date: 12/29/12
 * Time: 9:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChannelWidthMapFactory extends RiverSimModelFactory {
    public static ChannelWidthMap loadFromAPI(long id)
    {
        return (ChannelWidthMap)RiverSimModelFactory.loadObject(id, "channel_width_map", ChannelWidthMap.class);
    }

}
