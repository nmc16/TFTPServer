import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.lang.*;
import java.util.Scanner;
import java.util.Iterator;
import java.util.Random;
import java.io.*;

public class ServerResponse implements Runnable {
    private static final int DATA_SIZE = 516;
    private static final byte RRQ = 1;
    private static final byte WRQ = 2;
    private static final byte DATA = 3;
    private static final byte ACK = 4;
	private DatagramPacket initialPacket;
	private DatagramPacket data;
	private DatagramSocket socket;
	
	public ServerResponse(DatagramPacket data) {
		this.initialPacket = data;
		this.data = data;
	    try {
	    	Random r = new Random();
	        InetAddress address = InetAddress.getLocalHost();
	        socket = new DatagramSocket(r.nextInt(65553));
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	public void createFile(File file) {
		try {
			if(!file.exists()) {
				file.createNewFile();
			} 
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public File getFile() {
		int len = 0;
		int curr=2;
		
		while(initialPacket.getData()[curr] != 0){
			len++;
			curr++;
		}
		
		byte file[] = new byte[len];
		System.arraycopy(initialPacket.getData(), 2, file, 0, len);

		String fileName = (new String(file));
		  
		File f = new File(fileName);
		
		return f;
	}
	
	public void readFile() {
		byte[] block = {0, 0};
		boolean flag = false;
		
		while(!flag) {
			ByteArrayOutputStream reply = new ByteArrayOutputStream();
		
			block[0]++;
			if (block[0] == 0) {
				block[1]++;
			}
			
			int blockNumber = (block[1] & 0xFF) << 8 | (block[0] & 0xFF);
			reply.write(0);
			reply.write(DATA);
			reply.write(block, 0, block.length);
        
			byte[] buffer = new byte[512];
			File file = getFile();
			if (file.exists()) {
				try {
					RandomAccessFile f = new RandomAccessFile(file, "r");
					f.seek((blockNumber - 1) * 512);
					int i = f.read(buffer, 0, buffer.length);
					reply.write(buffer, 0, buffer.length);
	                f.close();
	                
	                if (i < 512) {
	                	flag = true;
	                }
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				break;
			}
			
			//Construct a new packet
		    DatagramPacket responseData = new DatagramPacket(reply.toByteArray(), reply.toByteArray().length,
		                                                     data.getAddress(), data.getPort());
		    //print out the data on the sent packet
		    System.out.println( "server.Server: Sending packet:");
		    System.out.println("To host: " + responseData.getAddress());
		    System.out.println("Destination host port: " + responseData.getPort());
		    int len = responseData.getLength();
		    System.out.println("Length: " + len);
		    System.out.println("Containing: " + Arrays.toString(reply.toByteArray()));
		    
			//SEND the PACKET
		    try {
		        socket.send(responseData);
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		    
		    if (!flag) {
		    	buffer = new byte[512];
		    	DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
		    	try {
		    		socket.receive(receivePacket);
		    		data = receivePacket;
		    	} catch (IOException e) {
		    		e.printStackTrace();
		    	}
		    }
		} 
	}
	
	public void writeToFile() {
		File file = getFile();
		createFile(file);
		
		byte[] block = {0, 0};
		boolean flag = false;
		
		while(true) {
			ByteArrayOutputStream reply = new ByteArrayOutputStream();
			reply.write(0);
			reply.write(ACK);
			reply.write(block, 0, block.length);
			
			//Construct a new packet
		    DatagramPacket responseData = new DatagramPacket(reply.toByteArray(), reply.toByteArray().length,
		                                                     data.getAddress(), data.getPort());
		    //print out the data on the sent packet
		    System.out.println( "server.Server: Sending packet:");
		    System.out.println("To host: " + responseData.getAddress());
		    System.out.println("Destination host port: " + responseData.getPort());
		    System.out.println("Length: " + responseData.getLength());
		    System.out.println("Containing: " + Arrays.toString(reply.toByteArray()) + "\n\n");
		    
			//SEND the PACKET
		    try {
		        socket.send(responseData);
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		    
		    if (flag) {
		    	break;
		    }
		    
		    byte[] buffer = new byte[516];
	    	DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
	    	try {
	    		socket.receive(receivePacket);
	    		data = receivePacket;
	    	} catch (IOException e) {
	    		e.printStackTrace();
	    	}
	    	
	    	//print out the data on the sent packet
		    System.out.println( "server.Server: Received packet:");
		    System.out.println("From host: " + receivePacket.getAddress());
		    System.out.println("Host port: " + receivePacket.getPort());
		    System.out.println("Length: " + receivePacket.getLength());
		    System.out.println("Containing: " + Arrays.toString(receivePacket.getData()));
		    
	    	block[0] = receivePacket.getData()[2];
	    	block[1] = receivePacket.getData()[3];
	    	
	    	byte[] b = Arrays.copyOfRange(receivePacket.getData(), 3, buffer.length);
	    	if (b[b.length - 1] == 0) {
	    		flag = true;
	    	}
	    	
	    	String contents = new String(b);
	    	
	    	try {
				FileWriter fw = new FileWriter(file, true);
				fw.write(contents);
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
	}

	@Override
	public void run() {
	    
	    if (initialPacket.getData()[1] == RRQ) {
	        readFile();
	    } else {
            writeToFile();
	    }
	    
	}
}
