package server;

import exception.AddressException;
import exception.EPException;
import exception.ExistsException;
import exception.IllegalOPException;
import shared.Helper;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Random;
import java.io.*;

/**
 * The thread created by the server, breaks up data for read and write 
 * request from the client and sends to the client that it is delegated to.
 * 
 * @version 2
 * @author Team6
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
	
	private int timeout = 2000;
	
	private InetAddress address;
	private int port;
	private int currDataBlock, currACKBlock;
	
	//TODO re add in timeout set
	
	
	public ServerResponse(DatagramPacket data) {
		this.initialPacket = data;
		this.data = data;
		
	    try {
	    	Random r = new Random();
	        this.address = data.getAddress();
	        this.port = data.getPort();
	        socket = new DatagramSocket(r.nextInt(65500));
	        
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
    
    /**
     * Creates datagram error packet using information passed.
     * 
     * @param errCode 2 byte Error Code
     * @param address to send to the Error Packet to
     * @param port port to send Packet to
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
	 * Reads file 512 bytes at a time from the file of the clients requests choice
	 * 
	 * @throws IllegalOPException 
	 */
	public void readFile() throws FileNotFoundException, SecurityException, AddressException, IllegalOPException, EPException {
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
			
			File file = Helper.getFile(initialPacket);

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
		    Helper.printPacketData(responseData, "Server (" + socket.getLocalPort() + "): Sending packet", ServerSettings.verbose);
		    
			//SEND the PACKET
		    try {
		        socket.send(responseData);
		        //socket.setSoTimeout(1000);
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		    
		    if (!flag) {
		    	buffer = new byte[512];
		    	DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
			    
		    	boolean cont = false;
		    	while(!cont){
		    		cont = true;
		    		try {
			    		socket.receive(receivePacket);
			    		data = receivePacket;
			    		
			    		//TODO if the data # does not equal currDataBlock # (or +1?) then ignore it and wait for a data packet again
			    	} catch(SocketTimeoutException e){
				    	e.printStackTrace();
				    	
				    	//SEND the PACKET
					    try {
					        socket.send(responseData);
					    } catch (IOException e1) {
					        e1.printStackTrace();
					    }
					    //try again
					    cont = false;
					    
					    
					    
				    } catch (IOException e) {
			    		e.printStackTrace();
			    	}
			    }
		    	
		    	if (data.getData()[0] == 0 && data.getData()[1] == 5) {
		    		throw new EPException("Error packet received from Client!", receivePacket);
		    	}
		    	if(data.getData()[0] != 0 || data.getData()[1] != 4){
			    	throw new IllegalOPException("Not vaild ACK OpCode");
			    }
		    	if(data.getPort() != this.port || !data.getAddress().equals(this.address)){
		    		throw new AddressException("Unknown TID/Address");
		    	}
		    }
		} 
	}
	
	/**
	 * for write requests, send data packet for the client to wrote 2 512 bytes at a time
	 * @throws IllegalOPException 
	 */
	public void writeToFile() throws SecurityException, IllegalOPException, ExistsException, EPException, AddressException {
        File file = Helper.getFile(initialPacket);
        Helper.createFile(file);

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
		    Helper.printPacketData(responseData, "Server (" + socket.getLocalPort() + "): Sending packet", ServerSettings.verbose);
		    
			//SEND the PACKET
		    try {
		        socket.send(responseData);
		        //socket.setSoTimeout(1000);
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		    
		    if (flag) {
		    	break;
		    }
		    
		    byte[] buffer = new byte[516];
	    	DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
	    	boolean cont = false;
	    	while(!cont){
	    		cont = true;	
	    		try {
		    		socket.receive(receivePacket);
		    		data = receivePacket;
		    		
		    		//TODO if ack/data is off go back to waiting for a recieve (cont = false)
		    		
		    	}catch(SocketTimeoutException e){
			    	e.printStackTrace();
			    	//SEND the PACKET
				    try {
				        socket.send(responseData);
				    } catch (IOException e1) {
				        e1.printStackTrace();
				    }
				    //try again
				    cont = false;
			    } catch (IOException e) {
		    		e.printStackTrace();
		    	}
	    	}
	    	
	    	
	    	byte datamin[] = Helper.minimi(receivePacket.getData(), receivePacket.getLength());
	    	//print out the data on the sent packet
	    	Helper.printPacketData(receivePacket, "Server (" + socket.getLocalPort() + "): Received Packet", ServerSettings.verbose);
		    
		    if (datamin[0] == 0 && datamin[1] == 5) {
		    	throw new EPException("Error packet received from Client!", receivePacket);
			} else if(datamin[0] != 0 || datamin[1] != 3){
		    	throw new IllegalOPException("Not vaild DATA OpCode");
		    }
		    if (data.getData()[0] == 0 && data.getData()[1] == 5) {
	    		throw new EPException("Error packet received from Client!", receivePacket);
	    	}
	    	if(!data.getAddress().equals(this.address)){
	    		throw new AddressException("unknown Transfer Id");
	    	}
	    	if(data.getPort() != this.port){
	    		throw new AddressException("unknown Port");
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
	    	} catch (EPException e) {
	    		Helper.printPacketData(e.getPacket(), "Server Thread (" + socket.getLocalPort() + "): Received Error Packet, Shutting down", true);
	    	}
	    } else {
	    	try {
	    		writeToFile();
	    	} catch (SecurityException e) {
	    		sendERRPacket(EC2, address, e.getMessage(), port);
	    	} catch (IllegalOPException e) {
	    		sendERRPacket(EC4, address, e.getMessage(), port);
	    	} catch (AddressException e) {
	    		sendERRPacket(EC5, address, e.getMessage(), port); 
	    	} catch (ExistsException e) {
                sendERRPacket(EC6, address, e.getMessage(), port);
            } catch (EPException e) {
            	Helper.printPacketData(e.getPacket(), "Server Thread (" + socket.getLocalPort() + "): Received Error Packet Shutting down", true);
            }
	    }
	    
	}
}
