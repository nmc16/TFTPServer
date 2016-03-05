package server;

import java.util.Scanner;

/**
 * Main server.Server program, takes in requests from intermediate host threads and makes a corresponding server thread
 *
 */
public class Server {

    public Server() {
        ServerSettings.stopRequests = false;
        ServerSettings.verbose = false;
    }

    /**
     * Prints the UI menu options to stdout
     */
    public void printMenu() {
        System.out.println(" Options:");
        System.out.println("    verbose [true|false] - Changes server display mode to verbose or silent mode.");
        System.out.println("    help - Prints options screen.");
        System.out.println("    quit - Quits the server.Server program after all requests handled.");
    }

    public void run() {
        Scanner reader = new Scanner(System.in);
        printMenu();

        Thread serverRequests = new Thread(new ServerRequest());
        serverRequests.start();

        while(true) {
            System.out.print("ENTER COMMAND > ");
            String input = reader.nextLine();

            // Parse the input and check for keywords
            String args[] = input.split("\\s+");

            if (args.length > 0) {
                if (args[0].toLowerCase().equals("quit")) {
                    break;
                } else if (args[0].toLowerCase().equals("help")) {
                    printMenu();
                } else if (args[0].toLowerCase().equals("verbose")) {
                    if (args.length == 2) {
                        ServerSettings.verbose = Boolean.valueOf(args[1]);
                        if(args[1].toLowerCase().equals("true")){
                        	System.out.println("Verbose mode set!");
                        } else if(args[1].toLowerCase().equals("false")){
                        	System.out.println("Verbose mode set to false!");
                        }
                    } else {
                        System.out.println("Verbose command must be followed by true or false!");
                    }
                }
            } else {
                System.out.println("Instruction invalid length!");
            }
        }

        // Set the flag to stop the server requests and shut down
        System.out.println("server.Server stopping all requests...");
        ServerSettings.stopRequests = true;

        // Wait for request handler to finish currently running requests
        try {
            serverRequests.join();
        } catch (InterruptedException e) {
            System.out.println("Could not stop requests properly: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("server.Server shutting down...");
        reader.close();
        System.exit(0);
    }

   public static void main(String args[]) {
       Server c = new Server();
	   c.run();
   }
}
