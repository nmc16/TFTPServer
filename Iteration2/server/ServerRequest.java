package server;

import shared.Helper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

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

    public ServerRequest() {
        openRequests = new ArrayList<Thread>();

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

        return q == 2;
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
                mydata = Helper.minimi(data, len);

                // Verify the data
                if(verify(mydata)){
                    // Print out the data on the received package
                    Helper.printPacketData(receivePacket, "Server", ServerSettings.verbose);
                    Thread clientThread = new Thread(new ServerResponse(receivePacket));
                    clientThread.start();
                    openRequests.add(clientThread);
                } else{
                    //terminate the program
                    throw new RuntimeException("Invalid data request");
                }

            } catch (SocketTimeoutException e) {
                // We don't care because our calls are non-blocking for this socket

            } catch (IOException e) {
                System.out.print("IO Exception: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }

            ArrayList<Thread> copy = new ArrayList<Thread>(openRequests);
            for (Thread t : copy) {
                if (!t.isAlive()) {
                    openRequests.remove(copy.indexOf(t));
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
