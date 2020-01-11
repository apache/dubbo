package org.apache.dubbo.remoting.pilot;

import org.apache.dubbo.common.URL;

import java.util.List;

/**
 * ChildListener
 * @author hzj
 * @date 2019/03/20
 */
public interface ChildListener {

    void chiledChanged(String path, List<URL> urls);
}
