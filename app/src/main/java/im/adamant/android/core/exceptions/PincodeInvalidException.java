package im.adamant.android.core.exceptions;

public class PincodeInvalidException extends Exception {
    public enum Reason {
        NOT_MATCH,
        TIMEOUT,
        EMPTY,
        INVALID
    }

    private int attempts;
    private Reason reason;

    public PincodeInvalidException(int attempts, Reason reason) {
        this.attempts = attempts;
        this.reason = reason;
    }

    public int getAttempts() {
        return attempts;
    }

    public Reason getReason() {
        return reason;
    }
}
