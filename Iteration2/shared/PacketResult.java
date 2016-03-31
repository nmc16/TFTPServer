package shared;

import java.net.DatagramPacket;

/**
 * Result object that holds the result of the socket operation.
 *
 * @version 1
 * @author Team6
 */
public class PacketResult {
    private boolean timeOut;
    private boolean success;
    private boolean duplicateData;
    private DatagramPacket packet;

    public PacketResult() {}

    public PacketResult(boolean timeOut, boolean success) {
        this.timeOut = timeOut;
        this.success = success;
        this.duplicateData = false;
    }

    public void setPacket(DatagramPacket packet) {
        this.packet = packet;
    }
    
    public void setDuplicateData(boolean duplicateData) {
    	this.duplicateData = duplicateData;
    }

    public void setTimeOut(boolean timeOut) {
        this.timeOut = timeOut;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public DatagramPacket getPacket() {
        return packet;
    }
    
    public boolean isDuplicatedData() {
    	return duplicateData;
    }

    public boolean isTimeOut() {
        return timeOut;
    }

    public boolean isSuccess() {
        return success;
    }
}
