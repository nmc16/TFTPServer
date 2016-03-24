package client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

import java.util.logging.Level;
import java.util.logging.Logger;

import exception.*;
import shared.*;

/**
 * Client program that connects through the error detector to connect to the server
 * and perform file operations.
 * 
 * @version 2
 * @author Team6
 */
public class Client {
    private static final int ERROR_SIM_PORT = 68;
    private static final int SERVER_PORT = 69;
    private int packetMode = 0;
    private final Logger LOG;
    private final SocketHelper socketHelper;
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
        } catch (IOException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            System.exit(1);
        }

        // Create the socket helper
        socketHelper = new SocketHelper(sendReceiveSocket);
    }

	/**
	 * Method that sends the initial request to the server and keeps the server and client 
	 * communicating until all of the data has been passed for the read/write operation.
	 *
	 * @param sendPacket Packet created for the initial request.
     * @throws java.io.IOException thrown if there is an error with the request sending or receiving
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
        boolean flag = false;
        while (running) {
        	int timeOutCount = 0;
        	byte data[] = new byte[516];
        	receivePacket = new DatagramPacket(data, data.length);

        	while(true){
                PacketResult result = socketHelper.receiveWithTimeout(timeOutCount, response, receiveAddress, receivePort, currBlock);
                if (result.isSuccess()) {
                    currBlock = DataHelper.getBlockNumber(result.getPacket());
                    receiveAddress = result.getPacket().getAddress();
                    receivePort = result.getPacket().getPort();
                    receivePacket = result.getPacket();
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

        	// Process the received datagram.
        	DataHelper.printPacketData(receivePacket, "Client: Packet received", verbose, false);
        	
        	// Check the OP Code
        	byte[] opCode = Arrays.copyOfRange(receivePacket.getData(), 0, 2);

            // If the code is an ACK then we need to send the next block of data
        	if (Arrays.equals(opCode, OpCodes.ACK_CODE)) {
                // If the last ack has been received
                if (flag) {
                    return;
                }

        		int blockNumber = DataHelper.getBlockNumber(receivePacket);
                byte byteBlockNumber[] = DataHelper.getNewBlock(blockNumber + 1);

        		// Get the data from the file
        		byte[] b = FileHelper.parseFile(blockNumber + 1, saveLocation, Charset.forName("UTF-8"));

                // If there is no more data left in the file break the loop
                if (b == null) {
                    break;
                }

                // If all of the data read was null data end the loop
                if (b.length < 512) {
                    flag = true;
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
                    running = false;
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
        System.out.println("    mode [test|normal] - Test mode sends packets to Error Sim, normal mode sends the packets to Server.");
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

        // Change the port for the request based on the user selected mode
        if (packetMode == 1) {
            return new DatagramPacket(buffer.toByteArray(), buffer.toByteArray().length, address, ERROR_SIM_PORT);
        } else {
            return new DatagramPacket(buffer.toByteArray(), buffer.toByteArray().length, address, SERVER_PORT);
        }
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
     * Sets the mode given the user input. If the mode is set to 1, it is in test mode and the packets will
     * be sent to the error sim. If the mode is 0 then the packets will be sent directly to the server.
     *
     * @param args Arguments passed from UI
     */
    private void runMode(String args[]) {
        if (args.length != 2) {
            LOG.warning("Instruction invalid length!");
            return;
        }

        if (args[1].toLowerCase().equals("test")) {
            packetMode = 1;
            LOG.info("Mode set to test!");
        } else if (args[1].toLowerCase().equals("normal")) {
            packetMode = 0;
            LOG.info("Mode set to normal!");
        } else {
            LOG.warning("Not valid mode!");
        }
    }

    /**
     * Runs the command given the split string around whitespace.
     * 
     * @param args Arguments passed from UI
     * @throws java.io.IOException thrown if there is an error with the command
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

        } else if (args[0].toLowerCase().equals("mode")) {
            runMode(args);

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
                    	socketHelper.sendErrorPacket(ErrorCodes.ILLEGAL_OP, address, receivePort, e);
                    } catch (AddressException e) {
                        socketHelper.sendErrorPacket(ErrorCodes.UNKNOWN_TID, address, receivePort, e);
                	} catch (FileNotFoundException e) {
                        socketHelper.sendErrorPacket(ErrorCodes.FILE_NOT_FOUND, address, receivePort, e);
        			} catch (ExistsException e){
                        socketHelper.sendErrorPacket(ErrorCodes.FILE_EXISTS, address, receivePort, e);
        			} catch (SecurityException e){
                        socketHelper.sendErrorPacket(ErrorCodes.ACCESS, address, receivePort, e);
        			} catch (DiskException e){
                        socketHelper.sendErrorPacket(ErrorCodes.DISK_ERROR, address, receivePort, e);
                    } catch (EPException e) {
        				DataHelper.printPacketData(e.getPacket(), "Client: Error Packet Received", true, false);
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
