package error;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;

import shared.Helper;
/**
 * The Thread for the Error simulator, takes init input from the Error simulator then send the request to the server 
 * (continued communication with server thread).
 * 
 * @version 2
 * @author Team6
 */
public class ErrorSimThread implements Runnable {
	private DatagramPacket packet;
	private DatagramSocket sendReceiveSocket;
	private int clientPort;
	private int serverPort;
	
	public ErrorSimThread(DatagramPacket packet) {
		this.packet = packet;
		this.clientPort = packet.getPort();
		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		} 
	}
	
	
	public static DatagramPacket BringError(byte msg[], String ErrCode, DatagramPacket Received, int porter) {
        byte newmsg[] = msg;
        InetAddress address = Received.getAddress();
        int length = Received.getLength();
        int port = porter;
        
        try{
        	address = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            System.out.println("Could not stop requests properly: " + e.getMessage());
            e.printStackTrace();
        }
        
        
        if(ErrCode.equals("01")){
        	//mess with data byte 1
        	newmsg[1] = 6;
        } else if(ErrCode.equals("02")){
        	//mess with data byte 0
        	newmsg[0] = 6;
        } else if(ErrCode.equals("03")){
        	//mess with port
        	port = 1;
        } else if(ErrCode.equals("04")){
        	//Wrong Address
        	try{
            	address = InetAddress.getByName("127.0.0.1");
            } catch (UnknownHostException e) {
                System.out.println("Could not stop requests properly: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return new DatagramPacket(newmsg, length, address, port);
    }
	
	public void PrintErrorList(){
		System.out.print("\n\nThis is the error SIM. To choose your error, please print exactly what's between the quotations: \n");
		System.out.print("\"00\": Normal Operations\n");
		System.out.print("\"01\": Changes the first byte in the OpCode\n");
		System.out.print("\"02\": Changes the second byte in the OpCode\n");
		System.out.print("\"03\": Change to an invalid port number\n");
		System.out.print("\"04\": Change to a different Address\n");
	    System.out.print("> ");
	}
	
	
	
	/**
	 *    
	 * @param buffer Array of bytes that was passed to it 512 if the buffer was filled
	 * @return true if the buffer is smaller then 512
	 */
	public boolean checkIfDone(byte[] buffer) {
		if (buffer[buffer.length - 1] == 0 && buffer[1] == 3) {
			return true;
		}
		return false;
	}
	/**
	 * Takes in information from the server thread, and passes it to the client and vice versa   
	 */
	@Override
	public void run() {
		// Initialize the variables
		  Scanner reader = new Scanner(System.in);
		  String input;
	      byte data[] = new byte[516];
	      
	      //create the packet
	      DatagramPacket sendPacket = new DatagramPacket(packet.getData(), packet.getLength(),
	                               					     packet.getAddress(), 69);
	      
	      byte datamins[] = Helper.minimi(sendPacket.getData(), sendPacket.getLength());
	      //print out the data to be sent
	      System.out.println( "Intermediate: Sending packet:");
	      System.out.println("To host: " + sendPacket.getAddress());
	      System.out.println("Destination host port: " + sendPacket.getPort());
	      System.out.println("Length: " + sendPacket.getLength());
	      System.out.print("Containing: ");
	      System.out.println(new String(sendPacket.getData()));
	      System.out.println("In bytes " + Arrays.toString(datamins) + "\n");
	      
	      //Send the data
	      try {
	    	  sendReceiveSocket.send(sendPacket);
	      } catch (IOException e) {
	    	  e.printStackTrace();
	    	  System.exit(1);
	      }
	
	      System.out.println("Intermediate: packet sent");
	      boolean flag = false;
	      while(true){

		      //empty the data receiver
		      data = new byte[516];
		      
		      //create a new receive packet
		      DatagramPacket receivePacket = new DatagramPacket(data, data.length);
		
		      if (checkIfDone(receivePacket.getData())) flag = true;
		      
		      //wait until a new packet is received
		      try {
		         sendReceiveSocket.receive(receivePacket);
		      } catch(IOException e) {
		         e.printStackTrace();
		         System.exit(1);
		      }
		      
		      serverPort = receivePacket.getPort();
		      
		      byte dataminr[] = Helper.minimi(receivePacket.getData(), receivePacket.getLength());
		      //print out the data on the received packet
		      System.out.println("Intermediate: Packet received:");
		      System.out.println("From host: " + receivePacket.getAddress());
		      System.out.println("Host port: " + receivePacket.getPort());
		      System.out.println("Length: " + receivePacket.getLength());
		      System.out.println("Containing: " + new String(receivePacket.getData()));
		      System.out.println("In bytes " + Arrays.toString(dataminr) + "\n\n");
		      
		      
		      //Decide on the error sim
		      PrintErrorList();
	          input = reader.nextLine();
		
		      
		      //create a new packet to send
		      sendPacket = BringError(dataminr, input, receivePacket, clientPort);
		      
		      byte datamins2[] = Helper.minimi(sendPacket.getData(), sendPacket.getLength());
		      //print out the data to be sent
		      System.out.println( "Intermediate: Sending packet:");
		      System.out.println("To host: " + sendPacket.getAddress());
		      System.out.println("Destination host port: " + sendPacket.getPort());
		      System.out.println("Length: " + sendPacket.getLength());
		      System.out.println("Containing: " + new String(sendPacket.getData()));
		      System.out.println("In bytes " + Arrays.toString(datamins2) + "\n");
		      
		      // Send the datagram packet to the client via the send socket. 
		      try {
		    	  sendReceiveSocket.send(sendPacket);
		      } catch (IOException e) {
		    	  e.printStackTrace();
		    	  System.exit(1);
		      }
		      
		      if (flag) {
		    	  break;
		      }
		      
		      //empty the data receiver
		      data = new byte[516];
		      
		      //create a new receive packet
		      receivePacket = new DatagramPacket(data, data.length);
		      
		      //wait until a new packet is received
		      try {
		    	  sendReceiveSocket.receive(receivePacket);
		    	  if (checkIfDone(receivePacket.getData())) flag = true;
		      } catch(IOException e) {
		    	  e.printStackTrace();
		    	  System.exit(1);
		      }
		      
		      
		    //Decide on the error sim
		      PrintErrorList();
	          input = reader.nextLine();
		
		      
		      //create a new packet to send
		      sendPacket = BringError(data, input, receivePacket, serverPort);
		      
		      byte datamins3[] = Helper.minimi(sendPacket.getData(), sendPacket.getLength());
		      //print out the data to be sent
		      System.out.println("Intermediate: Sending packet:");
		      System.out.println("To host: " + sendPacket.getAddress());
		      System.out.println("Destination host port: " + sendPacket.getPort());
		      System.out.println("Length: " + sendPacket.getLength());
		      System.out.println("Containing: " + new String(sendPacket.getData()));
		      System.out.println("In bytes " + Arrays.toString(datamins3) + "\n");
		      
		      // Send the datagram packet to the client via the send socket. 
		      try {
		    	  sendReceiveSocket.send(sendPacket);
		      } catch (IOException e) {
		    	  e.printStackTrace();
		    	  System.exit(1);
		      }
	      }
	      

	      // We're finished, so close the sockets.
	      sendReceiveSocket.close();
	      reader.close();
	}
	
}
