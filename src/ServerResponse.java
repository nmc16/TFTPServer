import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

public class ServerResponse implements Runnable {
    private static final int DATA_SIZE = 516;
    private static final byte RRQ = 1;
    private static final byte WRQ = 2;
    private static final byte DATA = 3;
    private static final byte ACK = 4;
    private byte highestBlock = 0;
    private byte lowestBlock = 0;
	private DatagramPacket data;
	
	public ServerResponse(DatagramPacket data) {
		this.data = data;
	}

	@Override
	public void run() {
	    byte[] reply = new byte[DATA_SIZE];
	    
	    if (data.getData()[1] == RRQ) {
	        reply[0] = 0;
	        reply[1] = DATA;
	        
	        lowestBlock++;
	        reply[2] = highestBlock;
	        reply[3] = lowestBlock;
	        
	    } else if (data.getData()[1] == ACK){
	        reply[0] = 0;
            reply[1] = DATA;
            lowestBlock = data.getData()[3];
            highestBlock = data.getData()[2];
            
            if (data.getData()[3] == 255) {
                lowestBlock = 0;
                highestBlock++;
            } else {
                lowestBlock++;
            }
            
            reply[2] = highestBlock;
            reply[3] = lowestBlock;
            
	    } else {
	        reply[0] = 0;
            reply[1] = ACK;
            reply[2] = highestBlock;
            reply[3] = lowestBlock;
	    }
	    
	    
	    //Construct a new packet
	    DatagramPacket responseData = new DatagramPacket(reply, reply.length,
	                                                     data.getAddress(), data.getPort());

	    //print out the data on the sent packet
	    System.out.println( "Server: Sending packet:");
	    System.out.println("To host: " + responseData.getAddress());
	    System.out.println("Destination host port: " + responseData.getPort());
	    int len = responseData.getLength();
	    System.out.println("Length: " + len);
	    System.out.print("Containing: ");
	    System.out.println(Arrays.toString(reply));
	      
	    //SEND the PACKET
	    try {
	        DatagramSocket sendSocket = new DatagramSocket();
	        sendSocket.send(responseData);
	        sendSocket.close();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
}
