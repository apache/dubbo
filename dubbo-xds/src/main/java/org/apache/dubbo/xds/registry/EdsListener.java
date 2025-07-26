package org.apache.dubbo.xds.registry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.url.component.URLAddress;

import java.util.List;

public interface EdsListener {
    
    void onNotify(List<URL> addresses);
}
