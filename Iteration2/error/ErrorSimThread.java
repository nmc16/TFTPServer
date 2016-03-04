package error;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

import server.ServerRequest;
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
	private DatagramSocket errorSendSocket;
	private int clientPort;
	private int serverPort;
	private boolean countMode = false, errSocket = false, delayPacket=false, lost=false;
	private int counter = 0;
	private int packCount = 0;
	private int packNum = 0;
	private int delay=1000;
	
	public ErrorSimThread(DatagramPacket packet) {
		this.packet = packet;
		this.clientPort = packet.getPort();
		try {
			sendReceiveSocket = new DatagramSocket();
			errorSendSocket = new DatagramSocket();

		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		} 
	}
	
	
	public DatagramPacket BringError(byte msg[], String ErrCode, DatagramPacket Received, int porter) {
        byte newmsg[] = msg;
        InetAddress address = Received.getAddress();
        int length = Received.getLength();
        int port = porter;
        
        String args[] = ErrCode.split("\\s+");
        
        if(args[0].equals("01")){
        	//mess with data byte 1
        	newmsg[1] = 6;
        	packNum = 1;
        } else if(args[0].equals("02")){
        	//mess with data byte 0
        	newmsg[0] = 6;
        	packNum = 1;
        } else if(args[0].equals("03")){
        	//mess with port
        	errSocket = true;
        	packNum = 1;
        } else if(args[0].equals("04")){
        	//Wrong Address
        	errSocket = true;
        	packNum = 1;
        } else if(args[0].equals("05")){
        	// Edit the last bit of the mode to make it invalid
        	newmsg[length - 2] = 122;
        	packNum = 1;
        } else if(args[0].equals("06")){ //&& Integer.valueOf(args[1])>0){
	        //delay the packet
        	if(args.length >1){
        		if (Integer.valueOf(args[1]) > 0){
	        		delay = Integer.valueOf(args[1]);
	        	}
	        }
        	delayPacket = true;
        	//TODO add default val of arg1
        }else if(args[0].equals("07")){
        	//duplicate sent packet
        	packNum = 0;
        	sendUsingSocket(new DatagramPacket(newmsg, length, address, port));
        	
        	
        } else if(args[0].equals("08")){
        	//lost
        	lost = true;
        	packNum=1;
        	
        }
        else if(args[0].equals("11")){
        	//pass shit
        	packNum = 1;
        }
        else if(args[0].equals("12") && Integer.valueOf(args[1])>0){
        	packNum = Integer.valueOf(args[1]);
        	countMode = true;
        }
        return new DatagramPacket(newmsg, length, address, port);
    }
	
	public void DelayPacket(DatagramPacket data){
		Thread requests = new Thread(new DelayPack(delay, data, this));
        requests.start();
		
		delayPacket=false;
		delay = 1000;
	}
	
	public void PrintErrorList(){
		System.out.print("\n\nThis is the error SIM. To choose your error, please print exactly what's between the quotations: \n");
		System.out.print("\"00\": Normal Operations\n");
		System.out.print("\"01\": Changes the first byte in the OpCode\n");
		System.out.print("\"02\": Changes the second byte in the OpCode\n");
		
		if (counter > 1) {
		    System.out.print("\"03\": Change to an invalid port number\n");
		    System.out.print("\"04\": Change to a different Address\n");
		    System.out.print("\"06\":Delay Packet\n");
		    System.out.print("\"07\":Duplicate Packet\n");
		    System.out.print("\"08\":Lose Packet\n");
		} else if (counter == 0) {
			System.out.print("\"05\": Change the mode\n");
		}
		
		System.out.print("\"11\": Always normal function\n");
		System.out.print("\"12 n\": Run 'n' number of normal function, where n is some positive int\n");
	    System.out.print("> ");
	    counter++;
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
	/**
	 * Takes in information from the server thread, and passes it to the client and vice versa   
	 */
	@Override
	public void run() {
		// Initialize the variables
		  Scanner reader = new Scanner(System.in);
		  String input;
	      byte data[] = new byte[516];
	      
	      packCount=0;
	      //Decide on the error sim
	      input = "00";
	      if(packCount == packNum){
	    	  PrintErrorList();
	    	  if(reader.hasNextLine()){
	    		  input = reader.nextLine();
	    	  }
	    	  	
	      }
	
	      
	      //create a new packet to send
	      DatagramPacket sendPacket = BringError(packet.getData(), input, packet, 69);
	      
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
	      if(!delayPacket){
	    	  sendUsingSocket(sendPacket);
	      } 
	      else{
	    	  //sendto thread
	    	  DelayPacket(sendPacket);
	    	  
	    	  
	      }
	      
	
	      System.out.println("Intermediate: packet sent");
	      boolean flag = false;
	      while(true){
	    	  DatagramPacket receivePacket;
	    	  lost=true;
	    	  while(lost){
	    		  lost = false;//set lost init, will be set back to true if packet is specified to be lost
			      //empty the data receiver
			      data = new byte[516];
			      
			      //create a new receive packet
			      receivePacket = new DatagramPacket(data, data.length);
			
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
			      
			      if(countMode){
			      	packCount++;
			      }
			      //Decide on the error sim
			      input = "00";
			      if(packCount == packNum){
			    	  PrintErrorList();
			    	  if(reader.hasNextLine()){
			    		  input = reader.nextLine();
			    	  }
			      }
			
			      
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
			      //sendUsingSocket(sendPacket);
			      //Send the data
			      if(delayPacket){
			    	  DelayPacket(sendPacket);
			      } else if(lost){
			    	  System.out.println("the packet was lost");
			      }
			      else{
			    	  //sendto thread
			    	  sendUsingSocket(sendPacket);
			    	  
			      }
	    	  }
		    
		      
		      if (flag) {
		    	  break;
		      }
		      lost=true;
		      while(lost){
		    	  
		    	  lost=false;//set init to false if the user input sets it back to true keep loop going
		    	  
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
			      input = "00";
			      if(packCount == packNum){
			    	  PrintErrorList();
			    	  if(reader.hasNextLine()){
			    		  input = reader.nextLine();
			    	  }
			      }
			      
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
			      //sendUsingSocket(sendPacket);
			      //Send the data
			      if(delayPacket){
			    	  DelayPacket(sendPacket);
			      } else if(lost){
			    	  System.out.println("the packet was lost");
			      }
			      else{
			    	  //sendto thread
			    	  sendUsingSocket(sendPacket);
			    	  
			      }
		      }
	      }
	      

	      // We're finished, so close the sockets.
	      sendReceiveSocket.close();
	      reader.close();
	}
	
}
