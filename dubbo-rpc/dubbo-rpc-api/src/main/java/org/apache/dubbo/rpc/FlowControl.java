package org.apache.dubbo.rpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;

import org.apache.dubbo.rpc.flowcontrol.AutoConcurrencyLimier;
import org.apache.dubbo.rpc.flowcontrol.HeuristicSmoothingFlowControl;

@SPI(AutoConcurrencyLimier.NAME)
public interface FlowControl {
    int getMaxConcurrency();
    boolean Begin();
    void End(long latency);
}
