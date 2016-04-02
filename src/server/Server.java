package server;

import shared.DataHelper;

import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Server class that runs the UI and starts the ServerRequest thread that will delegate all the
 * incoming requests.
 *
 * Handles the stopping of the server and the output settings.
 *
 * @version 2
 * @author Team6
 */
public class Server {
    private final Logger LOG;

    public Server() {
        DataHelper.configLogger();
        LOG = Logger.getLogger("global");
        ServerSettings.stopRequests = false;
        ServerSettings.verbose = false;
    }

    /**
     * Prints the UI menu options to the screen for the user.
     */
    public void printMenu() {
        System.out.println(" Options:");
        System.out.println("    verbose [true|false] - Changes server display mode to verbose or silent mode.");
        System.out.println("    help - Prints options screen.");
        System.out.println("    quit - Quits the Server program after all requests handled.");
    }

    /**
     * Runs the Server UI to control the user input and control when to shut down the Server requests.
     */
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
                        LOG.info("Verbose mode set to " + ServerSettings.verbose + "!");
                    } else {
                        LOG.warning("Instruction invalid length!");
                    }
                }
            } else {
                LOG.warning("Instruction invalid length!");
            }
        }

        // Set the flag to stop the server requests and shut down
        LOG.info("Server stopping all requests...");
        ServerSettings.stopRequests = true;

        // Wait for request handler to finish currently running requests
        try {
            serverRequests.join();
        } catch (InterruptedException e) {
            LOG.severe("Could not stop requests properly: " + e.getMessage());
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }

        LOG.info("Server shutting down...");
        reader.close();
        System.exit(0);
    }

   public static void main(String args[]) {
       Server c = new Server();
	   c.run();
   }
}
