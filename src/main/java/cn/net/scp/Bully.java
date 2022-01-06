package cn.net.scp;

import cn.net.scp.fault.Crasher;
import cn.net.scp.fault.Omission;
import cn.net.scp.fault.Timing;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class Bully {

    //heartbeat timeout
    int timeout;
    boolean isInitiator = false;
    volatile boolean running = true;

    //当前节点
    public static Node self;
    public static Logger logger;
    public static Map<Integer, Node> nodes;

    final ExecutorService executor = Executors.newFixedThreadPool(2);
    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) {
        System.out.println("Args: " + Arrays.toString(args));
        Bully bully = new Bully();
        bully.getArgs(args);
        logger = new Logger(Bully.self.getUuid());
        if (self.getType() == NodeType.Crash) {
            new Crasher().start();
        }
        bully.listen();
    }

    void getArgs(String[] args) {
        try {
            //初始化当前节点
            Bully.self = new Node(Integer.parseInt(args[0]), Integer.parseInt(args[1]), NodeType.valueOf(args[2]), this.timeout);
            timeout = Integer.parseInt(args[3]);
            nodes = getNodesConfiguration(args[4]);
            if (args.length > 5) {
                isInitiator = args[5].equalsIgnoreCase("Initiator");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println(new IncorrectArgumentsException().getMessage());
            System.exit(-1);
        }
    }

    Map<Integer, Node> getNodesConfiguration(String file) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        Map<Integer, Node> nodes = new ConcurrentHashMap<>();

        String line;
        String[] data;

        Node newNode;
        while ((line = reader.readLine()) != null) {
            data = line.split(",");
            newNode = new Node(Integer.parseInt(data[0]), Integer.parseInt(data[1]), NodeType.Normal, this.timeout);
            nodes.put(newNode.getUuid(), newNode);
            System.out.printf("Loaded node: %d, port: %d%n", newNode.getUuid(), newNode.getPort());
        }
        reader.close();
        return nodes;
    }

    /**
     * 1.节点node向所有比自己大的节点发送选举消息(选举为election消息)
     * 2.如果节点node得不到任何回复(回复为alive消息)，那么节点node成为master，并向所有的其它节点宣布自己是master(宣布为Result消息)
     * 3.如果node得到了任何回复，node节点就一定不是master，同时等待Result消息，如果等待Result超时那么重新发起选举
     */
    void startElection() {
        logger.logInternal("Triggering an election.");
        boolean ok = false;
        Collection<Node> all = nodes.values();

        for (Node n : all) {
            if (n.getUuid() > self.getUuid()) {
                ok = n.elect() || ok;
            }
        }
        // No OK responses, become the new leader.
        if (!ok) {
            logger.log("Timeout Triggered.");
            sendResult();
            //doneElection(30);
        }
    }

    /**
     * 完成选举操作
     */
    void doneElection(int delay) {
        scheduler.schedule(() -> {
            running = false;
            System.exit(0);
        }, delay, TimeUnit.SECONDS);
    }

    void sendResult() {
        logger.logInternal("No response send out result.");
        Collection<Node> all = nodes.values();
        for (Node n : all) {
            // Send result to all except self
            if (n.getUuid() != self.getUuid()) {
                n.result();
            }
        }
    }


    void listen() {
        if (this.isInitiator) {
            startElection();
        }
        try (ServerSocket serverSocket = new ServerSocket(self.getPort())) {
            while (running) {
                Socket client = serverSocket.accept();
                executor.submit(() -> {
                    if (client != null) {
                        try {
                            client.setSoTimeout(timeout);
                            receive(client);
                        } catch (SocketException e) {
                            e.printStackTrace();
                        }
                    }
                });
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    //ignore
                    Thread.currentThread().interrupt();
                }
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + self.getPort());
            System.exit(-1);
        }
    }

    private void receive(Socket socket) {
        BufferedReader reader = null;
        PrintWriter writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            int senderID = Integer.parseInt(reader.readLine());
            Message message = Message.valueOf(reader.readLine());
            //开始选举
            if (message == Message.ELECT) {
                Timing.artificialDelay();
                if (!Omission.omit()) {
                    writer.println(Message.OK);
                }
                Bully.logger.log(String.format("Send OKAY to %d.", senderID));
                startElection();
            }
            //选取成功
            if (message == Message.RESULT) {
                Bully.logger.log(String.format("Received Result from %d.", senderID));
                //doneElection(10);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        try {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
