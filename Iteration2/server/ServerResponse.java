package server;

import exception.AddressException;
import exception.EPException;
import exception.ExistsException;
import exception.IllegalOPException;
import shared.DataHelper;
import shared.FileHelper;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.security.AccessControlException;
import java.security.AccessController;
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
    private static final byte RRQ = 1;
    private static final byte DATA = 3;
    private static final byte ACK = 4;
    private static final byte EC1[] = {0, 1};
    private static final byte EC2[] = {0, 2};
    private static final byte EC3[] = {0, 3};
    private static final byte EC4[] = {0, 4};
    private static final byte EC5[] = {0, 5};
    private static final byte EC6[] = {0, 6};
    
	private DatagramPacket initialPacket;
	private DatagramPacket data;
	private DatagramSocket socket;
	
	
	private InetAddress address;
	private int port;
	private int currDataBlock=-1, currACKBlock=-1;
	private int timeOutCount = 0;
	
	
	private int opType;
	private String currFile;
	
	//TODO re add in timeout set
	
	
	public ServerResponse(DatagramPacket data) {
		this.initialPacket = data;
		this.data = data;
		
		if (initialPacket.getData()[1] == RRQ) {
			opType = 1;
		} else {
			opType = 2;
		}
		
		currFile = FileHelper.getFileFromPacket(initialPacket).getName();
		
	    try {
	    	Random r = new Random();
	        this.address = data.getAddress();
	        this.port = data.getPort();
	        socket = new DatagramSocket(r.nextInt(65500));
	        
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	
	public int getOpType(){
		return opType;
	}
	
	public String getCurrFile(){
		return currFile;
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
	public void readFile() throws IOException {
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
			char[] chars = new char[512];
			
			File file = FileHelper.getFileFromPacket(initialPacket);

			if (file.exists()) {
				FilePermission fp = new FilePermission(file.getAbsolutePath(), "read");
				try {
					AccessController.checkPermission(fp);
				} catch (AccessControlException e) {
					throw new SecurityException("Access Denied: File " + file.getAbsolutePath() + " does not have" +
                            " read access");
				}
                if (!file.canRead()) {
                    throw new SecurityException("Access Denied: File " + file.getAbsolutePath() + " does not have" +
                                                " read access");
                }

				try {
					BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file.getAbsolutePath()), "UTF-8"));
					
					if((file.length() - (blockNumber - 1) * 512) < 512 && (file.length() - (blockNumber - 1) * 512) > 0){
						int newSize = (int) file.length() - (blockNumber - 1) * 512;
						chars = new char[newSize];
					}
					br.skip((blockNumber - 1) * 512);
					int i = br.read(chars, 0, chars.length);
					reply.write(new String(chars).getBytes(), 0, chars.length);
					br.close();
      
	                if (i < 512) {
	                	flag = true;
	                }
	                
				} catch (IOException e) {
					throw new SecurityException("Access Denied: File " + file.getAbsolutePath() + " does not have" +
                            " read access");
				}
			} else {
				throw new FileNotFoundException("File does not exist on server");
			}
			
			//Construct a new packet
		    DatagramPacket responseData = new DatagramPacket(reply.toByteArray(), reply.toByteArray().length,
		                                                     data.getAddress(), data.getPort());
		    //print out the data on the sent packet
		    DataHelper.printPacketData(responseData, "Server (" + socket.getLocalPort() + "): Sending packet", ServerSettings.verbose, true);
		    
			//SEND the PACKET
		    try {
		        socket.send(responseData);
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		    
		    if (!flag) {
		    	buffer = new byte[512];
		    	DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
			    
		    	boolean cont = false;
		    	timeOutCount = 0;
		    	while(!cont){
		    		
		    		cont = true;
		    		try {
		    			socket.setSoTimeout(1000);
			    		socket.receive(receivePacket);
			    		data = receivePacket;
			    		
			    		if (data.getData()[0] == 0 && data.getData()[1] == 5) {
				    		throw new EPException("Error packet received from Client!", receivePacket);
				    	}
				    	if(data.getData()[0] != 0 || data.getData()[1] != 4){
					    	throw new IllegalOPException("Not vaild ACK OpCode");
					    }
				    	if(data.getPort() != this.port || !data.getAddress().equals(this.address)){
				    		throw new AddressException("Unknown TID/Address");
				    	}
				    	
			    		if(currDataBlock == -1){
			    			currDataBlock = data.getData()[2];
			    		} else if(currDataBlock+1 == data.getData()[2]){
			    			currDataBlock = data.getData()[2];
			    			
			    		} else {
			    			// if not expected packet ignore and keep waiting
			    			cont = false;
			    		}
			    		
			    		
			    	} catch(SocketTimeoutException e){
				    	e.printStackTrace();
				    	
				    	//SEND the PACKET
					    try {
					    	if(timeOutCount <= 5){
					    		timeOutCount++;
					    		System.out.println("Server timed out, resending (attempts left: " + timeOutCount + ")...");
					    		socket.send(responseData);
					    	}
					    	else{
					    		System.out.println("Timed out to many times, exiting thread...");
					    		break;
					    	}
					    } catch (IOException e1) {
					        e1.printStackTrace();
					    }
					    //try again
					    cont = false;
					    
					    
					    
				    } catch (IOException e) {
			    		e.printStackTrace();
			    	}
			    }
		    }
		} 
	}
	
	/**
	 * for write requests, send data packet for the client to wrote 2 512 bytes at a time
	 * @throws IllegalOPException 
	 */
	public void writeToFile() throws IOException {
        File file = FileHelper.getFileFromPacket(initialPacket);
        FileHelper.createFile(file);

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
		    DataHelper.printPacketData(responseData, "Server (" + socket.getLocalPort() + "): Sending packet", ServerSettings.verbose, true);
		    
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
	    	boolean cont = false;
	    	while(!cont){
	    		cont = true;	
	    		try {
	    			socket.setSoTimeout(4000);// TODO set back to 1000
		    		socket.receive(receivePacket);
		    		data = receivePacket;
		    		
		    		if (data.getData()[0] == 0 && data.getData()[1] == 5) {
				    	throw new EPException("Error packet received from Client!", receivePacket);
					} else if(data.getData()[0] != 0 ||data.getData()[1] != 3){
				    	throw new IllegalOPException("Not vaild DATA OpCode");
				    }
		    		
			    	if(!data.getAddress().equals(this.address)){
			    		throw new AddressException("unknown Transfer Id");
			    	}
			    	if(data.getPort() != this.port){
			    		throw new AddressException("unknown Port");
			    	}
			    	
		    		if(currACKBlock == -1){
		    			currACKBlock = data.getData()[2];
		    		} else if(currACKBlock+1 == data.getData()[2]){
		    			currACKBlock = data.getData()[2];
		    		} else{
		    			// ignore duplicated packet
		    			cont = false;
		    		}
		    		
		    		
		    		
		    	}catch(SocketTimeoutException e){
			    	if(timeOutCount <= 5){
			    		e.printStackTrace();
				    	//SEND the PACKET
				    	// TODO get this to send again???????????????
					    //try {
					        //socket.send(responseData);
					    //} catch (IOException e1) {
					      //  e1.printStackTrace();
					    //}
					    //try again
					    cont = false;
					    timeOutCount ++;
			    	}
			    	else{
			    		System.out.println("Timed out to many times, Exit Thread...");
			    		return;
			    	}
			    } catch (IOException e) {
		    		e.printStackTrace();
		    	}
	    	}
	    	
	    	
	    	byte datamin[] = DataHelper.minimi(receivePacket.getData(), receivePacket.getLength());
	    	//print out the data on the sent packet
	    	DataHelper.printPacketData(receivePacket, "Server (" + socket.getLocalPort() + "): Received Packet", ServerSettings.verbose, true);
		    
	    	block[0] = datamin[2];
	    	block[1] = datamin[3];
	    	
	    	byte[] b = Arrays.copyOfRange(datamin, 4, datamin.length);
	    	if (datamin.length < 512) {
	    		flag = true;
	    	}
	    	
	    	String contents = new String(b);
	    	
			FileWriter fw = new FileWriter(file, true);
			fw.write(contents);
				
			fw.close();
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
	    		DataHelper.printPacketData(e.getPacket(), "Server Thread (" + socket.getLocalPort() + "): Received Error Packet, Shutting down", true, true);
	    	} catch (IOException e) {
                sendERRPacket(EC3, address, e.getMessage(), port);
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
            	DataHelper.printPacketData(e.getPacket(), "Server Thread (" + socket.getLocalPort() + "): Received Error Packet Shutting down", true, true);
            } catch (IOException e) {
                sendERRPacket(EC3, address, e.getMessage(), port);
            }
	    }
	}
}
