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
    		System.out.println("BLOCK NUMBER: " + blockNumber);
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
	                
	                if (i != 512) {
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
		    System.out.println( "Server: Sending packet:");
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
		    	
		    	System.out.println("WAITING");
		    	buffer = new byte[512];
		    	DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
		    	try {
		    		socket.receive(receivePacket);
		    		data = receivePacket;
		    	} catch (IOException e) {
		    		e.printStackTrace();
		    	}
		    	System.out.println("GOTCHA");
		    }
		    
		}
		System.out.println("WE OUT");
        
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
		  if(!f.exists()) {
		      f.createNewFile();
		  } 
		  
		  try (Scanner s = new Scanner(f).useDelimiter("\\Z")) {
			  contents = s.next();
		  }
		  catch(FileNotFoundException e){
			  e.printStackTrace();
		  }
		  
		  //System.out.println("**********************"+ contents);
		  
		  try {
				//fos = new FileOutputStream(fout);
				FileWriter fw = new FileWriter(f);
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
	    
	    if (initialPacket.getData()[1] == RRQ) {
	        readFile();
	    } else {
            createFile(getFile());
	    }
	    
	}
}
