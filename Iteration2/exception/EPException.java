package exception;

import java.net.DatagramPacket;

public class EPException extends Exception {
	private DatagramPacket packet;
	public EPException(String message, DatagramPacket packet) {
		super(message); 
		this.packet = packet;
	}
	
	public DatagramPacket getPacket() {
		return packet;
	}
}
