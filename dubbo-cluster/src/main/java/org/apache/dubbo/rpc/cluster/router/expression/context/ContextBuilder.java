package org.apache.dubbo.rpc.cluster.router.expression.context;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SPI
public interface ContextBuilder {

    Map<String, Object> buildClientContext(URL url, Invocation invocation);

    <T> Map<String, Object> buildServerContext(Invoker<T> invoker, URL url, Invocation invocation);
}
