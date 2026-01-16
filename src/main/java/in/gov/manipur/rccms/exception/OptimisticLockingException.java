package in.gov.manipur.rccms.exception;

/**
 * Exception thrown when optimistic locking conflict is detected
 * Indicates that the resource was modified by another user/process
 */
public class OptimisticLockingException extends RuntimeException {
    
    public OptimisticLockingException(String message) {
        super(message);
    }
    
    public OptimisticLockingException(String message, Throwable cause) {
        super(message, cause);
    }
}

