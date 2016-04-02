package shared;

import exception.DiskException;
import exception.ExistsException;

import java.io.*;
import java.net.DatagramPacket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessControlException;
import java.security.AccessController;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared methods to help with file I/O Operations. Deals with reads and writes and is able to do file
 * manipulation like creation and permission checking.
 *
 * @version 1
 * @author Team6
 */
public class FileHelper {
    private FileHelper() {}

    /**
     * Checks the file has the permission passed.
     *
     * @param file File to check permissions on
     * @param perm String representing permission i.e. "read", "write"
     * @return true if file has permissions, false otherwise
     */
    public static boolean hasPermission(File file, String perm) {
        Path path = Paths.get(file.getAbsolutePath());
        boolean a = Files.isReadable(path);

        if (a) {
            return true;
        }

        try {
            // Check the file permissions
            FilePermission fp = new FilePermission(file.getPath(), perm);
            AccessController.checkPermission(fp);
            return true;
        } catch (AccessControlException e) {
            // If thrown the file doesn't have access
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Parses the file by blocks using the block number given where each block holds 512
     * bytes. Reads one block at a time.
     *
     * Synchronized method so that only one thread can access the system resource.
     *
     * @param blockNumber Block number to read from where 1 represents the first block in the file (byte 0)
     * @param location path to the file on the system
     * @param charset Character encoding to use for the read
     * @return returns the byte array (size 512) that holds the block parsed from the file
     * @throws java.io.IOException thrown if there is a problem accessing the file
     * @throws java.lang.SecurityException thrown if the file is not readable
     */
    public static synchronized byte[] parseFile(int blockNumber, String location, Charset charset) throws IOException {
        char[] data = new char[512];
        int offset = (blockNumber - 1) * 512;

        // Check that the file we are trying to read exists
        if (!Files.exists(Paths.get(location))) {
            throw new FileNotFoundException("Could not find the file to read from path: " + location);
        }

        // Check the file has read permissions
        File file = new File(location);
        if (!hasPermission(file, "read")) {
            throw new SecurityException("Access Denied: File " + file.getAbsolutePath() + " does not have" +
                                        " read access");
        }

        try {
            // Create the file reader using the charset passed
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(location), charset));

            // Check if there is less than 512 bytes left in the file to read
            if((file.length() - offset < 512 && (file.length() - offset) > 0)) {
                // Set the new size of the data array to the bytes left in the file
                int newSize = (int) file.length() - (blockNumber - 1) * 512;
                data = new char[newSize];
            } else if ((file.length() - offset) <= 0) {
                data = new char[0];
            }

            // Skip to the block we want to read from
            br.skip(offset);

            // Read the data and close the reader
            br.read(data, 0, data.length);
            br.close();

            return new String(data).getBytes();

        }   catch (IOException e) {
            // Wrap the IO Exception into our exception
            throw new DiskException(e.getMessage(), e);
        }
    }

    /**
     * File write method for the client side read that will write to the
     * string contents passed to the file in append mode so that no data is overwritten.
     *
     * @param data Data to write to file
     * @param file File to append data into
     */
    public static synchronized void writeFile(String data, File file) throws IOException {
        // Check that the file we are trying to write to exists
        if (!Files.exists(Paths.get(file.getAbsolutePath()))) {
            throw new FileNotFoundException("Could not find the file to write from path: " + file.getAbsolutePath());
        }

        // Check the file has write permissions
        if (!hasPermission(file, "write")) {
            throw new SecurityException("Access Denied: File " + file.getAbsolutePath() + " does not have" +
                                        " write access");
        }

        try {
            // Open a file writer in append mode
            FileWriter fw = new FileWriter(file, true);

            // Write the data and close the writer
            fw.write(data);
            fw.close();
        } catch (IOException e) {
            // Wrap the IO Exception into our exception
            throw new DiskException(e.getMessage(), e);
        }
    }


    /**
     * Creates a new text file if it doesn't already exist. If the file already exists
     * an exception is thrown.
     *
     * @param file file to check if it exists
     * @throws ExistsException thrown if file already exists
     * @throws java.lang.SecurityException thrown if the file could not be created
     */
    public static void createFile(File file) throws ExistsException {
        Path path = Paths.get(file.getAbsolutePath());
        if (Files.exists(path)) {
            throw new ExistsException("File already exists at path: " + file.getAbsolutePath());
        }

        try {
            Files.createFile(path);
        } catch (IOException e) {
            throw new SecurityException("Could not create file from path: " + file.getAbsolutePath());
        }
    }

    /**
     * Retrieves the file from the data packet and parses it to get the
     * file name.
     *
     * @return Returns the file corresponding to the client request
     */
    public static File getFileFromPacket(DatagramPacket initialPacket) {
        int len = 0;
        int curr = 2;

        // Loop until the first zero is reached
        while(initialPacket.getData()[curr] != 0){
            len++;
            curr++;
        }

        // Copy the filename from the array
        byte file[] = new byte[len];
        System.arraycopy(initialPacket.getData(), 2, file, 0, len);

        // Create the file and return
        String fileName = (new String(file));
        return new File(fileName);
    }

    /**
     * Method that will create the sub directories in a path from the running directory
     * of the program. Strips the file name away from the path and creates the rest of the path
     * as directories.
     *
     * @param path Path to save file under the root where the project is being run
     */
    public static void createSubDirectories(String path) {
        // Check if the path contains folder delimiters
        if (path.contains("\\") || path.contains("//")) {
            // Remove the filename from the directories
            Path p = Paths.get(path);
            String folders = path.replace(p.getFileName().toString(), "");

            // Create the folders if they don't already exist
            File file = new File(folders);
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    throw new SecurityException("Could not create subdirectories: " + folders);
                }
            }
        }
    }
    
    public static synchronized void removeFailedFile(String location){
    	DataHelper.configLogger();
    	Logger log = Logger.getLogger("global");
    	Path path = Paths.get(location);
    	try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		} 
    }
}
