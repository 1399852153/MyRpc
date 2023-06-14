package myrpc.exception;

public class MyRpcTimeoutException extends RuntimeException{

    public MyRpcTimeoutException(String message) {
        super(message);
    }

    public MyRpcTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public MyRpcTimeoutException(Throwable cause) {
        super(cause);
    }
}
