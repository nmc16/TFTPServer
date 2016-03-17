package client;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

import java.net.SocketTimeoutException;
import exception.AddressException;
import exception.ExistsException;
import exception.IllegalOPException;
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
    private static final byte ERR_CODE[] = {0, 5};
    private static final byte ACK_CODE[] = {0, 4};
    private static final byte EC4[] = {0, 4};
    private static final byte EC5[] = {0, 5};
    private static final int HOST_PORT = 68;
    private static boolean verbose = false;
    private boolean running = true;
    private InetAddress address, receiveAddress;
    private int receivePort = -1;
    private String location, saveLocation;
    private Path folderPath;
    private int currBlock;
    private int timeOutCount = 0;

    
    public Client() {
        try {
        	// Randomize a port number and create the socket
            Random r = new Random();
            this.address = InetAddress.getLocalHost();
            sendReceiveSocket = new DatagramSocket(r.nextInt(65553));
            folderPath = Paths.get("client_files");
        } catch (SocketException se) { 
            se.printStackTrace();
            System.exit(1);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
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
        System.out.println("Error code " + errCode[1] + " has occurred. Closing the current request...");
        try {
	        sendReceiveSocket.send(ErrPack);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
    }

    /**
     * Parses the file by blocks using the block number given where each block holds 512 
     * bytes. Reads one block at a time.
     * 
     * @param blockNumber Block number to read from where 1 represents the first block in the file (byte 0)
     * @return returns the byte array (size 512) that holds the block parsed from the file
     */
	public byte[] parseFile(int blockNumber) throws IOException, FileNotFoundException, ExistsException{
        
        char[] data = new char[512];
        int newSize;
        
        // Only try the read if the location has been saved
		if (location != null) {
			//try {
				File file = new File(location);
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(location), "UTF-8"));
				
				if((file.length() - (blockNumber - 1) * 512) < 512 && (file.length() - (blockNumber - 1) * 512) > 0){
					newSize = (int) file.length() - (blockNumber - 1) * 512;
					data = new char[newSize];
				}
				br.skip((blockNumber - 1) * 512);
				br.read(data, 0, data.length);
				br.close();
                return new String(data).getBytes();
                
			//} //catch (FileNotFoundException e) {
				//e.printStackTrace();
			//} catch (IOException e) {
				//e.printStackTrace();
			//}
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
		
		
		//FileOutputStream fos = ...;
		//fos.write("hello".getBytes());
		//fos.getFD().sync();
		//fos.close();
		//The call to the sync() method will throw a SyncFailedException, when the disk is full
		
	}
    
	/**
	 * Method that sends the initial request to the server and keeps the server and client 
	 * communicating until all of the data has been passed for the read/write operation.
	 * 
	 * @param sendPacket Packet created for the initial request.
	 */
    public void sendAndReceive(DatagramPacket sendPacket) throws IllegalOPException, AddressException, IOException, FileNotFoundException, ExistsException {

    	// Check that the files directory exists to store the read files into
        if (Files.notExists(folderPath)) {
        	if (!folderPath.toFile().mkdir()) {
        		System.out.println("Cannot make directory!");
        		System.exit(1);
        	}
        }
        
        //Print out the info on the packet
    	Helper.printPacketData(sendPacket, "Client: Sending packet", verbose);
       
        // Send the datagram packet to the intermediate via the send/receive socket.
        try {
            sendReceiveSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // Construct a DatagramPacket for receiving packets up
        // to 516 bytes long (the length of the byte array).
        DatagramPacket response = sendPacket;
        while (true) {
        	timeOutCount = 0;
        	byte data[] = new byte[516];
        	receivePacket = new DatagramPacket(data, data.length);
        	
        	boolean cont = false;
        	while(!cont){
        		cont = true;
	        	try {
	        		// Block until a datagram is received via sendReceiveSocket.
	        		if(response.getPort() != HOST_PORT){
	        			sendReceiveSocket.setSoTimeout(1000);
	        		} else {
	        			sendReceiveSocket.setSoTimeout(0);
	        		}
	        		
	        		sendReceiveSocket.receive(receivePacket);
	        		
		    		if(currBlock == 0 && receivePacket.getData()[2] == 0){
		    			currBlock = -1;
	        		}
		    		 if(currBlock+1 == receivePacket.getData()[2]){
		    			//if the new blocknum == +1 the previous
		    			currBlock =receivePacket.getData()[2];
		    		} else{
		    			cont = false;
		    		}
	        		
	        	}catch(SocketTimeoutException e){
			    	e.printStackTrace();
			    	try {
			    		if(timeOutCount < 5){
			    			timeOutCount++;
			    			sendReceiveSocket.send(response);
			    		}
			    		else{
			    			System.out.println("Timed out to many times, exiting...");
			    			break;
			    		}
			        } catch (IOException e1) {
			            e1.printStackTrace();
			            System.exit(1);
			        }
			    	cont = false;
			    	
			    	
			    } catch(IOException e) {
	        		e.printStackTrace();
	        		System.exit(1);
	        	}
	        	
        	}
        	
        	
        	if (receiveAddress == null) {
        		receiveAddress = receivePacket.getAddress();
        	}
        	
        	if (receivePort == -1) {
        		receivePort = receivePacket.getPort();
        	}
        	
        	if(!receiveAddress.equals(receivePacket.getAddress()) || receivePort != receivePacket.getPort()){
        		Helper.printPacketData(receivePacket, "Client Ecountered Error Packet", true);
        		throw new AddressException("The address or TID was not correct during transfer: " + receivePacket.getAddress() + ", " + receivePacket.getPort());
        	}

        	// Process the received datagram.
        	Helper.printPacketData(receivePacket, "Client: Packet received", verbose);
        	
        	// Check the OP Code
        	byte[] opCode = Arrays.copyOfRange(receivePacket.getData(), 0, 2);
            byte[] byteBlockNumber = Arrays.copyOfRange(receivePacket.getData(), 2, 4);

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
                byte[] transferred = Arrays.copyOfRange(receivePacket.getData(), 4, receivePacket.getLength());

                byte[] minimized = Helper.minimi(transferred, transferred.length);
                writeFile(new String(minimized), new File(folderPath.toString() + "\\" + saveLocation));
                                
                // Check if there is more data to be read or not
                if (minimized.length < 512) {
                    // No more data to be read
                    break;
                }

                // Otherwise send an acknowledge to the server
                response = createPacket(ACK_CODE, byteBlockNumber, receivePacket.getPort());    
            } else if (Arrays.equals(opCode, ERR_CODE)) {
            	// Quit the program and display message
            	Helper.printPacketData(receivePacket, "Client: Error Packet Receieved", true);
            	//running = false;
            	//return;
            	break;
            } else {
            	Helper.printPacketData(receivePacket, "Client Ecountered Error Packet", true);
            	throw new IllegalOPException("Illegal opCode");
            }

            // TODO this needs to be refactored
            if (response != null) {
        	    Helper.printPacketData(response, "Client: Sending Packet", verbose);
            }

            // Send the response to the server
            try {
                sendReceiveSocket.send(response);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        receiveAddress = null;
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
    private void runCommand(String args[]) throws IllegalOPException, AddressException, FileNotFoundException, IOException, ExistsException {
    	currBlock = 0;
    	
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
    		File file = new File(folderPath.toString() + "\\" + saveLocation);
            try {
            	// Check if there are slashes, which indicates directories
                Helper.createSubDirectories(folderPath.toString() + "\\" + saveLocation);
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
        } else if (args[0].toLowerCase().equals("verbose")) {
        	if (args.length != 2) {
        		System.out.println("Instruction invalid length!");
                return;
        	}
        	verbose = true;
        	System.out.println("Verbose mode set!");
        }
        else {
            System.out.println("Invalid command entered!");
        }
    }

    /**
     * Runs the UI and runs the commands entered
     * @throws IllegalOPException 
     */
    public void run() {
        Scanner reader = new Scanner(System.in);
        System.out.println("Starting client...");
        printMenu();
        
        while(running) {
        	System.out.print("\nENTER COMMAND > ");
        	
        	receiveAddress = null;
        	receivePort = -1;
        	
            // Read the input from the user
            String input = reader.nextLine();

            // Parse the input and check for keywords
            String args[] = input.split("\\s+");

            if (args.length > 0) {
                if (args[0].toLowerCase().equals("quit")) {
                    break;
                } else {
                    try{
                    	runCommand(args);
                    	
                    } catch(IllegalOPException e){
                    	sendERRPacket(EC4, address, e.getMessage(), receivePort);
                    	
                    } catch (AddressException e) {
                		sendERRPacket(EC5, address, e.getMessage(), receivePort); 
                	} catch (FileNotFoundException e) {
                		//TODO must send it back to start of client
        				//System.out.println("TESTING IF GOT TO THE TOP");
                		e.printStackTrace();
        			} catch (IOException e) {
        				e.printStackTrace();
        			} catch (ExistsException e){
        				e.printStackTrace();
        			}
                }
            } else {
                System.out.println("Instruction invalid length!");
            }
        }
        
        System.out.println("\nClient shutting down...");
        
        // We're finished, so close the socket.
        sendReceiveSocket.close();
        reader.close();
    }

    public static void main(String args[]) {
        Client c = new Client();
        c.run();
    }
}
