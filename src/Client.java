import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
	

	//Set up the packets and Sockets
   DatagramPacket sendPacket, receivePacket;
   DatagramSocket sendReceiveSocket;
    private static final byte READ_CODE[] = {0, 1};
    private static final byte WRITE_CODE[] = {0, 2};
    private static final byte ACK_CODE[] = {0, 4};
    private InetAddress address;
    private int hostPort;
   
   
   
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

    public void printMenu() {
        System.out.println("> Options:");
        System.out.println(">    read [filename] [mode] - Reads the file from the server under filename");
        System.out.println(">    write [filename] [file location] [mode] - Writes file at location to");
        System.out.println("                                                filename on server.");
        System.out.println(">    help - Prints options screen.");
    }

    public DatagramPacket createPacket(byte[] opCode, String fileName, String mode) {
        return createPacket(opCode, fileName, mode, null);
    }

    public DatagramPacket createPacket(byte[] opCode, String fileName, String mode, String location) {
        // Check that the op code is valid before creating
        if (opCode.length != 2) {
            throw new IllegalArgumentException("Op code must be length 2! Found length " + opCode.length + ".");
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        buffer.write(opCode[0]);
        buffer.write(opCode[1]);

        buffer.write(fileName.getBytes(), 0, fileName.length());
        buffer.write(0);
        buffer.write(mode.getBytes(), 0, mode.length());
        buffer.write(0);

        return new DatagramPacket(buffer.toByteArray(), buffer.toByteArray().length, address, hostPort);
    }

    public void run() {
        Scanner reader = new Scanner(System.in);
        while(true) {
            System.out.println("> Starting client...");
            printMenu();

            // Read the input from the user
            System.out.println("> ");
            String input = reader.next();

            // Parse the input and check for keywords
            String args[] = input.split(" ");

            if (args.length < 3) {
                System.out.println("> Instruction invalid length!");
            } else {
                if (args[1].toLowerCase().equals("read")) {
                    DatagramPacket packet = createPacket(READ_CODE, args[1], args[2]);
                } else if (args[1].toLowerCase().equals("write")) {
                    if ()
                }
            }


        }

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