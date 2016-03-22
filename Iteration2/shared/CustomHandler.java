package shared;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.logging.*;

/**
 * Output handler that selects the output stream based on the LogRecord received.
 *
 * Handler will select standard out if the level is below WARNING and will select
 * standard error otherwise to print the message with the formatter.
 *
 * @version 1
 * @author Team6
 */
public class CustomHandler extends Handler {

    private OutputStreamWriter error;
    private OutputStreamWriter stdout;

    public CustomHandler(OutputStream error, OutputStream stdout, Formatter formatter) {
        super();

        // Save the instance variables
        this.error = new OutputStreamWriter(error);
        this.stdout = new OutputStreamWriter(stdout);


        // Configure the formatter
        setFormatter(formatter);
    }

    /**
     * Checks if the record given is loggable by checking the streams can write the record.
     *
     * @param record LogRecord to write using one of the writers
     * @return True if the record is loggable, false otherwise
     */
    public boolean isLoggable(LogRecord record) {
        return !(error == null || stdout == null || record == null) && super.isLoggable(record);
    }

    /**
     * Publish the LogRecord using the error stream writer if the level is WARNING or above
     * and otherwise write using the standard out writer.
     *
     * Flushes the output on both writers after it is written.
     *
     * @param record LogRecord to write using one of the writers
     */
    @Override
    public void publish(LogRecord record) {
        // Check the record can be logged
        if (!isLoggable(record)) {
            return;
        }

        try {
            // Format the message and print it
            String message = getFormatter().format(record);

            // Find what level and print to the appropriate output
            if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                error.write(message);
            } else {
                stdout.write(message);
                stdout.flush();
            }

        } catch (Exception e) {
            // Catch the exception and report it
            reportError(null, e, ErrorManager.FORMAT_FAILURE);
        }
    }

    /**
     * Flushes both the standard output and error output writers given they are initialized.
     */
    @Override
    public void flush() {
        // Make sure the error output writer is initialized
        if (error != null) {
            try {
                // Flush the writer
                error.flush();
            } catch (Exception e) {
                // Catch the exception and report it
                reportError(e.getMessage(), e, ErrorManager.CLOSE_FAILURE);
            }
        }

        // Make sure the output writer is initialized
        if (stdout != null) {
            try {
                // Flush the writer
                stdout.flush();
            } catch (Exception e) {
                // Catch the exception and report it
                reportError(null, e, ErrorManager.CLOSE_FAILURE);
            }
        }
    }

    /**
     * Closes both of the output writers given they are initialized.
     *
     * @throws SecurityException Not thrown
     */
    @Override
    public void close() throws SecurityException {
        // Make sure output writer initialized
        if (error != null) {
            try {
                error.close();
            } catch (Exception e) {
                // Catch the exception and report it
                reportError(e.getMessage(), e, ErrorManager.CLOSE_FAILURE);
            }
        }

        // Make sure output writer initialized
        if (stdout != null) {
            try {
                stdout.close();
            } catch (Exception e) {
                // Catch the exception and report it
                reportError(e.getMessage(), e, ErrorManager.CLOSE_FAILURE);
            }
        }
    }
}
