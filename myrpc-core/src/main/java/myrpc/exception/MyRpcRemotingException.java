package myrpc.exception;

public class MyRpcRemotingException extends RuntimeException{

    public MyRpcRemotingException(String message) {
        super(message);
    }

    public MyRpcRemotingException(String message, Throwable cause) {
        super(message, cause);
    }

    public MyRpcRemotingException(Throwable cause) {
        super(cause);
    }
}
