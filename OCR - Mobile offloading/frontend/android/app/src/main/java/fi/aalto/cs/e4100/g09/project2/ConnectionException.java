package fi.aalto.cs.e4100.g09.project2;

/**
 * Error occurred on remote server or in the connection and transfer.
 */

public class ConnectionException extends Exception {

    public ConnectionException(String message) {
        super(message);
    }
}
