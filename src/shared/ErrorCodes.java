package shared;

/**
 * Class to hold constants for all the TFTP Error codes sent between the Client and server.
 *
 * @version 1
 * @author Team6
 */
public class ErrorCodes {
    public static final byte FILE_NOT_FOUND[] = {0, 1};
    public static final byte ACCESS[] = {0, 2};
    public static final byte DISK_ERROR[] = {0, 3};
    public static final byte ILLEGAL_OP[] = {0, 4};
    public static final byte UNKNOWN_TID[] = {0, 5};
    public static final byte FILE_EXISTS[] = {0, 6};
}
