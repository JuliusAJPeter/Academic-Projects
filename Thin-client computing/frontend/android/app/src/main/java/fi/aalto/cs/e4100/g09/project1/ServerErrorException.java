package fi.aalto.cs.e4100.g09.project1;

/**
 * Error occurred on remote server or in the connection and transfer.
 */

public class ServerErrorException extends Exception {

    public ServerErrorException(String message) {
        super(message);
    }
}
