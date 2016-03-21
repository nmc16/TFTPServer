package client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import exception.*;
import shared.DataHelper;
import shared.ErrorCodes;
import shared.FileHelper;
import shared.OpCodes;

/**
 * Client program that connects through the error detector to connect to the server
 * and perform file operations.
 * 
 * @version 2
 * @author Team6
 */
public class Client {
    private static final int ERROR_SIM_PORT = 68;
    private final Logger LOG;
    private DatagramPacket receivePacket;
    private DatagramSocket sendReceiveSocket;
    private String saveLocation;
    private InetAddress address, receiveAddress;
    private boolean verbose = false;
    private int receivePort = -1;
    private int currBlock;
    
    public Client() {
        // Config the logger if it hasn't been already and
        DataHelper.configLogger();
        LOG = Logger.getLogger("global");

        try {
        	// Randomize a port number and create the socket
            Random r = new Random();
            this.address = InetAddress.getLocalHost();
            sendReceiveSocket = new DatagramSocket(r.nextInt(65553));
        } catch (SocketException se) { 
            LOG.log(Level.SEVERE, se.getMessage(), se);
            System.exit(1);
        } catch (UnknownHostException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Waits to receive a packet with a timeout that if it is received it resends the DatagramPacket passed
     * to the method. Method does not use a timeout for the intial request packet sent.
     *
     * @param timeOutCount counter to show attempts remaining
     * @param response DatagramPacket to resend if the receives times out
     * @return true if the packet was received correctly, false if it needs to be re-run
     * @throws IOException thrown if there is an error with the packet or an error with the receive socket
     */
    private boolean receiveWithTimeout(int timeOutCount, DatagramPacket response) throws IOException {
        // Block until a datagram is received via sendReceiveSocket.
        if(response != null && response.getPort() != ERROR_SIM_PORT){
            sendReceiveSocket.setSoTimeout(1000);
        } else {
            sendReceiveSocket.setSoTimeout(0);
        }

        try {
            // Receive the packet
            sendReceiveSocket.receive(receivePacket);

            // If the packet is an error, throw an exception
            if (DataHelper.isErrorPacket(receivePacket)) {
                throw new EPException("Error packet received from Client!", receivePacket);
            }

            // Update the address and port if not already updated
            if (receiveAddress == null || receivePort == -1) {
                receiveAddress = receivePacket.getAddress();
                receivePort = receivePacket.getPort();
            }

            // Check that the address and port received are the ones we were expecting
            if(!receiveAddress.equals(receivePacket.getAddress()) || receivePort != receivePacket.getPort()){
                throw new AddressException("The address or TID was not correct during transfer: " +
                                           receivePacket.getAddress() + ", " + receivePacket.getPort());
            }

            // Check the packet is not duplicated
            if (currBlock + 1 == DataHelper.getBlockNumber(receivePacket) ||
                currBlock == 0 && DataHelper.getBlockNumber(receivePacket) == 0) {

                currBlock = DataHelper.getBlockNumber(receivePacket);
                return true;
            }

            // If we get here it must be a duplicated packet, ignore it
            LOG.warning("Received duplicate packet, ignoring...");
            return false;

        } catch(SocketTimeoutException e) {
            // Timed out, resend packet and do not continue
            LOG.warning("Received timed out. Re-sending packet (attempts remaining: " + (5 - timeOutCount) + ")...");
            sendReceiveSocket.send(response);
            return false;
        }
    }
    
	/**
	 * Method that sends the initial request to the server and keeps the server and client 
	 * communicating until all of the data has been passed for the read/write operation.
	 *
	 * @param sendPacket Packet created for the initial request.
	 */
    public void sendAndReceive(DatagramPacket sendPacket) throws IOException {
        //Print out the info on the packet
    	DataHelper.printPacketData(sendPacket, "Client: Sending packet", verbose, false);
       
        // Send the datagram packet to the intermediate via the send/receive socket.
        sendReceiveSocket.send(sendPacket);

        // Construct a DatagramPacket for receiving packets up
        // to 516 bytes long (the length of the byte array).
        DatagramPacket response = sendPacket;

        boolean running = true;
        while (running) {
        	int timeOutCount = 0;
        	byte data[] = new byte[516];
        	receivePacket = new DatagramPacket(data, data.length);

        	boolean cont = false;
        	while(!cont){
        		cont = receiveWithTimeout(timeOutCount, response);
                timeOutCount++;
        	}

        	// Process the received datagram.
        	DataHelper.printPacketData(receivePacket, "Client: Packet received", verbose, false);
        	
        	// Check the OP Code
        	byte[] opCode = Arrays.copyOfRange(receivePacket.getData(), 0, 2);

            // If the code is an ACK then we need to send the next block of data
        	if (Arrays.equals(opCode, OpCodes.ACK_CODE)) {
        		int blockNumber = DataHelper.getBlockNumber(receivePacket);
                byte byteBlockNumber[] = DataHelper.getNewBlock(blockNumber + 1);

        		// Get the data from the file
        		byte[] b = FileHelper.parseFile(blockNumber, saveLocation, Charset.forName("UTF-8"));

                // If there is no more data left in the file break the loop
                if (b == null) {
                    break;
                }

                // If all of the data read was null data end the loop
                if (b[0] == 0) {
                    running = false;
                }
                
                ByteArrayOutputStream reply = new ByteArrayOutputStream();
                reply.write(byteBlockNumber, 0, byteBlockNumber.length);
                reply.write(b, 0, b.length);
                
                // Otherwise send the new packet to the server
                response = createPacket(OpCodes.DATA_CODE, reply.toByteArray(), receivePacket.getPort());

        	} else if (Arrays.equals(opCode, OpCodes.DATA_CODE)) {
                // Get the data
                byte[] transferred = Arrays.copyOfRange(receivePacket.getData(), 4, receivePacket.getLength());
                byte[] minimized = DataHelper.minimi(transferred, transferred.length);

                // Write the data to the file location
                FileHelper.writeFile(new String(minimized), new File(saveLocation));
                                
                // Check if there is more data to be read or not
                if (minimized.length < 512) {
                    // No more data to be read
                    break;
                }

                // Otherwise send an acknowledge to the server
                byte byteBlockNumber[] = Arrays.copyOfRange(receivePacket.getData(), 2, 4);
                response = createPacket(OpCodes.ACK_CODE, byteBlockNumber, receivePacket.getPort());

            } else if (Arrays.equals(opCode, OpCodes.ERR_CODE)) {
            	// Quit the request and display message
            	DataHelper.printPacketData(receivePacket, "Client: Error Packet Received", true, false);
            	break;
            } else {
                // There must have been an error in the packet OP code
            	throw new IllegalOPException("Illegal opCode received: " + Arrays.toString(opCode));
            }

        	DataHelper.printPacketData(response, "Client: Sending Packet", verbose, false);

            // Send the response to the server
            sendReceiveSocket.send(response);
        }
   }

    /**
     * Prints the UI menu options to stdout
     */
    public void printMenu() {
        System.out.println(" Options:");
        System.out.println("    read [filename] [file location] [mode] - Reads the file from the server under filename and saves");
        System.out.println("                                             the file under the path specified under the root where the");
        System.out.println("                                             project is being run under directory \"client_files\".");
        System.out.println("    write [filename] [file location] [mode] - Writes file at location to filename on server.");
        System.out.println("    verbose [true|false] - Changes server display mode to verbose or silent mode.");
        System.out.println("    help - Prints options screen.");
        System.out.println("    quit - Quits the client program.");
    }

    /**
     * Creates a datagram packet that holds the request using the OP code and file name on the
     * server to perform the IO operations on, as well as the mode to perform them in.
     * 
     * @param opCode 2 byte operation code 
     * @param fileName File name on the server to perform IO on
     * @param mode mode for IO
     * @param saveLocation file save location
     * @return Datagram packet created for request
     */
    public DatagramPacket createPacket(byte[] opCode, String fileName, String mode, String saveLocation) {
        // Check that the op code is valid before creating
        if (opCode.length != 2) {
            throw new IllegalArgumentException("Op code must be length 2! Found length " + opCode.length + ".");
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        // Write the op code into the buffer
        buffer.write(opCode[0]);
        buffer.write(opCode[1]);

        // Write the file name and mode separated by 0s
        buffer.write(fileName.getBytes(), 0, fileName.length());
        buffer.write(0);
        buffer.write(mode.getBytes(), 0, mode.length());
        buffer.write(0);

        // Save the location for writing later
        this.saveLocation = saveLocation;

        return new DatagramPacket(buffer.toByteArray(), buffer.toByteArray().length, address, ERROR_SIM_PORT);
    }

    /**
     * Creates datagram packet using port and data passed.
     * 
     * @param opCode 2 byte operation code
     * @param data byte array of data to send to server
     * @param port port to send data to
     * @return Datagram packet for the request
     */
    public DatagramPacket createPacket(byte[] opCode, byte[] data, int port) {
        // Check that the op code is valid before creating
        if (opCode.length != 2) {
            throw new IllegalArgumentException("Op code must be length 2! Found length " + opCode.length + ".");
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        buffer.write(opCode[0]);
        buffer.write(opCode[1]);

        buffer.write(data, 0, data.length);

        return new DatagramPacket(buffer.toByteArray(), buffer.toByteArray().length, address, port);
    }

    /**
     * Method to run the read request between the client and server.
     *
     * @param args Arguments passed from UI
     * @throws IOException thrown if there is a problem with the transfer
     */
    private void runRead(String args[]) throws IOException {
        // Check the command has the correct amount of arguments
        if (args.length != 4) {
            LOG.warning("Instruction invalid length!");
            return;
        }

        // Create the request packet with the read code
        DatagramPacket packet = createPacket(OpCodes.READ_CODE, args[1], args[3], args[2]);
        File file = new File(saveLocation);

        try {
            // Create the subdirectories if they don't already exist
            FileHelper.createSubDirectories(saveLocation);

            // Create the file
            FileHelper.createFile(file);
        } catch (ExistsException e) {
            // The file already exists, warn the user and exit the command
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return;
        } catch (SecurityException e) {
            // The folders could not be created
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return;
        }

        sendAndReceive(packet);
    }

    /**
     * Method that runs the read request to the server.
     *
     * @param args Arguments passed from UI
     * @throws IOException thrown if there is a an error with the request
     */
    private void runWrite(String args[]) throws IOException {
        // Check the argument is valid length
        if (args.length != 4) {
            LOG.warning("Instruction invalid length!");
            return;
        }

        // Create the request packet and send it
        DatagramPacket packet = createPacket(OpCodes.WRITE_CODE, args[1], args[3], args[2]);
        sendAndReceive(packet);
    }

    /**
     * Sets the verbose output mode on the client given the input arguments.
     *
     * @param args Arguments passed from UI
     */
    private void runVerbose(String args[]) {
        if (args.length != 2) {
            LOG.warning("Instruction invalid length!");
            return;
        }

        verbose = Boolean.valueOf(args[1]);
        LOG.info("Verbose mode set to " + verbose);
    }

    /**
     * Runs the command given the split string around whitespace.
     * 
     * @param args Arguments passed from UI
     */
    private void runCommand(String args[]) throws IOException {
        // Reset the program counters
    	currBlock = 0;
        receiveAddress = null;
        receivePort = -1;

        if (args[0].toLowerCase().equals("help")) {
            printMenu();
            return;
        }

        if (args[0].toLowerCase().equals("read")) {
            // Run the read command
            runRead(args);
            
        } else if (args[0].toLowerCase().equals("write")) {
            // Run the write command
            runWrite(args);

        } else if (args[0].toLowerCase().equals("verbose")) {
            runVerbose(args);

        } else {
            LOG.warning("Invalid command entered!");
        }
    }

    /**
     * Runs the UI and the commands entered until the user enters the "quit" command
     * which closes the program.
     */
    public void run() {
        Scanner reader = new Scanner(System.in);
        LOG.info("Starting client...");
        printMenu();
        
        while(true) {
        	System.out.print("\nENTER COMMAND > ");

            // Read the input from the user
            String input = reader.nextLine();

            // Parse the input and check for keywords
            String args[] = input.split("\\s+");

            // Check the user passed in arguments
            if (args.length > 0) {
                // Quit the program if quit is entered
                if (args[0].toLowerCase().equals("quit")) {
                    break;
                } else {
                    try{
                        // Run the command
                    	runCommand(args);
                    	
                    } catch(IllegalOPException e){
                        LOG.log(Level.SEVERE, e.getMessage(), e);
                    	DataHelper.sendErrorPacket(ErrorCodes.ILLEGAL_OP, sendReceiveSocket, address, receivePort, e.getMessage());
                    } catch (AddressException e) {
                        LOG.log(Level.SEVERE, e.getMessage(), e);
                        DataHelper.sendErrorPacket(ErrorCodes.UNKNOWN_TID, sendReceiveSocket, address, receivePort, e.getMessage());
                	} catch (FileNotFoundException e) {
                        LOG.log(Level.SEVERE, e.getMessage(), e);
                        DataHelper.sendErrorPacket(ErrorCodes.FILE_NOT_FOUND, sendReceiveSocket, address, receivePort, e.getMessage());
        			} catch (ExistsException e){
                        LOG.log(Level.SEVERE, e.getMessage(), e);
                        DataHelper.sendErrorPacket(ErrorCodes.FILE_EXISTS, sendReceiveSocket, address, receivePort, e.getMessage());
        			} catch (SecurityException e){
                        LOG.log(Level.SEVERE, e.getMessage(), e);
                        DataHelper.sendErrorPacket(ErrorCodes.ACCESS, sendReceiveSocket, address, receivePort, e.getMessage());
        			} catch (DiskException e){
                        LOG.log(Level.SEVERE, e.getMessage(), e);
                        DataHelper.sendErrorPacket(ErrorCodes.DISK_ERROR, sendReceiveSocket, address, receivePort, e.getMessage());
                    } catch (EPException e) {
        				DataHelper.printPacketData(receivePacket, "Client: Error Packet Received", true, false);
        			} catch (IOException e) {
                        LOG.log(Level.SEVERE, e.getMessage(), e);
                    }
                }
            } else {
                LOG.warning("Instruction invalid length!");
            }
        }
        
        LOG.info("Client shutting down...");
        
        // We're finished, so close the socket.
        sendReceiveSocket.close();
        reader.close();
    }

    public static void main(String args[]) {
        Client c = new Client();
        c.run();
    }
}
