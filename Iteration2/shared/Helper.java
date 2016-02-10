package shared;

import server.ServerSettings;

import java.net.DatagramPacket;

/**
 * Holds helper methods shared by the server and client for parsing
 * messages.
 *
 * @version 1
 * @author Team6
 */
public class Helper {

    /**
     * Change a byte array into a string of byte numbers
     *
     * @param msg byte array to change
     * @return String of byte numbers
     */
    public String changeToBytes(byte msg[]){
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
    public byte[] minimi(byte msg[], int len) {
        int n = 0;
        byte[] newmsg = new byte[len];

        while(n!=len){
            newmsg[n] = msg[n];
            n++;
        }

        return newmsg;
    }

    public void printPacketData(DatagramPacket packet, String clazz) {
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
