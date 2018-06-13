import org.gearman.Gearman;
import org.gearman.GearmanFunction;
import org.gearman.GearmanFunctionCallback;
import org.gearman.GearmanServer;
import org.gearman.GearmanWorker;

public class RiverExtractionWorker { 
  public RiverExtractionWorker() {
      Gearman gearman = Gearman.createGearman();
      GearmanServer server = gearman.createGearmanServer( "localhost", 4730);
      GearmanWorker worker = gearman.createGearmanWorker();
      worker.addFunction("channel_map", new ChannelFunc());
      worker.addServer(server);
  }
}

class ChannelFunc implements GearmanFunction {

  public byte[] work(String function, byte[] data, GearmanFunctionCallback callback) throws Exception {
    return data;
  } 
}
