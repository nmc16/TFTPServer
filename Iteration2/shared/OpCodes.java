package shared;

/**
 * Class to hold constants for all the TFTP Op codes sent between the Client and server.
 *
 * @version 1
 * @author Team6
 */
public class OpCodes {
    public static final byte READ_CODE[] = {0, 1};
    public static final byte WRITE_CODE[] = {0, 2};
    public static final byte DATA_CODE[] = {0, 3};
    public static final byte ACK_CODE[] = {0, 4};
    public static final byte ERR_CODE[] = {0, 5};
}
