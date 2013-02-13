package net.johnewart.rivertools.factories;

import net.johnewart.rivertools.core.Simulation;

/**
 * Created with IntelliJ IDEA.
 * User: jewart
 * Date: 12/29/12
 * Time: 9:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimulationFactory {

    public static Simulation loadFromAPI(long id)
    {
        return (Simulation)RiverSimModelFactory.loadObject(id, "simulation", Simulation.class);
    }
}
