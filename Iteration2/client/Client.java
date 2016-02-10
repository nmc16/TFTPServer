package client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

import exception.ExistsException;
import shared.Helper;

/**
 * Client program that connects through the error detector to connect to the server
 * and perform file operations.
 * 
 * @version 2
 * @author Team6
 */
public class Client {
    private DatagramPacket receivePacket;
    private DatagramSocket sendReceiveSocket;
    private static final byte READ_CODE[] = {0, 1};
    private static final byte WRITE_CODE[] = {0, 2};
    private static final byte DATA_CODE[] = {0, 3};
    private static final byte ACK_CODE[] = {0, 4};
    private static final int HOST_PORT = 68;
    private InetAddress address;
    private String location, mode, saveLocation;


    public Client() {
        try {
        	// Randomize a port number and create the socket
            Random r = new Random();
            address = InetAddress.getLocalHost();
            sendReceiveSocket = new DatagramSocket(r.nextInt(65553));
        } catch (SocketException se) { 
            se.printStackTrace();
            System.exit(1);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Parses the file by blocks using the block number given where each block holds 512 
     * bytes. Reads one block at a time.
     * 
     * @param blockNumber Block number to read from where 1 represents the first block in the file (byte 0)
     * @return returns the byte array (size 512) that holds the block parsed from the file
     */
	public byte[] parseFile(int blockNumber) {
        byte[] data = new byte[512];
        int newsize;
        long newsize2;
        
        // Only try the read if the location has been saved
		if (location != null) {
			try {
				// Create an access file in read write mode
				RandomAccessFile file = new RandomAccessFile(location, "rw");
				
				// Skip to the beginning of the block we want to read from
				file.seek((blockNumber - 1) * 512);
				
				// Read the full block and close the reader
				if((file.length() - file.getFilePointer()) < 512 && (file.length() - file.getFilePointer()) > 0){
					newsize2 = file.length() - file.getFilePointer();
					newsize = (int) newsize2;
					data = new byte[newsize];
				}
				
				// Read the data into the byte array and return it
				file.read(data, 0, data.length);
				file.close();
                return data;
                
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return null;
    }
	
	/**
	 * File write method for the client side read that will write to the
	 * string contents passed to the file in append mode so that no data is overwritten.
	 * 
	 * @param data Data to write to file
	 * @param file File to append data into
	 */
	public void writeFile(String data, File file) {
		try {
			// Open a file writer in append mode
			FileWriter fw = new FileWriter(file, true);
			
			// Write the data and close the writer
			fw.write(data);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
	}
    
	/**
	 * Method that sends the initial request to the server and keeps the server and client 
	 * communicating until all of the data has been passed for the read/write operation.
	 * 
	 * @param sendPacket Packet created for the initial request.
	 */
    public void sendAndReceive(DatagramPacket sendPacket) {

        //Print out the info on the packet
        System.out.println("client.Client: Sending packet:");
        System.out.println("To host: " + sendPacket.getAddress());
        System.out.println("Destination host port: " + sendPacket.getPort());
        System.out.println("Length: " + sendPacket.getLength());
        System.out.println("Containing: " + new String(sendPacket.getData()));
        System.out.println("Byte form: " + Arrays.toString(sendPacket.getData()) + "\n\n");

        // Send the datagram packet to the intermediate via the send/receive socket.

        try {
            sendReceiveSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("client.Client: Packet sent.\n");

        // Construct a DatagramPacket for receiving packets up
        // to 516 bytes long (the length of the byte array).

        while (true) {
        	byte data[] = new byte[516];
        	receivePacket = new DatagramPacket(data, data.length);

        	try {
        		// Block until a datagram is received via sendReceiveSocket.
        		sendReceiveSocket.receive(receivePacket);
        	} catch(IOException e) {
        		e.printStackTrace();
        		System.exit(1);
        	}

        	// Process the received datagram.
        	byte datamin[] = Helper.minimi(receivePacket.getData(), receivePacket.getLength());
        	System.out.println("client.Client: Packet received:");
        	System.out.println("From host: " + receivePacket.getAddress());
        	System.out.println("Host port: " + receivePacket.getPort());
        	System.out.println("Length: " + receivePacket.getLength());
        	System.out.println("Containing: " + new String(receivePacket.getData()));
            System.out.println("In byte form: " + Arrays.toString(datamin) + "\n\n");
        	
        	// Check the OP Code
        	byte[] opCode = Arrays.copyOfRange(receivePacket.getData(), 0, 2);
            byte[] byteBlockNumber = Arrays.copyOfRange(receivePacket.getData(), 2, 4);
            DatagramPacket response = null;

            // If the code is an ACK then we need to send the next block of data
        	if (Arrays.equals(opCode, ACK_CODE)) {

                // Increment block number to next block
        		byteBlockNumber[0]++;
    			if (byteBlockNumber[0] == 0) {
    				byteBlockNumber[1]++;
    			}
    			
        		int blockNumber = (byteBlockNumber[1] & 0xFF) << 8 | (byteBlockNumber[0] & 0xFF);

        		// Get the data from the file
        		byte[] b = parseFile(blockNumber);

                // If there is no more data left in the file break the loop
                if (b == null || b[0] == 0) {
                    break;
                }
                
                ByteArrayOutputStream reply = new ByteArrayOutputStream();
                reply.write(byteBlockNumber, 0, byteBlockNumber.length);
                reply.write(b, 0, b.length);
                
                // Otherwise send the new packet to the server
                response = createPacket(DATA_CODE, reply.toByteArray(), receivePacket.getPort());


        	} else if (Arrays.equals(opCode, DATA_CODE)) {
                // Get the data
                byte[] transferred = Arrays.copyOfRange(receivePacket.getData(), 4, 516);

                byte[] minimized = Helper.minimi(transferred, transferred.length);
                
                writeFile(new String(minimized), new File(saveLocation));
                
                // Check if there is more data to be read or not
                if (transferred[transferred.length - 1] == 0) {
                    // No more data to be read
                    break;
                }

                // Otherwise send an acknowledge to the server
                response = createPacket(ACK_CODE, byteBlockNumber, receivePacket.getPort());    
            }

            // TODO this needs to be refactored
            if (response != null) {
        	    System.out.println("Client: Sending packet:");
                System.out.println("To host: " + response.getAddress());
                System.out.println("Destination host port: " + response.getPort());
                System.out.println("Length: " + response.getLength());
                System.out.println("Containing: " + new String(response.getData()));
                System.out.println("Byte form: " + Arrays.toString(response.getData()) + "\n\n");
            }

            // Send the response to the server
            try {
                sendReceiveSocket.send(response);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
   }

    /**
     * Prints the UI menu options to stdout
     */
    public void printMenu() {
        System.out.println(" Options:");
        System.out.println("    read [filename] [file location] [mode] - Reads the file from the server under filename");
        System.out.println("    write [filename] [file location] [mode] - Writes file at location to");
        System.out.println("                                               filename on server.");
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
     * @return Datagram packet created for request
     */
    public DatagramPacket createPacket(byte[] opCode, String fileName, String mode) {
        return createPacket(opCode, fileName, mode, null);
    }

    public DatagramPacket createPacket(byte[] opCode, String fileName, String mode, String location) {
        // Check that the op code is valid before creating
        if (opCode.length != 2) {
            throw new IllegalArgumentException("Op code must be length 2! Found length " + opCode.length + ".");
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        // Write the op code into the buffer
        buffer.write(opCode[0]);
        buffer.write(opCode[1]);

        // Write the file name and mode seperated by 0s
        buffer.write(fileName.getBytes(), 0, fileName.length());
        buffer.write(0);
        buffer.write(mode.getBytes(), 0, mode.length());
        buffer.write(0);

        this.location = location;
        this.mode = mode;
        
        return new DatagramPacket(buffer.toByteArray(), buffer.toByteArray().length, address, HOST_PORT);
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
     * Runs the command given the split string around whitespace.
     * 
     * @param args Arguments passed from UI
     */
    private void runCommand(String args[]) {

        if (args[0].toLowerCase().equals("help")) {
            printMenu();
            return;
        }

        if (args[0].toLowerCase().equals("read")) {
            if (args.length != 4) {
                System.out.println("Instruction invalid length!");
                return;
            }
            DatagramPacket packet = createPacket(READ_CODE, args[1], args[3]);
            saveLocation = args[2];
    		File file = new File(saveLocation);
            try {
    			Helper.createFile(file);
    		} catch (ExistsException e) {
    			e.printStackTrace();
    		}
            sendAndReceive(packet);
        } else if (args[0].toLowerCase().equals("write")) {
            if (args.length != 4) {
                System.out.println("Instruction invalid length!");
                return;
            }
            DatagramPacket packet = createPacket(WRITE_CODE, args[1], args[3], args[2]);
            sendAndReceive(packet);
        } else {
            System.out.println("Invalid command entered!");
        }
    }

    /**
     * Runs the UI and runs the commands entered
     */
    public void run() {
        Scanner reader = new Scanner(System.in);
        System.out.println("Starting client...");
        printMenu();

        while(true) {
            // Read the input from the user
            System.out.print("> ");
            String input = reader.nextLine();

            // Parse the input and check for keywords
            String args[] = input.split("\\s+");

            if (args.length > 0) {
                if (args[0].toLowerCase().equals("quit")) {
                    System.out.println("client.Client shutting down...");
                    break;
                } else {
                    runCommand(args);
                }
            } else {
                System.out.println("Instruction invalid length!");
            }
        }
        // We're finished, so close the socket.
        sendReceiveSocket.close();
        reader.close();
    }

    public static void main(String args[]) {
        Client c = new Client();
        c.run();
    }
}
