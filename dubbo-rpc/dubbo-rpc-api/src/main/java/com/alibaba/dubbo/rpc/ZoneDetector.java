package com.alibaba.dubbo.rpc;

import com.alibaba.dubbo.common.extension.SPI;

/**
 * Extend and provide your own implementation if you want to distribute traffic around registries.
 * Please, name it as 'default'
 */
@SPI
public interface ZoneDetector {

    String getZoneOfCurrentRequest(Invocation invocation);

    String isZoneForcingEnabled(Invocation invocation, String zone);

}
