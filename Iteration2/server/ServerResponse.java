package server;

import exception.AddressException;
import exception.EPException;
import exception.ExistsException;
import exception.IllegalOPException;
import shared.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Random;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private final SocketHelper socketHelper;
    private final Logger LOG;
	private DatagramPacket initialPacket;
	private DatagramPacket data;
	private DatagramSocket socket;

	private InetAddress address;
	private int port;
	private int currBlock = -1;
	private int timeOutCount = 0;

	//TODO re add in timeout set
	public ServerResponse(DatagramPacket data) {
        // Configure logger and get it
        DataHelper.configLogger();
        LOG = Logger.getLogger("global");

        // Store the intial packet
		this.initialPacket = data;
		this.data = data;
		
	    try {
            // Randomize port number to use for socket
	    	Random r = new Random();
            socket = new DatagramSocket(r.nextInt(65500));
	    } catch (IOException e) {
            // If there is an error in creation, exit the thread
	        LOG.log(Level.SEVERE, e.getMessage(), e);
            System.exit(1);
	    }

        // Store the address and port
        this.address = data.getAddress();
        this.port = data.getPort();

        // Create the helper object
        socketHelper = new SocketHelper(socket);
	}


	/**
	 * Reads file 512 bytes at a time from the file of the clients requests choice
	 * 
	 * @throws IllegalOPException 
	 */
	public void readFile() throws IOException {
		currBlock = 0;
        timeOutCount = 0;

		while(true) {
            currBlock++;
			ByteArrayOutputStream reply = new ByteArrayOutputStream();
		
			byte blockNumber[] = DataHelper.getNewBlock(currBlock);

			reply.write(0);
			reply.write(DATA);
			reply.write(blockNumber, 0, blockNumber.length);


			File file = FileHelper.getFileFromPacket(initialPacket);
            byte[] buffer = FileHelper.parseFile(currBlock, file.getAbsolutePath(), Charset.forName("UTF-8"));
            reply.write(buffer, 0, buffer.length);

			//Construct a new packet
		    DatagramPacket responseData = new DatagramPacket(reply.toByteArray(), reply.toByteArray().length,
		                                                     data.getAddress(), data.getPort());
		    //print out the data on the sent packet
		    DataHelper.printPacketData(responseData, "Server (" + socket.getLocalPort() + "): Sending packet", ServerSettings.verbose, true);
		    
			//SEND the PACKET
		    socket.send(responseData);

            if (buffer.length < 512) {
                break;
            }

		    timeOutCount = 0;
		    while(true){
                PacketResult result = socketHelper.receiveWithTimeout(timeOutCount, responseData, address, port, currBlock - 1);
                if (result.isSuccess()) {
                    currBlock = DataHelper.getBlockNumber(result.getPacket());
                    address = result.getPacket().getAddress();
                    port = result.getPacket().getPort();
                    data = result.getPacket();
                    break;
                }

                if (result.isTimeOut()) {
                    timeOutCount++;
                }

                if (timeOutCount >= 5) {
                    LOG.severe("Timed out too many times, cancelling request...");
                    return;
                }
			}

		}
	}
	
	/**
	 * for write requests, send data packet for the client to wrote 2 512 bytes at a time
	 * @throws IllegalOPException 
	 */
	public void writeToFile() throws IOException {
        currBlock = 0;
        timeOutCount = 0;

        File file = FileHelper.getFileFromPacket(initialPacket);
        FileHelper.createFile(file);

		boolean flag = false;
		
		while(true) {
			ByteArrayOutputStream reply = new ByteArrayOutputStream();
			reply.write(0);
			reply.write(ACK);

            byte blockNumber[] = DataHelper.getNewBlock(currBlock);
			reply.write(blockNumber, 0, blockNumber.length);
			
			//Construct a new packet
		    DatagramPacket responseData = new DatagramPacket(reply.toByteArray(), reply.toByteArray().length,
		                                                     data.getAddress(), data.getPort());
		    //print out the data on the sent packet
		    DataHelper.printPacketData(responseData, "Server (" + socket.getLocalPort() + "): Sending packet", ServerSettings.verbose, true);

		    socket.send(responseData);
		    
		    if (flag) {
		    	break;
		    }

	    	while(true){
                PacketResult result = socketHelper.receiveWithTimeout(timeOutCount, responseData, address, port, currBlock);
                if (result.isSuccess()) {
                    currBlock = DataHelper.getBlockNumber(result.getPacket());
                    address = result.getPacket().getAddress();
                    port = result.getPacket().getPort();
                    data = result.getPacket();
                    break;
                }

                if (result.isTimeOut()) {
                    timeOutCount++;
                }

                if (timeOutCount >= 5) {
                    LOG.severe("Timed out too many times, cancelling request...");
                    return;
                }
            }

	    	byte datamin[] = DataHelper.minimi(data.getData(), data.getLength());
	    	//print out the data on the sent packet
	    	DataHelper.printPacketData(data, "Server (" + socket.getLocalPort() + "): Received Packet", ServerSettings.verbose, true);
	    	
	    	byte[] b = Arrays.copyOfRange(datamin, 4, datamin.length);
	    	if (datamin.length < 512) {
	    		flag = true;
	    	}
	    	
	    	String contents = new String(b);
			FileHelper.writeFile(contents, file);
		}
	}

    /**
     * Creates datagram error packet using information passed.
     *
     * @param errCode 2 byte Error Code
     * @param address to send to the Error Packet to
     * @param port port to send Packet to
     * @param cause exception that caused error packet
     */
    public void sendERRPacket(byte[] errCode, InetAddress address, int port, Exception cause) {
        socketHelper.sendErrorPacket(errCode, address, port, cause);
    }

	@Override
	public void run() {
	    
	    if (initialPacket.getData()[1] == RRQ) {
	    	try {
	    		readFile();
	    	} catch (FileNotFoundException e) {
	    		socketHelper.sendErrorPacket(ErrorCodes.FILE_NOT_FOUND, address, port, e);
	    	} catch (SecurityException e) {
                socketHelper.sendErrorPacket(ErrorCodes.ACCESS, address, port, e);
	    	} catch (IllegalOPException e) {
                socketHelper.sendErrorPacket(ErrorCodes.ILLEGAL_OP, address, port, e);
	    	} catch (AddressException e) {
                socketHelper.sendErrorPacket(ErrorCodes.UNKNOWN_TID, address, port, e);
	    	} catch (EPException e) {
	    		DataHelper.printPacketData(e.getPacket(), "Server Thread (" + socket.getLocalPort() + "): Received Error Packet, Shutting down", true, true);
	    	} catch (IOException e) {
                socketHelper.sendErrorPacket(ErrorCodes.DISK_ERROR, address, port, e);
            }
	    } else {
	    	try {
	    		writeToFile();
	    	} catch (SecurityException e) {
                socketHelper.sendErrorPacket(ErrorCodes.ACCESS, address, port, e);
	    	} catch (IllegalOPException e) {
                socketHelper.sendErrorPacket(ErrorCodes.ILLEGAL_OP, address, port, e);
	    	} catch (AddressException e) {
                socketHelper.sendErrorPacket(ErrorCodes.UNKNOWN_TID, address, port, e);
	    	} catch (ExistsException e) {
                socketHelper.sendErrorPacket(ErrorCodes.FILE_EXISTS, address, port, e);
            } catch (EPException e) {
            	DataHelper.printPacketData(e.getPacket(), "Server Thread (" + socket.getLocalPort() + "): Received Error Packet Shutting down", true, true);
            } catch (IOException e) {
                socketHelper.sendErrorPacket(ErrorCodes.DISK_ERROR, address, port, e);
            }
	    }
	}
}
