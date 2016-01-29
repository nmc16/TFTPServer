import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.lang.*;
import java.util.Scanner;
import java.util.Iterator;
import java.io.*;

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
	
	public boolean writeToFile() throws IOException{
		int len = 0;
		int curr=2;
		String contents="";
		
		while(data.getData()[curr] != 0){
			len++;
			curr++;
		}
		byte file[] = new byte[len];
		
		System.arraycopy(data.getData(), 2, file, 0, len);
		
		  
		  String fileName = (new String(file));
		  
		  File f = new File(fileName);
		  
		  try (Scanner s = new Scanner(f).useDelimiter("\\Z")) {
			  contents = s.next();
		  }
		  catch(FileNotFoundException e){
			  e.printStackTrace();
		  }
		  
		  //System.out.println("**********************"+ contents);
		  
		  try {
				//fos = new FileOutputStream(fout);
			  	File outFile = new File("out.txt");
				FileWriter fw = new FileWriter(outFile);
				try {
					//System.out.println("Hi "+s);
					fw.write(contents);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				fw.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		  
		  
		
		return true;
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
	    try{
	    	writeToFile();
	    }
	    catch(IOException e){
	    	e.printStackTrace();
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
