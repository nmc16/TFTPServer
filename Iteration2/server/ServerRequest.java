package server;

import shared.DataHelper;
import shared.FileHelper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Thread that receives incoming requests from clients and delegates them to individual
 * {@link ServerResponse} threads. Checks the requests are valid and sends an error packet
 * back if they are not.
 * 
 * @version 2
 * @author Team6
 */
public class ServerRequest implements Runnable {

    // Initialize the sockets and packets
    private DatagramPacket receivePacket;
    private DatagramSocket receiveSocket;
    private ArrayList<Thread> openRequests;
    private ArrayList<String> filesInUse;

    public ServerRequest() {
        openRequests = new ArrayList<Thread>();
        filesInUse = new ArrayList<String>();

        try {
            receiveSocket = new DatagramSocket(69);
            receiveSocket.setSoTimeout(1000);
        } catch (SocketException se) {
            System.out.println("Could not create request socket: " + se.getMessage());
            se.printStackTrace();
            System.exit(1);
        }
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
    
    public boolean allowAccess(DatagramPacket packet){
    	//File file = DataHelper.getFile(initialPacket)
    	for(String fileName: filesInUse){//CHANGE TO .lentgh format and make sure corresponding thread is still running
    		//if file already being used
    		if (fileName.equals(FileHelper.getFileFromPacket(packet).getName())) {
    			return false;
    		}
    	}
    	
    	return true;
    	
    }

    @Override
    public void run() {

        while(!ServerSettings.stopRequests) {
            // Initialize the required variables
            byte data[] = new byte[100];
            byte mydata[];

            receivePacket = new DatagramPacket(data, data.length);

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
	                    Thread clientThread = new Thread(new ServerResponse(receivePacket));
	                    clientThread.start();
	                    openRequests.add(clientThread);
	                    filesInUse.add(FileHelper.getFileFromPacket(receivePacket).getName());
	                } else{
	                    //terminate the program
	                    System.out.println("\nReceived invalid request! Not allowing request to be performed.");
	                    DataHelper.printPacketData(receivePacket, "Server Request Thread: Invalid Request", true, true);
	                    ServerResponse response = new ServerResponse(receivePacket);
	                    byte[] errcode = {0, 4};
	                    response.sendERRPacket(errcode, receivePacket.getAddress(), "Invalid data request", receivePacket.getPort());
	                }
                } else{
            	 	System.out.println("\n access denied! File in use");
            	 	DataHelper.printPacketData(receivePacket, "Server Request Thread: Sercurity declined", true, true);
            	 	ServerResponse response = new ServerResponse(receivePacket);
            	 	byte[] errcode = {0, 2};
                    response.sendERRPacket(errcode, receivePacket.getAddress(), "Sercurity declined", receivePacket.getPort());
                }

            } catch (SocketTimeoutException e) {
                // We don't care because our calls are non-blocking for this socket

            } catch (IOException e) {
                System.out.print("IO Exception: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
            int index;
            ArrayList<Thread> copy = new ArrayList<Thread>(openRequests);
            for (Thread t : copy) {
                if (!t.isAlive()) {
                    index = copy.indexOf(t);
                	openRequests.remove(index);
                    filesInUse.remove(index);
                }
            }
        }

        // Close it up
        System.out.println("Server waiting for current requests to finish (" + openRequests.size() + ")...");
        receiveSocket.close();

        for (Thread t : openRequests) {
            try {
                t.join();
            } catch (InterruptedException e) {
                System.out.println("Error waiting for requests to finish: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }
}
