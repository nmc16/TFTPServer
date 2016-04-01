package server;

import exception.IllegalOPException;
import shared.DataHelper;
import shared.ErrorCodes;
import shared.FileHelper;
import shared.SocketHelper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread that receives incoming requests from clients and delegates them to individual
 * {@link ServerResponse} threads. Checks the requests are valid and sends an error packet
 * back if they are not.
 * 
 * @version 2
 * @author Team6
 */
public class ServerRequest implements Runnable {
    private final SocketHelper socketHelper;
    private final Logger LOG;
    private DatagramSocket receiveSocket;
    private ArrayList<Thread> openRequests;
    private ArrayList<String> filesInUse;

    public ServerRequest() {
    	// Set up the Logger
        DataHelper.configLogger();
        LOG = Logger.getLogger("global");
        
        // Set up the lists for tracking requests
        openRequests = new ArrayList<Thread>();
        filesInUse = new ArrayList<String>();

        try {
        	// Get the site local address
            String siteLocalAddress = InetAddress.getLocalHost().getHostAddress();
            InetAddress socketAddress = InetAddress.getByName(siteLocalAddress);
        	
            receiveSocket = new DatagramSocket(69, socketAddress);
            receiveSocket.setSoTimeout(1000);
            
            LOG.info("Server is broadcasting on address: " + siteLocalAddress + " and port: " + 69 + ".");
        } catch (IOException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            System.exit(1);
        }

        // Set up the SocketHelper
        socketHelper = new SocketHelper(receiveSocket);
    }

    /**
     * Method to make sure that the data is in the proper format
     *
     * @param msg is the either read or write request from the client
     * @return true if in format 02 || 01 then some text 0 then some text 0
     */
    public boolean verify(byte msg[]){
        int n = msg.length;
        int m = 2;
        int q = 0;

        if(msg[0] != 0){
            return false;
        }

        if (msg[1] != 1 && msg[1] != 2) {
            return false;
        }

        while(m != n){
            if(msg[m] == 0){
                if(msg[m-1] == 2 || msg[m-1] == 1){
                    return false;
                }
                if(q == 0){
                    if(msg[m+1] == 0){
                        return false;
                    }
                } else {
                    if(m+1 != n){
                        return false;
                    }
                }
                q++;
            }
            m++;
        }
        
        int index = 0;
        for (int i = msg.length - 2; i > 0; i--) {
        	index = i;
        	if (msg[i] == 0) {
        		break;
        	}
        }
        
        String s = new String(Arrays.copyOfRange(msg, index, msg.length - 1));
        s = s.trim();
        if (!s.equals("octet") && !s.equals("netascii") && !s.equals("wait")) {
        	return false;
        }
        
        return q == 2;
    }

    /**
     * Allows access to the file being read.
     *
     * @param packet Packet to check file from
     * @return true if the file is not in use and the request can go through
     */
    public boolean allowAccess(DatagramPacket packet){
    	for(String fileName: filesInUse){//CHANGE TO .lentgh format and make sure corresponding thread is still running
    		if (fileName.equals(FileHelper.getFileFromPacket(packet).getName())) {
                // If file is already active
    			return false;
    		}
    	}

        // The file is not active
    	return true;
    }

    /**
     * Removes inactive threads from the thread list and the files being preformed on from the active
     * files list.
     */
    public void removeInactive() {
        int index;

        // Create a copy of the original list
        ArrayList<Thread> copy = new ArrayList<Thread>(openRequests);

        // Loop and find inactive threads and remove them from the original list
        for (Thread t : copy) {
            if (!t.isAlive()) {
                index = copy.indexOf(t);
                openRequests.remove(index);
                filesInUse.remove(index);
            }
        }
    }

    @Override
    public void run() {
        // Run until the server UI tells us to stop
        while(!ServerSettings.stopRequests) {
            // Initialize the required variables
            byte data[] = new byte[100];
            byte mydata[];

            DatagramPacket receivePacket = new DatagramPacket(data, data.length);

            try {
                receiveSocket.receive(receivePacket);

                // Minimize the data
                int len = receivePacket.getLength();
                mydata = DataHelper.minimi(data, len);
                
                if(allowAccess(receivePacket)){
                	
	                // Verify the data
	                if(verify(mydata)){
	                    // Print out the data on the received package
	                    DataHelper.printPacketData(receivePacket, "Server", ServerSettings.verbose, true);

                        // Start the new thread
	                    Thread clientThread = new Thread(new ServerResponse(receivePacket));
	                    clientThread.start();

                        // Add thread and file to active lists
	                    openRequests.add(clientThread);
	                    filesInUse.add(FileHelper.getFileFromPacket(receivePacket).getName());

	                } else{
	                    // Terminate the request
	                    LOG.warning("Received invalid request! Not allowing request to be performed.");
	                    DataHelper.printPacketData(receivePacket, "Server Request Thread: Invalid Request",
                                                   ServerSettings.verbose, true);
	                    socketHelper.sendErrorPacket(ErrorCodes.ILLEGAL_OP, receivePacket.getAddress(),
                                                     receivePacket.getPort(), new IllegalOPException("Invalid data request"));
	                }
                } else {
                    // If the file is already in use we don't want to start another request on it
                    LOG.warning("Request denied, file already in use.");
                    DataHelper.printPacketData(receivePacket, "Server Request Thread: access denied! File in use",
                                               ServerSettings.verbose, true);
                    socketHelper.sendErrorPacket(ErrorCodes.ACCESS, receivePacket.getAddress(), receivePacket.getPort(),
                                                 new SecurityException("Request denied, file already in use."));
                }

            } catch (SocketTimeoutException e) {
                // We don't care because our calls are non-blocking for this socket

            } catch (IOException e) {
                LOG.log(Level.SEVERE, e.getMessage(), e);
                System.exit(1);
            }

            removeInactive();
        }

        // Close it up
        LOG.info("Server waiting for current requests to finish (" + openRequests.size() + ")...");
        receiveSocket.close();

        for (Thread t : openRequests) {
            try {
                t.join();
            } catch (InterruptedException e) {
                LOG.severe("Error waiting for requests to finish: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }
}
