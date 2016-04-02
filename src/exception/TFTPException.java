package exception;

import java.io.IOException;

/**
 * Super class for all checked exceptions thrown from client and server.
 *
 * @version 1
 * @author Team6
 */
public class TFTPException extends IOException {
    public TFTPException(String message) {
        super(message);
    }

    public TFTPException(String message, Throwable cause) {
        super(message, cause);
    }
}
