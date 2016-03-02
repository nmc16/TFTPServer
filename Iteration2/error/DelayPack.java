package error;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

import shared.Helper;

public class DelayPack implements Runnable {
	
	private int delay;
	private DatagramPacket data;
	private DatagramSocket sendSocket;
	private ErrorSimThread es;
	
	
	public DelayPack(int delay, DatagramPacket data, ErrorSimThread es){
		this.delay = delay;
		this.data = data;
		this.es = es;
		
		
		 try {
		    	Random r = new Random();
		        sendSocket = new DatagramSocket(r.nextInt(65500));
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
	}
	/*
	public void sendUsingSocket(DatagramPacket packet) {
		try {
			
	            sendSocket.send(packet);
			}
	      catch(IOException e) {
	         e.printStackTrace();
	         System.exit(1);
	      }
	}
	*/
	
	
	@Override
	public void run() {
		try{
			Thread.sleep(delay);
		}
		catch(InterruptedException e){
			
		}
		
		es.sendUsingSocket(data);
		
	}
	
}
