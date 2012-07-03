import org.gearman.Gearman;
import org.gearman.GearmanFunction;
import org.gearman.GearmanFunctionCallback;
import org.gearman.GearmanServer;
import org.gearman.GearmanWorker;

public class RiverExtractionWorker implements GearmanFunction { 
  public static void main(String... args) {
      Gearman gearman = Gearman.createGearman();
      GearmanServer server = gearman.createGearmanServer( "localhost", 4730);
      GearmanWorker worker = gearman.createGearmanWorker();
      worker.addFunction("channel_map", new RiverExtractionWorker());
      worker.addServer(server);
  }
 
  @Override
  public byte[] work(String function, byte[] data, GearmanFunctionCallback callback) throws Exception {
    return data;
  } 
}
