package fi.aalto.cs.e4100.g09.project1;

/**
 * Error occurred on remote server or in the connection and transfer.
 */

public class ForbiddenException extends Exception {

    public ForbiddenException(String message) {
        super(message);
    }
}
