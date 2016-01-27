import java.io.*;
import java.net.*;

public class Client {
	

	//Set up the packets and Sockets
   DatagramPacket sendPacket, receivePacket;
   DatagramSocket sendReceiveSocket;
   
   //Set up a chooser of whether or not it's read or write
   public static int readorwrite = 0;
   public static int numberotimes = 1;
   
   
   
   
   //A Method to Change bytes into a readable string
   public String changetobytes(byte msg[]){
	   String cud = "";
	   int n = 0;
	   int end = msg.length;
	   
	   //Go through every byte until the end
	   while(n != end){
		   cud = cud + msg[n] + " ";
		   n = n + 1;
	   }
	   return cud;
   }
   
   
   
   //A method to minimize the data recieved into only what we want
   public byte[] minimi(byte msg[], int len){
	   int n = 0;
	   byte[] newmsg = new byte[len];
	   while(n!=len){
		   newmsg[n] = msg[n];
		   n++;
	   }
	   return newmsg;
   }
   
   
   //A method to change a string of bytes into the appropriate format for the assignment
   public byte[] changetoREAD(byte msg[]){
	   byte[] readmsg = new byte[msg.length + 12];
	   String s2 = "netascii";
	   byte[] msg2 = s2.getBytes();
	   
	   //Change the first 2 bytes
	   readmsg[0] = 0;
	   readmsg[1] = 1;
	   int n = 0;
	   int m = 0;
	   int end = msg.length;
	   
	   //At on the string message
	   while(n != end){
		   readmsg[n+2] = msg[n];
		   n = n + 1;
	   }
	   //Add on the second zero byte
	   readmsg[n+2] = 0;
	   n = n + 1;
	   end = n + 8;
	   //Add on the mode (ie netascii)
	   while(n != end){
		   readmsg[n+2] = msg2[m];
		   n = n + 1;
		   m = m + 1;
	   }
	   //Add the terminating zero
	   readmsg[n+2] = 0;
	   return readmsg;
   }


   public Client()
   {
      try {
         // Construct a datagram socket and bind it to any available 
         // port on the local host machine. This socket will be used to
         // send and receive UDP Datagram packets.
         sendReceiveSocket = new DatagramSocket();
      } catch (SocketException se) {   // Can't create the socket.
         se.printStackTrace();
         System.exit(1);
      }
   }

   public void sendAndReceive()
   {
      // Prepare a DatagramPacket and send it via sendReceiveSocket
      // to port 68 on the destination host.
 
      String s = "Text.txt" + Integer.toString(numberotimes);
      System.out.println("Client: sending a packet containing:\n" + s);

      //Change the message to bytes
      byte msg[] = s.getBytes();
      
      //change the message to READ format
      byte mymsg[] = changetoREAD(msg);
      if(readorwrite == 0){
    	  //If necessary, change it into write format
    	 mymsg[1] = 2;
    	 readorwrite = 1;
      }
      else if(readorwrite == 1){
    	  readorwrite = 0;
      }
      else{
    	  mymsg[1] = 4;
      }

      
      //Create the Datagram packet to send to the intermediate
      try {
         sendPacket = new DatagramPacket(mymsg, mymsg.length,
                                         InetAddress.getLocalHost(), 68);
      } catch (UnknownHostException e) {
         e.printStackTrace();
         System.exit(1);
      }

      //Print out the info on the packet
      System.out.println("Client: Sending packet:");
      System.out.println("To host: " + sendPacket.getAddress());
      System.out.println("Destination host port: " + sendPacket.getPort());
      int len = sendPacket.getLength();
      System.out.println("Length: " + len);
      System.out.print("Containing: ");
      System.out.println(s);
      System.out.println("in bytes it is: " + changetobytes(mymsg) + "\n\n");

      // Send the datagram packet to the intermediate via the send/receive socket. 

      try {
         sendReceiveSocket.send(sendPacket);
      } catch (IOException e) {
         e.printStackTrace();
         System.exit(1);
      }

      System.out.println("Client: Packet sent.\n");

      // Construct a DatagramPacket for receiving packets up 
      // to 100 bytes long (the length of the byte array).

      byte data[] = new byte[100];
      byte minidat[];
      receivePacket = new DatagramPacket(data, data.length);

      try {
         // Block until a datagram is received via sendReceiveSocket.  
         sendReceiveSocket.receive(receivePacket);
      } catch(IOException e) {
         e.printStackTrace();
         System.exit(1);
      }

      // Process the received datagram.
      System.out.println("Client: Packet received:");
      System.out.println("From host: " + receivePacket.getAddress());
      System.out.println("Host port: " + receivePacket.getPort());
      len = receivePacket.getLength();
      System.out.println("Length: " + len);
      System.out.print("Containing: ");
      
      minidat = minimi(data,len);

      // Form a String from the byte array.   
      System.out.println(changetobytes(minidat));

      // We're finished, so close the socket.
      sendReceiveSocket.close();
   }

   public static void main(String args[])
   {
	   while(numberotimes != 10){
		   Client c = new Client();
		   c.sendAndReceive();
		   numberotimes++;
	   }
	   readorwrite = 5;
	   Client c = new Client();
	   c.sendAndReceive();
   }
}