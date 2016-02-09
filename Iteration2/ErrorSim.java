import java.io.*;
import java.net.*;
import java.util.Arrays;
/**
 *Main error simulator creates a ErroSimThread for each new client request 
 *
 */
public class ErrorSim {

	private DatagramPacket receivePacket;
	private DatagramSocket receiveSocket;
   
	public static boolean shall = true;
   
	//Initialize the sockets
	public ErrorSim() {
		try {
			receiveSocket = new DatagramSocket(68);
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
	 * Receives new client request and creates a errorSim thread for it
	 */
	public void receiveAndEcho() {
		// Initialize the variables
		byte data[] = new byte[516];
      
		while(shall){
			receivePacket = new DatagramPacket(data, data.length);
			System.out.println("Intermediate: Waiting for Packet.\n");
	
			// Block until a datagram packet is received from receiveSocket.
			try {        
				receiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			byte datamin[] = minimi(receivePacket.getData(), receivePacket.getLength());
	
			// Print out the data within the received packet
			System.out.println("Intermediate: Packet received:");
			System.out.println("From host: " + receivePacket.getAddress());
			System.out.println("Host port: " + receivePacket.getPort());
			System.out.println("Length: " + receivePacket.getLength());
			System.out.println("Containing: " + new String(receivePacket.getData()));
			System.out.println("In bytes " + Arrays.toString(datamin) + "\n\n");
	      
			Thread t = new Thread(new ErrorSimThread(receivePacket));
			t.start();
		}
      
		receiveSocket.close();
	}
	
	public static void main(String args[]) {
		ErrorSim c = new ErrorSim();
		c.receiveAndEcho();
	}
}
