import java.io.*;
import java.net.*;

public class ErrorGen {

   DatagramPacket sendPacket, receivePacket;
   DatagramSocket sendrecieveSocket, receiveSocket, sendSocket;
   
   public static boolean shall = true;
   
   public String changetobytes(byte msg[]){
	   String cud = "";
	   int n = 0;
	   int end = msg.length;
	   while(n != end){
		   cud = cud + msg[n] + " ";
		   n = n + 1;
	   }
	   return cud;
   }
   
   public byte[] minimi(byte msg[], int len){
	   int n = 0;
	   byte[] newmsg = new byte[len];
	   while(n!=len){
		   newmsg[n] = msg[n];
		   n++;
	   }
	   return newmsg;
   }
   
   public byte[] empty(byte msg[]){
	   int n = 0;
	   byte[] newmsg = new byte[msg.length];
	   while(n!=msg.length){
		   newmsg[n] = 0;
		   n++;
	   }
	   return newmsg;
   }

   //Initialize the sockets
   public ErrorGen()
   {
      try {
         sendrecieveSocket = new DatagramSocket();

         receiveSocket = new DatagramSocket(68);
         
         sendSocket = new DatagramSocket();
         
         // to test socket timeout (2 seconds)
         //receiveSocket.setSoTimeout(2000);
      } catch (SocketException se) {
         se.printStackTrace();
         System.exit(1);
      } 
   }

   public void receiveAndEcho()
   {
      // Initialize the variables
      byte data[] = new byte[516];
      byte mydata[];
      int clientport;
      
      while(shall){
	      receivePacket = new DatagramPacket(data, data.length);
	      System.out.println("Intermediate: Waiting for Packet.\n");
	
	      // Block until a datagram packet is received from receiveSocket.
	      try {        
	         System.out.println("Waiting...");
	         receiveSocket.receive(receivePacket);
	      } catch (IOException e) {
	         System.out.print("IO Exception: likely:");
	         System.out.println("Receive Socket Timed Out.\n" + e);
	         e.printStackTrace();
	         System.exit(1);
	      }
	
	      // Print out the data within the received packet
	      System.out.println("Intermediate: Packet received:");
	      System.out.println("From host: " + receivePacket.getAddress());
	      System.out.println("Host port: " + receivePacket.getPort());
	      clientport = receivePacket.getPort();
	      int len = receivePacket.getLength();
	      System.out.println("Length: " + len);
	      System.out.print("Containing: " );
	
	      // Form a String from the byte array.
	      String received = new String(data,0,len);
	      
	      //minimize the data
	      mydata = minimi(data,len);
	      String bytereceived = changetobytes(mydata);
	      System.out.println(received + "\n");
	      System.out.println("In bytes " + bytereceived + "\n");
	      
	      // Slow things down (wait 5 seconds)
	      try {
	          Thread.sleep(5000);
	      } catch (InterruptedException e ) {
	          e.printStackTrace();
	          System.exit(1);
	      }
	 
	      //create the packet
	      sendPacket = new DatagramPacket(data, receivePacket.getLength(),
	                               receivePacket.getAddress(), 69);
	
	      //print out the data to be sent
	      System.out.println( "Intermediate: Sending packet:");
	      System.out.println("To host: " + sendPacket.getAddress());
	      System.out.println("Destination host port: " + sendPacket.getPort());
	      len = sendPacket.getLength();
	      System.out.println("Length: " + len);
	      System.out.print("Containing: ");
	      System.out.println(new String(sendPacket.getData(),0,len));
	      System.out.println("In bytes " + bytereceived + "\n");
	      
	      //Send the data
	      try {
	         sendrecieveSocket.send(sendPacket);
	      } catch (IOException e) {
	         e.printStackTrace();
	         System.exit(1);
	      }
	
	      System.out.println("Intermediate: packet sent");
	      
	      //empty the data receiver
	      data = empty(data);
	      
	      //create a new receive packet
	      receivePacket = new DatagramPacket(data, data.length);
	
	      //wait until a new packet is received
	      try {
	         sendrecieveSocket.receive(receivePacket);
	      } catch(IOException e) {
	         e.printStackTrace();
	         System.exit(1);
	      }
	      
	      //print out the data on the received packet
	      System.out.println("Intermediate: Packet received:");
	      System.out.println("From host: " + receivePacket.getAddress());
	      System.out.println("Host port: " + receivePacket.getPort());
	      len = receivePacket.getLength();
	      System.out.println("Length: " + len);
	      System.out.print("Containing in bytes: " );
	
	      // minimize the data
	      mydata = minimi(data,len);
	      
	      //determine whether to shutdown or not
	      if(mydata[1] == 0){
	    	  shall = false;
	      }
	      bytereceived = changetobytes(mydata);
	      System.out.println(bytereceived + "\n");
	      
	      //create a new packet to send
	      sendPacket = new DatagramPacket(mydata, receivePacket.getLength(),
	              receivePacket.getAddress(), clientport);
	      
	      //print out the data to be sent
	      System.out.println( "Intermediate: Sending packet:");
	      System.out.println("To host: " + sendPacket.getAddress());
	      System.out.println("Destination host port: " + sendPacket.getPort());
	      len = sendPacket.getLength();
	      System.out.println("Length: " + len);
	      System.out.print("Containing: ");
	      System.out.println("In bytes " + bytereceived + "\n");
	      
	   // Send the datagram packet to the client via the send socket. 
	      try {
	         sendSocket.send(sendPacket);
	      } catch (IOException e) {
	         e.printStackTrace();
	         System.exit(1);
	      }
      }
      

      // We're finished, so close the sockets.
      sendrecieveSocket.close();
      receiveSocket.close();
      sendSocket.close();
   }

   public static void main( String args[] )
   {
	   //while(shall){
		   ErrorGen c = new ErrorGen();
		   c.receiveAndEcho();
	   //}
   }
}
