// SimpleEchoServer.java
// This class is the server side of a simple echo server based on
// UDP/IP. The server receives from a client a packet containing a character
// string, then echoes the string back to the client.
// Last edited January 9th, 2016

import java.io.*;
import java.net.*;

public class Server {

	//initialize the sockets and packets
   DatagramPacket sendPacket, receivePacket;
   DatagramSocket receiveSocket;
   
   public static boolean shall = true;
   
   //change a byte array into a string of vyte numbers
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
   
   //Method to make sure that the data is in the proper format
   public boolean verify(byte msg[]){
	   int n = msg.length;
	   int m = 2;
	   int q = 0;
	   
	   if(msg[0] != 0){
		   return false;
	   }

	   if(msg[1] == 3 || msg[1] == 4){
		   return true;
	   } else if (msg[1] != 1 && msg[1] != 2) {
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
			   }
			   else {
				   if(m+1 != n){
					   return false;
				   }
			   }
			   q++;
		   }
		   m++;
	   }
	   if(q!= 2){
		   return false;
	   }
	   return true;
   }
   
   //minimize the data
   public byte[] minimi(byte msg[], int len) {
	   int n = 0;
	   byte[] newmsg = new byte[len];
	   while(n!=len){
		   newmsg[n] = msg[n];
		   n++;
	   }
	   return newmsg;
   }

   //start up the sockets
   public Server() {
      try {
         receiveSocket = new DatagramSocket(69);
     
      } catch (SocketException se) {
         se.printStackTrace();
         System.exit(1);
      } 
   }

   public void receiveAndEcho() {
       while(shall){
    	  //Initialize the required variables
          byte data[] = new byte[100];
          byte mydata[];
          receivePacket = new DatagramPacket(data, data.length);
          System.out.println("Server: Waiting for Packet.\n");
    
          // Block until a datagram packet is received from receiveSocket.
          try {        
             System.out.println("Waiting..."); // so we know we're waiting
             receiveSocket.receive(receivePacket);
          } catch (IOException e) {
             System.out.print("IO Exception: likely:");
             System.out.println("Receive Socket Timed Out.\n" + e);
             e.printStackTrace();
             System.exit(1);
          }
    
          // Print out the data on the recieved package
          System.out.println("Server: Packet received:");
          System.out.println("From host: " + receivePacket.getAddress());
          System.out.println("Host port: " + receivePacket.getPort());
          int len = receivePacket.getLength();
          System.out.println("Length: " + len);
          System.out.print("Containing: " );
    
          // Form a String from the byte array.
          String received = new String(data,0,len);
          mydata = minimi(data,len);
          //minimize the data
          String bytereceived = changetobytes(mydata);
          System.out.println(received + "\n");
          System.out.println("In bytes " + bytereceived + "\n");
          
          //verify the data
          if(verify(mydata)){
        	  System.out.println("verified\n");
        	  Thread clientThread = new Thread(new ServerResponse(receivePacket));
        	  clientThread.start();
          } else{
              //terminate the program
              throw new RuntimeException("Invalid data request");
          }
    
          System.out.println("Server: packet sent");
      }
      //close it up
      receiveSocket.close();
   }

   public static void main(String args[]) {
       Server c = new Server();
	   c.receiveAndEcho();
   }
}