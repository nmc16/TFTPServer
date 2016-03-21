package exception;

/**
 * Exception to throw on IO operations on the disk.
 *
 * @version 1
 * @author Team6
 */
public class DiskException extends TFTPException {
    public DiskException(String message, Throwable cause) {
        super(message, cause);
    }
}
