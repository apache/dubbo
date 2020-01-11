package org.apache.dubbo.remoting.pilot;

/**
 * StateListener
 * @author hzj
 * @date 2019/03/20
 */
public interface StateListener {

    int DISCONNECTED = 0;

    int CONNECTED = 1;

    void stateChanged(int connected);
}
