package org.apache.dubbo.xds.protocol;

import java.util.List;

public interface XdsResourceListener<T> {


    void onResourceUpdate(List<T> resource);

}
