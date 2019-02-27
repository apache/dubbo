package org.apache.dubbo.remoting.zookeeper;

import java.util.List;

/**
 * @author cvictory ON 2019-02-26
 */
public interface DataListener {

    void dataChanged(String path, Object value, EventType eventType);
}
