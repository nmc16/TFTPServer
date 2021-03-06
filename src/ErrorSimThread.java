import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

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
	   
	public boolean checkIfDone(byte[] buffer) {
		if (buffer[buffer.length - 1] == 0 && buffer[1] == 3) {
			return true;
		}
		return false;
	}
	
	@Override
	public void run() {
		// Initialize the variables
	      byte data[] = new byte[516];
	      
	      //create the packet
	      DatagramPacket sendPacket = new DatagramPacket(packet.getData(), packet.getLength(),
	                               					     packet.getAddress(), 69);
	      //print out the data to be sent
	      System.out.println( "Intermediate: Sending packet:");
	      System.out.println("To host: " + sendPacket.getAddress());
	      System.out.println("Destination host port: " + sendPacket.getPort());
	      System.out.println("Length: " + sendPacket.getLength());
	      System.out.print("Containing: ");
	      System.out.println(new String(sendPacket.getData()));
	      System.out.println("In bytes " + Arrays.toString(sendPacket.getData()) + "\n");
	      
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
		      
		      //print out the data on the received packet
		      System.out.println("Intermediate: Packet received:");
		      System.out.println("From host: " + receivePacket.getAddress());
		      System.out.println("Host port: " + receivePacket.getPort());
		      System.out.println("Length: " + receivePacket.getLength());
		      System.out.println("Containing: " + new String(receivePacket.getData()));
		      System.out.println("In bytes " + Arrays.toString(receivePacket.getData()) + "\n\n");
		
		      
		      //create a new packet to send
		      sendPacket = new DatagramPacket(data, receivePacket.getLength(),
		              						  receivePacket.getAddress(), clientPort);
		      
		      //print out the data to be sent
		      System.out.println( "Intermediate: Sending packet:");
		      System.out.println("To host: " + sendPacket.getAddress());
		      System.out.println("Destination host port: " + sendPacket.getPort());
		      System.out.println("Length: " + sendPacket.getLength());
		      System.out.println("Containing: " + new String(sendPacket.getData()));
		      System.out.println("In bytes " + Arrays.toString(sendPacket.getData()) + "\n");
		      
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
		      
		      //create a new packet to send
		      sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
		              						  receivePacket.getAddress(), serverPort);
		      
		      //print out the data to be sent
		      System.out.println("Intermediate: Sending packet:");
		      System.out.println("To host: " + sendPacket.getAddress());
		      System.out.println("Destination host port: " + sendPacket.getPort());
		      System.out.println("Length: " + sendPacket.getLength());
		      System.out.println("Containing: " + new String(sendPacket.getData()));
		      System.out.println("In bytes " + Arrays.toString(sendPacket.getData()) + "\n");
		      
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
	}
	
}
