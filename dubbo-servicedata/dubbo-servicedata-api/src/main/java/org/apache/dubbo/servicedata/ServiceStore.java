package org.apache.dubbo.servicedata;


import org.apache.dubbo.common.URL;

/**
 * @author cvictory ON 2018/8/24
 */
public interface ServiceStore {

    void put(URL url);

    void remove(URL url);

    URL peek(URL url);
}
