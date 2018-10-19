package cn.enn.portal.jobCenter.core.exception;

public class IncompatibleClassException extends RuntimeException {
    public IncompatibleClassException() {
    }

    public IncompatibleClassException(String msg) {
        super(msg);
    }

    public IncompatibleClassException(String msg, Throwable throwable) {
        super(msg, throwable);
    }
}
