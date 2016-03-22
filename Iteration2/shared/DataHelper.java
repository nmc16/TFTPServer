package shared;

import server.ServerSettings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.logging.*;

/**
 * Holds helper methods shared by the server and client for parsing
 * messages.
 *
 * @version 1
 * @author Team6
 */
public class DataHelper {
    private static final Logger LOG = Logger.getLogger("global");
    private static boolean isLoggerSetup = false;

	private DataHelper() {}

    /**
     * Configures the global logger if it has not already been logged.
     */
    public static void configLogger() {
        if (!isLoggerSetup) {

            // Set the logging format
            System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] %2$s (%4$s): %5$s%6$s%n");
            LOG.setLevel(Level.ALL);

            // Remove all of the handlers from the logger
            Handler handlers[] = LOG.getHandlers();
            for (Handler handler : handlers) {
                LOG.removeHandler(handler);
            }

            // We don't want to use the parent logger
            LOG.setUseParentHandlers(false);

            // Setup the handler
            CustomHandler errorHandler = new CustomHandler(System.err, System.out, new SimpleFormatter());
            LOG.addHandler(errorHandler);

            isLoggerSetup = true;
        }
    }

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
     * Checks if the DatagramPacket represents an error code.
     *
     * @param packet DatagramPacket to check Op Code
     * @return true if packet is an error packet
     */
    public static boolean isErrorPacket(DatagramPacket packet) {
        return (packet.getData()[0] == 0 && packet.getData()[1] == 5);
    }

    /**
     * Converts the two bytes from the DatagramPacket representing the block number (2-4)
     * into an integer value.
     *
     * @param packet DatagramPacket to get the block number from
     * @return Integer representation of byte array block number
     */
    public static int getBlockNumber(DatagramPacket packet) {
        byte[] blockNumber = Arrays.copyOfRange(packet.getData(), 2, 4);
        return (int) ByteBuffer.wrap(blockNumber).getShort();
    }

    /**
     * Converts an integer value into a short and then into a 2 byte array.
     *
     * Values will be cut if the integer is larger than representable by a short.
     *
     * @param blockNumber Block number to convert
     * @return 2 byte array representing block number
     */
    public static byte[] getNewBlock(int blockNumber) {
        return ByteBuffer.allocate(2).putShort((short)blockNumber).array();
    }
	
    /**
     * Prints the packet received or sent data contents to the screen and 
     * the console input line after. Only prints if {@link ServerSettings}
     * verbose flag is set.
     * 
     * @param packet DatagramPacket to print data of
     * @param header Header to print at top to show how is displaying data 
     */
    public static void printPacketData(DatagramPacket packet, String header, boolean verbose, boolean tail) {
        // Check for NPE
        if (packet == null) {
            return;
        }

        // Print out the data on the received package if verbose mode on
        if (verbose) {
            // Form a String from the byte array that has zeros removed
            String received = new String(packet.getData(), 0, packet.getLength());
            String byteReceived = changeToBytes(minimi(packet.getData(), packet.getLength()));

            // Log the info to the console
            if (isErrorPacket(packet)) {
                LOG.severe("\n" + header + ":" +
                        "\n\tFrom host: " + packet.getAddress() +
                        "\n\tHost port: " + packet.getPort() +
                        "\n\tLength: " + packet.getLength() +
                        "\n\tContaining: " + received +
                        "\n\tByte array: " + byteReceived);
            } else {
                LOG.info("\n" + header + ":" +
                        "\n\tFrom host: " + packet.getAddress() +
                        "\n\tHost port: " + packet.getPort() +
                        "\n\tLength: " + packet.getLength() +
                        "\n\tContaining: " + received +
                        "\n\tByte array: " + byteReceived);
            }

            if (tail) {
                System.out.print("\nENTER COMMAND > ");
            }
        }
    }
}
