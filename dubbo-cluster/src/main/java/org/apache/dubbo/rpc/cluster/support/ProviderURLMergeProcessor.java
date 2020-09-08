package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;

import java.util.Map;

@SPI
public interface ProviderURLMergeProcessor {
    URL mergeProviderUrl(URL providerUrl, Map<String, String> localParametersMap);

    boolean accept(URL providerUrl, Map<String, String> localParametersMap);
}
