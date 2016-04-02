package error;

import shared.DataHelper;

import java.net.DatagramPacket;
import java.util.logging.Logger;

public class DelayPack implements Runnable {
	private int delay;
	private DatagramPacket data;
	private ErrorSimThread es;
    private final Logger LOG;

	public DelayPack(int delay, DatagramPacket data, ErrorSimThread es) {
        DataHelper.configLogger();
        LOG = Logger.getLogger("global");
		this.delay = delay;
		this.data = data;
		this.es = es;
	}
	
	@Override
	public void run() {
		try{
			Thread.sleep(delay);
		} catch(InterruptedException e){
			LOG.severe("Interrupted while trying to delay packet sending.");
		}
		
		es.sendUsingSocket(data);
	}
}
