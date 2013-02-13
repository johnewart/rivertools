package net.johnewart.rivertools.factories;

import net.johnewart.rivertools.core.AerialMap;

/**
 * Created with IntelliJ IDEA.
 * User: jewart
 * Date: 12/29/12
 * Time: 9:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class AerialMapFactory extends RiverSimModelFactory {
    public static AerialMap loadFromAPI(long id)
    {
        return (AerialMap)RiverSimModelFactory.loadObject(id, "aerial_map", AerialMap.class);
    }

}
