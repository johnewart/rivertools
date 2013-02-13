package net.johnewart.rivertools.factories;

import net.johnewart.rivertools.core.ChannelMap;

/**
 * Created with IntelliJ IDEA.
 * User: jewart
 * Date: 12/29/12
 * Time: 9:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChannelMapFactory extends RiverSimModelFactory {
    public static ChannelMap loadFromAPI(long id)
    {
        return (ChannelMap)RiverSimModelFactory.loadObject(id, "channel_map", ChannelMap.class);
    }

}
