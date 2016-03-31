package error;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import shared.DataHelper;
import shared.OpCodes;

/**
 * The Thread for the Error simulator, takes init input from the Error simulator then send the request to the server 
 * (continued communication with server thread).
 * 
 * @version 2
 * @author Team6
 */
public class ErrorSimThread implements Runnable {
    private static final String ACK = "ack";
    private static final String DATA = "data";
    private static final int delay = 2000;
    private final Scanner reader;
    private final Logger LOG;
    private DatagramSocket sendReceiveSocket;
	private DatagramSocket errorSendSocket;
	private DatagramPacket initPacket;
	private int clientPort;
	private int serverPort = -1;
	private int blockNum = 0;
    private String packetType = "";
    private String argument = "";
	private String mode = "00";
	private boolean errSocket = false, delayPacket = false, duplicate = false, lost = false, errorSent = false;
	
	public ErrorSimThread(DatagramPacket packet, Scanner reader) {
		// Store the instance variables
        this.clientPort = packet.getPort();
		this.initPacket= packet;
		this.reader = reader;

        // Configure the logger
        DataHelper.configLogger();
        LOG = Logger.getLogger("global");

		try {
            // Set up the regular socket and the incorrect port socket
			sendReceiveSocket = new DatagramSocket();
			errorSendSocket = new DatagramSocket();

		} catch (SocketException se) {
			LOG.log(Level.SEVERE, se.getMessage(), se);
			System.exit(1);
		} 
	}

    /**
     * Edits the mode of the packet string by going to the 0 separating the file name and mode
     * and replacing the mode with the new mode passed.
     *
     * @param msg Byte array of the original packet
     * @param newMode New mode to replace in the request packet
     * @return Byte array of the new request message
     */
    public byte[] editMode(byte msg[], String newMode) {
        // Find the zero separating the text file name and the mode
        int index;
        for (index = msg.length - 2; index > 0; index--) {
            if (msg[index] == 0) {
                break;
            }
        }

        // Write the old message without the mode
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(msg, 0, index + 1);

        // Add the new mode from the user input
        bytes.write(newMode.getBytes(), 0, newMode.getBytes().length);

        return bytes.toByteArray();
    }

    /**
     * Creates the error packet based of the user input mode that the thread should be running in.
     *
     * Edits the original packet to contain the error the user entered at thread creation.
     *
     * @param msg Message to alter for the error packet
     * @param received Original packet to alter
     * @param port Port to send packet to
     * @return DatagramPacket with error
     */
	public DatagramPacket bringError(byte msg[], DatagramPacket received, int port) {
        // Create a copy of the message to alter
        byte newMsg[] = Arrays.copyOf(msg, received.getLength());

        if(mode.equals("00")){
        	// Normal operation, don't alter anything
        	return new DatagramPacket(newMsg, received.getLength(), received.getAddress(), port);

        } else if(mode.equals("01")){
        	// Change the OP Code
            byte[] bytes = DataHelper.getNewBlock(Integer.valueOf(argument));
            newMsg[0] = bytes[0];
            newMsg[1] = bytes[1];

        } else if(mode.equals("02")){
        	// Use a socket with a different port
        	if (!errorSent) {
        		errorSent = true;
        		errSocket = true;
        	}

        } else if(mode.equals("03")){
        	// Use socket with different InetAddress
        	if (!errorSent) {
        		errorSent = true;
        		errSocket = true;
        	}

        } else if(mode.equals("04")){
        	// Edit the last bit of the mode to make it invalid
            newMsg = editMode(newMsg, argument);
            return new DatagramPacket(newMsg,newMsg.length, received.getAddress(), port);

        } else if(mode.equals("05")){ //&& Integer.valueOf(args[1])>0){
	        // Delay the packet
        	delayPacket = true;

        }else if(mode.equals("06")){
        	// Duplicate the packet
        	duplicate = true;

        } else if(mode.equals("07")){
        	// Lose the packet
        	lost = true;
        	
        } else if (mode.equals("08")) {
        	// Copy into new array with new length and return the datagram packet with the new length
        	byte[] bytes = Arrays.copyOf(newMsg, Integer.valueOf(argument));
        	return new DatagramPacket(bytes, bytes.length, received.getAddress(), port);
        } else if (mode.equals("09") && !errorSent) {
        	byte[] bytes = DataHelper.getNewBlock(Integer.valueOf(argument));
            newMsg[2] = bytes[0];
            newMsg[3] = bytes[1];
            errorSent = true;
        }

        return new DatagramPacket(newMsg, received.getLength(), received.getAddress(), port);
    }

    /**
     * Delays the packet selected by creating a new thread to send the packet from delaying the send for 2 seconds.
     *
     * @param data DatagramPacket to delay the sending of
     */
	public void delayPacket(DatagramPacket data){
		Thread requests = new Thread(new DelayPack(delay, data, this));
        requests.start();
		
		delayPacket = false;
	}

    /**
     * Prints the UI menu to the screen for the user.
     */
	public void printErrorList() {
		System.out.print("\nThis is the error SIM. To choose your error, please print exactly what's between the quotations and the packet number afterwards: \n");
		System.out.print("\"00\": Normal Operations\n");
		System.out.print("\"01 [2 byte opcode] [ack|data] [block number]\": Change the OpCode\n");
	    System.out.print("\"02 [ack|data] [block number]\": Change to an invalid port number\n");
	    System.out.print("\"03 [ack|data] [block number]\": Change to a different Address\n");
	    System.out.print("\"04 [mode]\": Change the mode\n");
	    System.out.print("\"05 [ack|data] [block number]\": Delay Packet\n");
	    System.out.print("\"06 [ack|data] [block number]\": Duplicate Packet\n");
	    System.out.print("\"07 [ack|data] [block number]\": Lose Packet\n");
	    System.out.print("\"08 [new length] [ack|data] [block number]\": Change packet length\n");
	    System.out.print("\"09 [2 byte Block Number] [ack|data] [block number]\": Change packet block number\n");
	    System.out.print("> ");
	}

    /**
     * Sends the packet passed using the normal socket if the Error Socket flag is not set. Uses the Error Socket
     * if the flag is set and the packet is to be sent from an incorrect source.
     *
     * @param packet DatagramPacket to send from the socket
     */
	public void sendUsingSocket(DatagramPacket packet) {
		try {
            // If the packet is supposed to be sent from an incorrect address use the error socket
			if (errSocket) {
				errSocket = false;
				errorSendSocket.send(packet);
			} else {
                // Otherwise send from the normal socket
	            sendReceiveSocket.send(packet);
			}
	      } catch(IOException e) {
	         LOG.log(Level.SEVERE, e.getMessage(), e);
	         System.exit(1);
	      }
	}

	/**
	 * Checks if the byte array is the last data packet to be sent.
     *
	 * @param buffer Array of bytes that was passed to it 512 if the buffer was filled
	 * @return true if the buffer is smaller then 512
	 */
	public boolean checkIfDone(byte[] buffer) {
        return buffer[buffer.length - 1] == 0 && buffer[1] == 3;
    }

    /**
     * Sets the mode for the thread operation. Checks the input String to see if the user entered a valid
     * command and if so the mode is set and the packet count is reset.
     *
     * @param input User input string to check for mode
     * @return true if user input was valid and mode was set correctly
     */
	public boolean setMode(String input) {
		String args[] = input.split("\\s+");

        // All commands must have length two except the normal operation mode and the edited OP code
        if (args.length != 3 && !(args.length == 1 && Integer.valueOf(args[0]) == 0) &&
                                !(args.length == 4 && (Integer.valueOf(args[0]) == 1 || Integer.valueOf(args[0]) == 8 || Integer.valueOf(args[0]) == 9))) {
 	  		return false;
 	  	}

        // Check that the number represents one of our op modes
 	  	if (Integer.valueOf(args[0]) < 0 || Integer.valueOf(args[0]) > 9) {
 	  		return false;
 	  	}

        // Set the mode and second argument
 	  	mode = args[0];

        // If normal operation no need to store the arguments
        if (args.length == 1) {
            return true;
        }

        argument = args[1];

        // Check that the argument is valid
        if ((Integer.valueOf(args[0]) == 1 || Integer.valueOf(args[0]) == 9) && (argument.length() != 2 || args.length != 4)) {
            return false;
        } else if (Integer.valueOf(args[0]) == 8 && args.length != 4) {
        	return false;
        }

        // Set the mode to the first packet if the request is to be edited
        if (Integer.valueOf(args[0]) == 4) {
            blockNum = -1;
        } else if (Integer.valueOf(args[0]) == 1 || Integer.valueOf(args[0]) == 8 || Integer.valueOf(args[0]) == 9) {
            // We need to store the last argument instead for edited OP Code
            packetType = args[2];
            blockNum = Integer.valueOf(args[3]);
        } else {
            // Otherwise set the packet to the one specified by the user
            packetType = args[1];
            blockNum = Integer.valueOf(args[2]);
        }

 	  	return (packetType.equals(ACK) || packetType.equals(DATA));
	}

    /**
     * Method that receives a packet and sends it to the destination. Edits the packet to include an error
     * if the packet number expected to be changed is the same as the amount of packets received and sent.
     *
     * @return true if there are more packets that should be received in the transfer
     */
	public boolean recAndSend(){
        byte data[] = new byte[516];
		
		// Create a new receive packet
	    DatagramPacket receivePacket = new DatagramPacket(data, data.length);

	    // Wait until a new packet is received
	    try {
	       sendReceiveSocket.receive(receivePacket);
	    } catch(IOException e) {
	       LOG.log(Level.SEVERE, e.getMessage(), e);
	       System.exit(1);
	    }

        // Update the port if it hasn't been already
	    if (serverPort == -1) {
            serverPort = receivePacket.getPort();
	    }

	    DataHelper.printPacketData(receivePacket, "ErrorSim Received Packet", true, false);

        // If the data packet was received from the server send it to the client and vice versa
	    if(receivePacket.getPort() == serverPort){
            receivePacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), receivePacket.getAddress(), clientPort);
	    } else {
            receivePacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), receivePacket.getAddress(), serverPort);
	    }

        // Check if the packet should be edited to include an error
        byte[] opCode = Arrays.copyOfRange(receivePacket.getData(), 0, 2);
	    if(blockNum == DataHelper.getBlockNumber(receivePacket) &&
                ((packetType.equals(DATA) && Arrays.equals(opCode, OpCodes.DATA_CODE)) ||
                 (packetType.equals(ACK) && Arrays.equals(opCode, OpCodes.ACK_CODE)))) {
            // Edit the packet to include error
            receivePacket = bringError(DataHelper.minimi(receivePacket.getData(), receivePacket.getLength()), receivePacket, receivePacket.getPort());
	    }

        DataHelper.printPacketData(receivePacket, "ErrorSim Sending Packet", true, false);

        // If the packet received is an error packet end the transfer
        if(DataHelper.isErrorPacket(receivePacket)){
	    	  sendUsingSocket(receivePacket);
	    	  return false;
	    }

	    // Send the new packet
	    if (lost) {
            LOG.info("This packet is lost, it will not be sent...");
            lost = false;
	    } else if (duplicate) {
            LOG.info("This packet will be duplicated...");
            sendUsingSocket(receivePacket);
            sendUsingSocket(receivePacket);
            duplicate = false;
	    } else if(delayPacket){
            LOG.info("This packet will be delayed by 2 seconds...");
            delayPacket(receivePacket);
	    } else{
	    	  sendUsingSocket(receivePacket);
	    }

		return true;
	}

	/**
	 * Takes in information from the server thread, and passes it to the client and vice versa   
	 */
	@Override
	public void run() {
		// Initialize the variables
		String input;
	      
		// Get the user input
		while(true){
			printErrorList();			
	 	  	input = reader.nextLine();
	 	  	if(setMode(input)) {
	 	  		break;
	 	  	} else{
	 	  		LOG.warning("Incorrect input (do it again!)");
	 	  	}
		}

        // Create the datagram packet for the request
		DatagramPacket sent = new DatagramPacket(initPacket.getData(), initPacket.getLength(), initPacket.getAddress(), 69);

        // Edit the request if that's what the user wants
        if (blockNum == -1) {
            sent = bringError(initPacket.getData(), initPacket, 69);
        }

        // Send the packet
		sendUsingSocket(sent);

        // Loop until the send and receive method is finished
        boolean cont = true;
		while(cont){
			cont = recAndSend();
		}

		// We're finished, so close the sockets.
		sendReceiveSocket.close();
	}
	
}
