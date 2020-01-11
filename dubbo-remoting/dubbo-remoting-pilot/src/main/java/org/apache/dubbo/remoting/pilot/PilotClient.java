package org.apache.dubbo.remoting.pilot;

import org.apache.dubbo.common.URL;

import java.util.List;
import java.util.Set;

/**
 * interface for pilot client
 * @author hzj
 * @date 2019/03/20
 */
public interface PilotClient {

    /**
     * send service subscribe request
     */
    void discoveryService(URL url);

    /**
     * check the connection status
     */
    boolean isConnected();

    /**
     * close channel
     */
    void close();

    /**
     * close all watch stream
     */
    void closeOldStream();

    /**
     * add connect state listener
     */
    void addStateListener(StateListener listener);

    /**
     * remove connect state listener
     */
    void removeStateListener(StateListener listener);

    /**
     * get connect state listeners
     */
    List<StateListener> getStateListeners();

    /**
     * add watch result listener
     */
    Set<ChildListener> addChildListener(String path, ChildListener listener);

    /**
     * remove watch result listener
     */
    void removeChildListener(String path, ChildListener listener);

    /**
     * get watch result listeners
     */
    Set<ChildListener> getChildListeners(String path);
}
