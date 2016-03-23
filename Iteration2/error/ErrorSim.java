package error;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import shared.DataHelper;

/**
 * Main error simulator creates the {@link error.ErrorSimThread} threads for each new client request.
 *
 * Listens to initial packets to send on port 68.
 *
 * @version 3
 * @author Team6
 */
public class ErrorSim {
    private final Logger LOG;
	private DatagramSocket receiveSocket;
	private static final Scanner READER = new Scanner(System.in);

	public ErrorSim() {
        // Initialize the logger if it hasn't been already and get it
        DataHelper.configLogger();
        LOG = Logger.getLogger("global");

        // Set up the socket to receive requests on
		try {
			receiveSocket = new DatagramSocket(68);
		} catch (SocketException se) {
			LOG.log(Level.SEVERE, se.getMessage(), se);
			System.exit(1);
		} 
	}
	
	/**
	 * Listens for new client requests and creates an ErrorSimThread thread
     * to deal with each new request.
	 */
	public void receiveAndEcho() {
        LOG.info("Error Simulator started. Waiting to receive packet...");
      
		while(true){
            // Create the receive packet for the request
            byte data[] = new byte[516];
			DatagramPacket receivePacket = new DatagramPacket(data, data.length);

			// Block until a datagram packet is received from receiveSocket.
			try {        
				receiveSocket.receive(receivePacket);
			} catch (IOException e) {
				LOG.log(Level.SEVERE, e.getMessage(), e);
				break;
			}

			// Print out the data within the received packet
			DataHelper.printPacketData(receivePacket, "Error Simulator Packet received", true, false);

            // Start a new thread to deal with the request
            LOG.info("Starting new request thread...");
			Thread t = new Thread(new ErrorSimThread(receivePacket, READER));
			t.start();
		}

        // Close the resources
        READER.close();
		receiveSocket.close();
	}
	
	public static void main(String args[]) {
		ErrorSim c = new ErrorSim();
		c.receiveAndEcho();
	}
}
