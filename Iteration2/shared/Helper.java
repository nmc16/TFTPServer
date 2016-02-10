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
     * Change a byte array into a string of byte numbers
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
     * Minimizes byte array from a request
     *
     * @param msg client request (read or write)
     * @param len len of msg
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
	 * creates a new text file if it doesn't already exist
	 * @param file file to check if it exists
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
	 * 
	 * @return returns the file corresponding to the client request
	 */
    public static File getFile(DatagramPacket initialPacket) {
		int len = 0;
		int curr=2;
		
		while(initialPacket.getData()[curr] != 0){
			len++;
			curr++;
		}
		
		byte file[] = new byte[len];
		System.arraycopy(initialPacket.getData(), 2, file, 0, len);

		String fileName = (new String(file));
		File f = new File(fileName);
		
		return f;
	}
	
    public static void printPacketData(DatagramPacket packet, String clazz) {
        // Print out the data on the received package if verbose mode on
        if (ServerSettings.verbose) {
            System.out.println("\n" + clazz + " Packet received:");
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
