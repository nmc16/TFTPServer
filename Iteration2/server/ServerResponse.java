package server;

import exception.AddressException;
import exception.ExistsException;
import exception.IllegalOPException;

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
/**
 * 
 * The thread created by the server, breaks up data for read and write request from the client and sends to the error sim thread
 *
 */
public class ServerResponse implements Runnable {
    private static final int DATA_SIZE = 516;
    private static final byte RRQ = 1;
    private static final byte WRQ = 2;
    private static final byte DATA = 3;
    private static final byte ACK = 4;
    private static final byte EC0[] = {0, 0};
    private static final byte EC1[] = {0, 1};
    private static final byte EC2[] = {0, 2};
    private static final byte EC3[] = {0, 3};
    private static final byte EC4[] = {0, 4};
    private static final byte EC5[] = {0, 5};
    private static final byte EC6[] = {0, 6};
    private static final byte EC7[] = {0, 7};
    
	private DatagramPacket initialPacket;
	private DatagramPacket data;
	private DatagramSocket socket;
	
	private InetAddress address;
	private int port;
	
	public ServerResponse(DatagramPacket data) {
		this.initialPacket = data;
		this.data = data;
	    try {
	    	Random r = new Random();
	        this.address = data.getAddress();
	        this.port = data.getPort();
	        socket = new DatagramSocket(r.nextInt(65553));
	    } catch (IOException e) {
	        e.printStackTrace();
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
     * Creates datagram error packet using information passed.
     * 
     * @param errCode 2 byte Error Code
     * @param address to send to the Error Packet to
     * @param port port to send Packet to
     * @return void
     */
    public void sendERRPacket(byte[] errCode,  InetAddress address, String tempString, int port) {
        // Check that the Error code is valid before creating
        if (errCode.length != 2) {
            throw new IllegalArgumentException("Op code must be length 2! Found length " + errCode.length + ".");
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        buffer.write(0);
        buffer.write(5);
        buffer.write(errCode[0]);
        buffer.write(errCode[1]);
        buffer.write(tempString.getBytes(), 0, tempString.length());
        buffer.write(0);

        DatagramPacket ErrPack = new DatagramPacket(buffer.toByteArray(), buffer.toByteArray().length, address, port);
        
        try {
	        socket.send(ErrPack);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
    }
	
	
	/**
	 * creates a new text file if it doesn't already exist
	 * @param file file to check if it exists
	 */
	public void createFile(File file) throws ExistsException {
		try {
			if(!file.exists()) {
                // TODO should check the value this returns
				file.createNewFile();
			}
			else{
				throw new ExistsException("File already exists");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 
	 * @return returns the file corresponding to the client request
	 */
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
	/**
	 * Reads file 512 bytes at a time from the file of the clients requests choice
	 * @throws IllegalOPException 
	 */
	public void readFile() throws FileNotFoundException, SecurityException, AddressException, IllegalOPException {
		byte[] block = {0, 0};
		boolean flag = false;
		int newsize;
        long newsize2;

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
                if (!file.canRead()) {
                    throw new SecurityException("Access Denied: File " + file.getAbsolutePath() + "does not have" +
                                                " read access");
                }

				try {
					RandomAccessFile f = new RandomAccessFile(file, "r");
					f.seek((blockNumber - 1) * 512);
					if((f.length() - f.getFilePointer()) < 512 && (f.length() - f.getFilePointer()) > 0){
						newsize2 = f.length() - f.getFilePointer();
						newsize = (int) newsize2;
						System.out.println("IN here");
						buffer = new byte[newsize];
					}
					int i = f.read(buffer, 0, buffer.length);
					reply.write(buffer, 0, buffer.length);
	                f.close();
	                
	                if (i < 512) {
	                	flag = true;
	                }
	                
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
		    	if(data.getAddress() != this.address){
		    		throw new AddressException("unknown Transfer Id");
		    	}
		    	if(data.getData()[0] != 0 || data.getData()[1] != 3){
			    	throw new IllegalOPException("Not vaild ACK OpCode");
			    }
		    }
		} 
	}
	/**
	 * for write requests, send data packet for the client to wrote 2 512 bytes at a time
	 * @throws IllegalOPException 
	 */
	public void writeToFile() throws SecurityException, IllegalOPException, ExistsException {
        File file = getFile();
        createFile(file);

        if (!file.canWrite()) {
            throw new SecurityException("Access Denied: File " + file.getAbsolutePath() + "does not have" +
                                        " write access");
        }
		
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
	    	
	    	
	    	byte datamin[] = minimi(receivePacket.getData(), receivePacket.getLength());
	    	//print out the data on the sent packet
		    System.out.println( "server.Server: Received packet:");
		    System.out.println("From host: " + receivePacket.getAddress());
		    System.out.println("Host port: " + receivePacket.getPort());
		    System.out.println("Length: " + receivePacket.getLength());
		    System.out.println("Containing: " + Arrays.toString(datamin) + "\n");
		    
		    if(datamin[0] != 0 || datamin[1] != 3){
		    	throw new IllegalOPException("Not vaild DATA OpCode");
		    }
		    
	    	block[0] = datamin[2];
	    	block[1] = datamin[3];
	    	
	    	byte[] b = Arrays.copyOfRange(datamin, 3, datamin.length);
	    	if (datamin.length < 512) {
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
	    	try {
	    		readFile();
	    	} catch (FileNotFoundException e) {
	    		sendERRPacket(EC1, address, e.getMessage(), port);
	    	} catch (SecurityException e) {
	    		sendERRPacket(EC2, address, e.getMessage(), port);
	    	} catch (IllegalOPException e) {
	    		sendERRPacket(EC4, address, e.getMessage(), port);
	    	} catch (AddressException e) {
	    		sendERRPacket(EC5, address, e.getMessage(), port); 
	    	}
	    } else {
	    	try {
	    		writeToFile();
	    	} catch (SecurityException e) {
	    		sendERRPacket(EC2, address, e.getMessage(), port);
	    	} catch (IllegalOPException e) {
	    		sendERRPacket(EC4, address, e.getMessage(), port);
	    	} catch (ExistsException e) {
                sendERRPacket(EC6, address, e.getMessage(), port);
            }
	    }
	    
	}
}
