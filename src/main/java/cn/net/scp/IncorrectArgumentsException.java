package cn.net.scp;

public class IncorrectArgumentsException extends Exception {

    private static final long serialVersionUID = 3582791246080108540L;

    public IncorrectArgumentsException() {
        super("Incorrect arguments.\nUsage: java -jar bully.jar uuid port nodeType timeout configuration-file [Initiator]");
    }

    public IncorrectArgumentsException(String message) {
        super(message + "\nUsage: java -jar bully.jar uuid port nodeType timeout configuration-file [Initiator]");
    }
}
