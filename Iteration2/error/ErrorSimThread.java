package error;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
/**
 * The Thread for the Error simulator, takes init input from the Error simulator then send the request to the server (cont communication with server thread)
 *
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
	
	/**
     * minimizes byte array request
     * @param msg client request (read or write)
     * @param len len of msg
     * @return minimized byte array
     */
    public byte[] minimi(byte msg[], int len) {
 	   int n = 0;
 	   byte[] newmsg = new byte[len];
 	   while(n!=len){
 		   newmsg[n] = msg[n];
 		   n++;
 	   }
 	   return newmsg;
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
	      byte data[] = new byte[516];
	      
	      //create the packet
	      DatagramPacket sendPacket = new DatagramPacket(packet.getData(), packet.getLength(),
	                               					     packet.getAddress(), 69);
	      
	      byte datamins[] = minimi(sendPacket.getData(), sendPacket.getLength());
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
		      
		      byte dataminr[] = minimi(receivePacket.getData(), receivePacket.getLength());
		      //print out the data on the received packet
		      System.out.println("Intermediate: Packet received:");
		      System.out.println("From host: " + receivePacket.getAddress());
		      System.out.println("Host port: " + receivePacket.getPort());
		      System.out.println("Length: " + receivePacket.getLength());
		      System.out.println("Containing: " + new String(receivePacket.getData()));
		      System.out.println("In bytes " + Arrays.toString(dataminr) + "\n\n");
		
		      
		      //create a new packet to send
		      sendPacket = new DatagramPacket(data, receivePacket.getLength(),
		              						  receivePacket.getAddress(), clientPort);
		      
		      byte datamins2[] = minimi(sendPacket.getData(), sendPacket.getLength());
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
		      
		      //create a new packet to send
		      sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
		              						  receivePacket.getAddress(), serverPort);
		      
		      byte datamins3[] = minimi(sendPacket.getData(), sendPacket.getLength());
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
	}
	
}
