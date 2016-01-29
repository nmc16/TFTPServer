import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
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

public class Client {
    private DatagramPacket receivePacket;
    private DatagramSocket sendReceiveSocket;
    private static final byte READ_CODE[] = {0, 1};
    private static final byte WRITE_CODE[] = {0, 2};
    private static final byte DATA_CODE[] = {0, 3};
    private static final byte ACK_CODE[] = {0, 4};
    private static final int HOST_PORT = 68;
    private InetAddress address;
    private String location, mode;


    public Client() {
        try {
            // Construct a datagram socket and bind it to any available
            // port on the local host machine. This socket will be used to
            // send and receive UDP Datagram packets.
            Random r = new Random();
            address = InetAddress.getLocalHost();
            sendReceiveSocket = new DatagramSocket(r.nextInt(65553));
        } catch (SocketException se) {   // Can't create the socket.
            se.printStackTrace();
            System.exit(1);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

	public byte[] parseFile(int blockNumber) {
        byte[] data = new byte[512];
		if (location != null) {
			try {
				RandomAccessFile file = new RandomAccessFile(location, "rw");
				file.seek(blockNumber * 512);

				file.read(data, 0, data.length);
                return data;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		return null;
    }
    
    public void sendAndReceive(DatagramPacket sendPacket) {

        //Print out the info on the packet
        System.out.println("Client: Sending packet:");
        System.out.println("To host: " + sendPacket.getAddress());
        System.out.println("Destination host port: " + sendPacket.getPort());
        System.out.println("Length: " + sendPacket.getLength());
        System.out.print("Containing: " + new String(sendPacket.getData()));
        System.out.println("Byte form: " + Arrays.toString(sendPacket.getData()) + "\n\n");

        // Send the datagram packet to the intermediate via the send/receive socket.

        try {
            sendReceiveSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("Client: Packet sent.\n");

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
        	System.out.println("Client: Packet received:");
        	System.out.println("From host: " + receivePacket.getAddress());
        	System.out.println("Host port: " + receivePacket.getPort());
        	System.out.println("Length: " + receivePacket.getLength());
        	System.out.println("Containing: " + new String(receivePacket.getData()));
            System.out.println("In byte form: " + Arrays.toString(receivePacket.getData()) + "\n\n");
        	
        	// Check the OP Code
        	byte[] opCode = Arrays.copyOfRange(receivePacket.getData(), 0, 2);
            byte[] byteBlockNumber = Arrays.copyOfRange(receivePacket.getData(), 2, 4);
            DatagramPacket response = null;

            // If the code is an ACK then we need to send the next block of data
        	if (Arrays.equals(opCode, ACK_CODE)) {

                // Increment block number to next block
        		int blockNumber = (byteBlockNumber[1] & 0xFF) << 8 | (byteBlockNumber[0] & 0xFF);
        		blockNumber++;
        	
        		// Process the received datagram.
        		System.out.println("Client: Packet received:");
        		System.out.println("From host: " + receivePacket.getAddress());
        		System.out.println("Host port: " + receivePacket.getPort());
        		System.out.println("Length: " + receivePacket.getLength());
        		System.out.println("Containing: " + new String(receivePacket.getData()));
        		System.out.println("Byte form: " + Arrays.toString(receivePacket.getData()) + "\n\n");

                // Get the data from the file
        		byte[] b = parseFile(blockNumber);

                // If there is no more data left in the file break the loop
                if (b == null) {
                    break;
                }

                // Otherwise send the new packet to the server
                response = createPacket(DATA_CODE, b, receivePacket.getPort());


        	} else if (Arrays.equals(opCode, DATA_CODE)) {
                // Get the data
                byte[] transferred = Arrays.copyOfRange(receivePacket.getData(), 4, 516);
                String s = new String(transferred);
                System.out.println("Data: " + s);

                // Check if there is more data to be read or not
                if (transferred[transferred.length - 1] == 0) {
                    // No more data to be read
                    break;
                }

                // Otherwise send an acknowledge to the server
                response = createPacket(ACK_CODE, byteBlockNumber, receivePacket.getPort());
                System.out.println("Client: Sending packet:");
                System.out.println("To host: " + response.getAddress());
                System.out.println("Destination host port: " + response.getPort());
                System.out.println("Length: " + response.getLength());
                System.out.print("Containing: " + new String(response.getData()));
                System.out.println("Byte form: " + Arrays.toString(response.getData()) + "\n\n");
            }

        	System.out.println("SENDING STUFF");
            try {
                sendReceiveSocket.send(response);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

   }

    public void printMenu() {
        System.out.println(" Options:");
        System.out.println("    read [filename] [mode] - Reads the file from the server under filename");
        System.out.println("    write [filename] [file location] [mode] - Writes file at location to");
        System.out.println("                                               filename on server.");
        System.out.println("    help - Prints options screen.");
        System.out.println("    quit - Quits the client program.");
    }

    public DatagramPacket createPacket(byte[] opCode, String fileName, String mode) {
        return createPacket(opCode, fileName, mode, null);
    }

    public DatagramPacket createPacket(byte[] opCode, String fileName, String mode, String location) {
        // Check that the op code is valid before creating
        if (opCode.length != 2) {
            throw new IllegalArgumentException("Op code must be length 2! Found length " + opCode.length + ".");
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        buffer.write(opCode[0]);
        buffer.write(opCode[1]);

        buffer.write(fileName.getBytes(), 0, fileName.length());
        buffer.write(0);
        buffer.write(mode.getBytes(), 0, mode.length());
        buffer.write(0);

        this.location = location;
        this.mode = mode;
        
        return new DatagramPacket(buffer.toByteArray(), buffer.toByteArray().length, address, HOST_PORT);
    }

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

    private void runCommand(String args[]) {

        if (args[0].toLowerCase().equals("help")) {
            printMenu();
            return;
        }

        if (args[0].toLowerCase().equals("read")) {
            if (args.length != 3) {
                System.out.println("Instruction invalid length!");
                return;
            }
            DatagramPacket packet = createPacket(READ_CODE, args[1], args[2]);
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
                    System.out.println("Client shutting down...");
                    break;
                } else {
                    runCommand(args);
                }
            } else {
                System.out.println("Instruction invalid length!");
                return;
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