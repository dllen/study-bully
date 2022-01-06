package cn.net.scp.fault;


import cn.net.scp.Bully;
import cn.net.scp.NodeType;

/**
 * Mock timing op
 */
public class Timing {

    static final double maxDelay = 2000;

    public static void artificialDelay() {
        if (Bully.self.getType() != NodeType.Timing) {
            return;
        }
        try {
            long delay = (long) (Math.random() * maxDelay);
            Bully.logger.logInternal("Artificial delay: " + delay);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
