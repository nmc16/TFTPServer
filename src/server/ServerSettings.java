package server;

/**
 * Settings class to check for verbose settings and if the request threads should shut down from
 * user input.
 *
 * @version 1
 * @author Team6
 */
public class ServerSettings {
    public static volatile boolean stopRequests = false;
    public static volatile boolean verbose = false;

    private ServerSettings() {}
}
