package cn.net.scp.fault;


import cn.net.scp.Bully;

/**
 * Mock client crash
 */
public class Crasher extends Thread {

    final double threshold = 0.2;
    final long sleep = 50;

    public void run() {
        while (Math.random() > threshold) {
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Bully.logger.logInternal("Process crashed");
        System.exit(-1);
    }
}
