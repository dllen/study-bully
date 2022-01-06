package cn.net.scp.fault;

import cn.net.scp.Bully;
import cn.net.scp.NodeType;

/**
 * Mock mission
 */
public class Omission {

    static final double threshold = 0.2;

    public static boolean omit() {
        if (Bully.self.getType() != NodeType.Omission) {
            return false;
        }
        boolean result = Math.random() < threshold;
        if (result) {
            Bully.logger.logInternal("Omission.");
        }
        return result;
    }
}
