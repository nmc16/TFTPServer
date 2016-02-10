package shared;

import server.ServerSettings;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;

import exception.ExistsException;

/**
 * Holds helper methods shared by the server and client for parsing
 * messages.
 *
 * @version 1
 * @author Team6
 */
public class Helper {
	private Helper() {}

    /**
     * Change a byte array into a string of byte numbers.
     *
     * @param msg byte array to change
     * @return String of byte numbers
     */
    public static String changeToBytes(byte msg[]){
        String cud = "";
        int n = 0;
        int end = msg.length;

        while(n != end){
            cud = cud + msg[n] + " ";
            n = n + 1;
        }
        return cud;
    }

    /**
     * Minimizes byte array from a request and removes all the null data entries (zeros)
     * at the end of the data request.
     *
     * @param msg client request (read or write)
     * @param len Length of msg
     * @return minimized byte array
     */
    public static byte[] minimi(byte msg[], int len) {
        int n = 0;
        byte[] newmsg = new byte[len];

        while(n!=len){
            newmsg[n] = msg[n];
            n++;
        }

        return newmsg;
    }

    /**
	 * Creates a new text file if it doesn't already exist. If the file already exists
	 * an exception is thrown.
	 * 
	 * @param file file to check if it exists
	 * @throws ExistsException Thrown if file already exists
	 */
	public static void createFile(File file) throws ExistsException {
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
	 * Retrieves the file from the data packet and parses it to get the 
	 * file name.
	 * 
	 * @return Returns the file corresponding to the client request
	 */
    public static File getFile(DatagramPacket initialPacket) {
		int len = 0;
		int curr=2;
		
		// Loop until the first zero is reached
		while(initialPacket.getData()[curr] != 0){
			len++;
			curr++;
		}
		
		// Copy the filename from the array
		byte file[] = new byte[len];
		System.arraycopy(initialPacket.getData(), 2, file, 0, len);

		// Create the file and return
		String fileName = (new String(file));
		File f = new File(fileName);
		
		return f;
	}
	
    /**
     * Prints the packet received or sent data contents to the screen and 
     * the console input line after. Only prints if {@link ServerSettings}
     * verbose flag is set.
     * 
     * @param packet DatagramPacket to print data of
     * @param header Header to print at top to show how is displaying data 
     */
    public static void printPacketData(DatagramPacket packet, String header, boolean verbose) {
        // Print out the data on the received package if verbose mode on
        if (verbose) {
            System.out.println("\n" + header + ": ");
            System.out.println("From host: " + packet.getAddress());
            System.out.println("Host port: " + packet.getPort());
            System.out.println("Length: " + packet.getLength());

            // Form a String from the byte array.
            String received = new String(packet.getData(), 0, packet.getLength());
            byte[] mydata = minimi(packet.getData(), packet.getLength());

            // Minimize the data
            String bytereceived = changeToBytes(mydata);
            System.out.println("Containing: " + received);
            System.out.println("In bytes " + bytereceived + "\n\n");

            System.out.print("ENTER COMMAND > ");
        }
    }
}
