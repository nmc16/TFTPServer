package error;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Scanner;

import shared.DataHelper;

/**
 * The Thread for the Error simulator, takes init input from the Error simulator then send the request to the server 
 * (continued communication with server thread).
 * 
 * @version 2
 * @author Team6
 */
public class ErrorSimThread implements Runnable {
	private DatagramSocket sendReceiveSocket;
	private DatagramSocket errorSendSocket;
	private DatagramPacket initPacket;
	private final Scanner reader;
	private int clientPort;
	private int serverPort = -1;
	private int packCount = 0;
	private int packNum = 0;
	private int delay = 2000;
	private String mode = "00";
	private boolean errSocket = false, delayPacket = false, duplicate = false, lost = false;
	
	public ErrorSimThread(DatagramPacket packet, Scanner reader) {
		this.clientPort = packet.getPort();
		this.initPacket= packet;
		this.reader = reader;
		try {
			sendReceiveSocket = new DatagramSocket();
			errorSendSocket = new DatagramSocket();

		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		} 
	}

	public DatagramPacket bringError(byte msg[], DatagramPacket received, int port) {
        byte newmsg[] = msg;
        InetAddress address = received.getAddress();
        int length = received.getLength();
        
       
        if(mode.equals("00")){
        	//normal
        	return new DatagramPacket(newmsg, length, address, port);
        }
        else if(mode.equals("01")){
        	//mess with data byte 1
        	newmsg[1] = 6;
        } else if(mode.equals("02")){
        	//mess with data byte 0
        	newmsg[0] = 6;
        } else if(mode.equals("03")){
        	//mess with port
        	errSocket = true;
        } else if(mode.equals("04")){
        	//Wrong Address
        	errSocket = true;
        } else if(mode.equals("05")){
        	// Edit the last bit of the mode to make it invalid
        	newmsg[length - 2] = 122;
        } else if(mode.equals("06")){ //&& Integer.valueOf(args[1])>0){
	        //delay the packet
        	
        	delayPacket = true;
        	//TODO add default val of arg1
        }else if(mode.equals("07")){
        	//duplicate sent packet
        	duplicate = true;
        	
        	
        } else if(mode.equals("08")){
        	//lost
        	lost = true;
        	
        } 
        
        
        return new DatagramPacket(newmsg, length, address, port);
    }
	
	public void delayPacket(DatagramPacket data){
		Thread requests = new Thread(new DelayPack(delay, data, this));
        requests.start();
		
		delayPacket=false;
	}
	
	public void printErrorList() {
		System.out.print("\n\nThis is the error SIM. To choose your error, please print exactly what's between the quotations and the packet number afterwards: \n");
		System.out.print("\"00\": Normal Operations\n");
		System.out.print("\"01\": Changes the first byte in the OpCode\n");
		System.out.print("\"02\": Changes the second byte in the OpCode\n");

	    System.out.print("\"03\": Change to an invalid port number\n");
	    System.out.print("\"04\": Change to a different Address\n");
	    System.out.print("\"05\": Change the mode\n");
	    System.out.print("\"06\": Delay Packet\n");
	    System.out.print("\"07\": Duplicate Packet\n");
	    System.out.print("\"08\": Lose Packet\n");

	    System.out.print("> ");
	}
	
	
	public void sendUsingSocket(DatagramPacket packet) {
		try {
			if (errSocket) {
				errSocket = false;
				errorSendSocket.send(packet);
			} else {
	            sendReceiveSocket.send(packet);
			}
	      } catch(IOException e) {
	         e.printStackTrace();
	         System.exit(1);
	      }
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
	
	public boolean setMode(String input) {
		String args[] = input.split("\\s+");
 	  	
 	  	if(args.length != 2){
 	  		return false;
 	  	}
 	  	
 	  	int i = Integer.valueOf(args[0]);
 	  	if (i < 0 || i > 9) {
 	  		return false;
 	  	}
 	  	
 	  	mode = args[0];
 	  	packCount = 1;
 	  	packNum = Integer.valueOf(args[1]);
 	  	
 	  	return true;
 	  	
	}
	
	
	public boolean recAndSend(){
		
		 byte data[] = new byte[516];
		
		//create a new receive packet
	      DatagramPacket receivePacket = new DatagramPacket(data, data.length);
	      
	      //wait until a new packet is received
	      try {
	         sendReceiveSocket.receive(receivePacket);
	      } catch(IOException e) {
	         e.printStackTrace();
	         System.exit(1);
	      }
	      
	      if(serverPort == -1){
	    	  serverPort = receivePacket.getPort();
	      }
	      
	      DataHelper.printPacketData(receivePacket, "ErrorSim Received Packet", true, false);
	      	      
	      if(receivePacket.getPort() == serverPort){
	    	  	receivePacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), receivePacket.getAddress(), clientPort);
	      } else {
	    	  receivePacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), receivePacket.getAddress(), serverPort);
	      }
	      
	      packCount++;
	      if(packCount == packNum){
	    	  //call function
	    	  receivePacket = bringError(DataHelper.minimi(receivePacket.getData(), receivePacket.getLength()), receivePacket, receivePacket.getPort());
	      }
	      
	      if(receivePacket.getData()[1] == 5){
	    	  DataHelper.printPacketData(receivePacket, "ErrorSim Sending Error Packet", true, false);
	    	  sendUsingSocket(receivePacket);
	    	  return false;
	      }
	      
	      DataHelper.printPacketData(receivePacket, "ErrorSim Sending Packet", true, false);
	      
	      //send
	      if(lost){
	    	  System.out.println("Packet Lost");
	    	  lost = false;
	    	  //recAndSend();
	      } else if(duplicate){
	    	  sendUsingSocket(receivePacket);
	    	  sendUsingSocket(receivePacket);
	    	  duplicate = false;
	      } else if(delayPacket){
	    	  delayPacket(receivePacket);
	      } else{
	    	  sendUsingSocket(receivePacket);
	      }
		       
		return true;
	      
	      
	}
	
	
	
	/**
	 * Takes in information from the server thread, and passes it to the client and vice versa   
	 */
	@Override
	public void run() {
		// Initialize the variables
		String input;
	      
		//Decide on the error sim
		while(true){
			printErrorList();			
	 	  	input = reader.nextLine();
	 	  	if(setMode(input)) {
	 	  		break;
	 	  	} else{
	 	  		System.out.println("Incorrect input (do it again!)");
	 	  	}
	 	  	
		}
		
		DatagramPacket sent = new DatagramPacket(initPacket.getData(), initPacket.getLength(), initPacket.getAddress(), 69);
		sendUsingSocket(sent);
		
		packCount++;
		
		
		while(true){
			boolean cont;
			cont = recAndSend();
			
			if(!cont){
				break;
			}

			cont = recAndSend();
			
			if(!cont){
				break;
			}
		}

		// We're finished, so close the sockets.
		sendReceiveSocket.close();
	}
	
}
