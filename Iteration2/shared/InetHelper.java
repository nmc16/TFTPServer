package shared;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class that provides functionality to get broadcast IP for the Client and server.
 * 
 * @version 1
 * @author Team6
 */
public class InetHelper {
	 /**
     * Get IP address from first non-loopback interface
     * 
     * @return Address from first non-loopback interface
     */
	public static String getIPAddress() {
		DataHelper.configLogger();
		Logger log = Logger.getLogger("global");
		
		try {
			// Get all the network interfaces on the system
			for (NetworkInterface i : Collections.list(NetworkInterface.getNetworkInterfaces())) {
				
				// Go through all the addresses and find the first non-loopback (local) IP
				for (InetAddress address : Collections.list(i.getInetAddresses())) {
                    if (!address.isLoopbackAddress() && address.getHostAddress().indexOf(':') < 0) {
                    	return address.getHostAddress();
                    }
				}
			}
        } catch (IOException e) {
        	log.log(Level.SEVERE, e.getMessage(), e);
        }
		return "";
    }
}
