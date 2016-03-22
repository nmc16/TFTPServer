package shared;

import exception.AddressException;
import exception.EPException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared methods for socket operations between the client and server.
 *
 * @version 1
 * @author Team6
 */
public class SocketHelper {
    private final Logger LOG;
    private final DatagramSocket socket;

    public SocketHelper(DatagramSocket socket) {
        this.socket = socket;

        // Get the logger
        DataHelper.configLogger();
        LOG = Logger.getLogger("global");
    }

    /**
     * Receive method for the socket that receives the next data packet and compares it to the expected values.
     *
     * Throws exception if one of the values are not what was expected.
     *
     * Socket will time out after 1s. If a timeout occurs the DatagramPacket passed will be resent using the socket.
     *
     * The result returned holds whether the receive was a success or fail and whether
     * the socket timed out on the request. The result also holds the DatagramPacket received from transfer.
     *
     * @param timeOutCount Timeouts that have already happened to display in log
     * @param response DatagramPacket that should be resent on timeout
     * @param expectedAddress Expected InetAddress that the received packet should be from
     * @param expectedPort Expected port number that the received packet should be from
     * @param block Block number that is expected
     * @return PacketResult object that holds the result of the transfer
     * @throws IOException thrown if there is a problem with the received data packet
     */
    public PacketResult receiveWithTimeout(int timeOutCount, DatagramPacket response, InetAddress expectedAddress,
                                           int expectedPort, int block) throws IOException {
        
        PacketResult result = new PacketResult(false, false);
        byte[] buffer = new byte[516];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        // Block until a datagram is received via sendReceiveSocket.
        // TODO this needs to be changed, client should be able to timeout on request
        if(response != null && response.getPort() != 68){
            socket.setSoTimeout(1000);
        } else {
            socket.setSoTimeout(0);
        }

        try {
            // Receive the packet
            socket.receive(packet);

            // If the packet is an error, throw an exception
            if (DataHelper.isErrorPacket(packet)) {
                throw new EPException("Error packet received from Client!", packet);
            }

            // Update the address and port if not already updated
            if (expectedAddress == null || expectedPort == -1) {
                expectedAddress = packet.getAddress();
                expectedPort = packet.getPort();
            }

            // Check that the address and port received are the ones we were expecting
            if(!expectedAddress.equals(packet.getAddress()) || expectedPort != packet.getPort()){
                throw new AddressException("The address or TID was not correct during transfer: " +
                                           packet.getAddress() + ", " + packet.getPort());
            }

            // Check the packet is not duplicated
            if (block + 1 == DataHelper.getBlockNumber(packet) || block == 0 && DataHelper.getBlockNumber(packet) == 0) {
                result.setSuccess(true);
                result.setPacket(packet);
                return result;
            }

            // If we get here it must be a duplicated packet, ignore it
            LOG.warning("Received duplicate packet, ignoring...");
            return result;

        } catch(SocketTimeoutException e) {
            // Timed out, resend packet and do not continue
            LOG.warning("Receive timed out. Re-sending packet (attempts remaining: " + (5 - timeOutCount) + ")...");
            socket.send(response);
            result.setTimeOut(true);
            return result;
        }
    }


    /**
     * Creates datagram error packet using the error code and address. Sends the error packet using the
     * socket with the error code provided.
     *
     * @param errCode 2 byte Error Code
     * @param address to send the Error Packet to
     * @param port port to send Packet to
     * @param cause cause of the error packet
     */
    public void sendErrorPacket(byte[] errCode, InetAddress address, int port, Exception cause) {
        // Check that the Error code is valid before creating
        if (errCode.length != 2) {
            throw new IllegalArgumentException("Op code must be length 2! Found length " + errCode.length + ".");
        }

        // Create buffer array
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        // Write the Error Packet code
        buffer.write(0);
        buffer.write(5);

        // Write the error code and the message afterwards
        buffer.write(errCode[0]);
        buffer.write(errCode[1]);
        buffer.write(cause.getMessage().getBytes(), 0, cause.getMessage().length());
        buffer.write(0);

        // Log the error code that has occurred
        LOG.severe("Error code " + errCode[0] + errCode[1] + " has occurred. Closing the current request...");
        LOG.log(Level.SEVERE, cause.getMessage(), cause);

        // Create the packet and send it using the socket
        DatagramPacket errPack = new DatagramPacket(buffer.toByteArray(), buffer.toByteArray().length, address, port);
        try {
            socket.send(errPack);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }

}
