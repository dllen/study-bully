package cn.net.scp;

import cn.net.scp.fault.Omission;
import cn.net.scp.fault.Timing;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


public class Node {

    private final int uuid;
    private final int port;
    private final NodeType type;
    private final int timeout;

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    public Node(int uuid, int port, NodeType type, int timeout) {
        this.port = port;
        this.uuid = uuid;
        this.type = type;
        this.timeout = timeout;
    }

    public int getUuid() {
        return uuid;
    }

    public int getPort() {
        return port;
    }

    public NodeType getType() {
        return type;
    }

    /**
     * 触发 elect
     *
     * @return
     */
    public synchronized boolean elect() {
        boolean connectOk = connect();
        if (connectOk) {
            boolean ok = false;
            Bully.logger.log(String.format("Send Elect %d to %d.", Bully.self.getUuid(), getUuid()));
            Timing.artificialDelay();
            if (!Omission.omit()) {
                writer.println(Bully.self.getUuid());
                writer.println(Message.ELECT);
            }
            if (getMessage() == Message.OK) {
                ok = true;
                Bully.logger.logInternal(String.format("Received OKAY from %d.", getUuid()));
            }
            disconnect();
            return ok;
        }
        return false;
    }

    /**
     * 通知大家成为 leader
     */
    public synchronized void result() {
        boolean connectOk = connect();
        if (connectOk) {
            Bully.logger.log(String.format("Send Result %d to %d.", Bully.self.getUuid(), getUuid()));
            Timing.artificialDelay();
            if (!Omission.omit()) {
                writer.println(Bully.self.getUuid());
                writer.println(Message.RESULT);
            }
            disconnect();
        }
    }

    private Message getMessage() {
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                return Message.valueOf(line);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return null;
    }

    private boolean connect() {
        try {
            socket = new Socket("localhost", this.getPort());
            socket.setSoTimeout(timeout);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return true;
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return false;
    }

    private void disconnect() {
        try {
            socket.close();
            socket = null;
            writer = null;
            reader = null;
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
