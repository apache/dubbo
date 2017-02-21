package com.alibaba.dubbo.rpc.protocol.hprose;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;
import hprose.client.HproseHttpClient;
import hprose.client.HproseTcpClient;
import hprose.server.HproseHttpService;
import hprose.server.HproseTcpServer;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Created by wuyu on 2017/2/3.
 */
public class HproseTcpProtocol extends AbstractProxyProtocol {

    private final Map<Integer, HproseTcpServer> hproseHttpServiceMap = new ConcurrentHashMap<>();

    private final List<HproseTcpClient> hproseHttpClients = new ArrayList<>();

    @Override
    public int getDefaultPort() {
        return 4321;
    }

    @Override
    protected <T> Runnable doExport(T impl, final Class<T> type, final URL url) throws RpcException {
        final int port = url.getPort();
        int timeout = url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        int connections = url.getParameter(Constants.THREADS_KEY, 200);

        HproseTcpServer hproseServer = hproseHttpServiceMap.get(port);
        if (hproseServer == null) {
            try {
                hproseServer = new HproseTcpServer("tcp://" + (url.getParameter("anyhost", false) ? url.getHost() : "0.0.0.0") + ":" + url.getPort());
                hproseServer.setTimeout(timeout);
                hproseServer.setThreadPool(Executors.newFixedThreadPool(connections));
                hproseServer.start();
            } catch (Exception e) {
                throw new RpcException(e);
            }
            hproseHttpServiceMap.put(port, hproseServer);
        }
        hproseServer.add(impl, type, type.getName());

        return new Runnable() {
            @Override
            public void run() {
                HproseTcpServer hproseTcpServer = hproseHttpServiceMap.get(port);
                if (hproseTcpServer != null) {
                    hproseTcpServer.remove(type.getName());
                }
            }
        };
    }

    @Override
    protected <T> T doRefer(Class<T> type, URL url) throws RpcException {
        int timeout = url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        int connections = url.getParameter(Constants.CONNECTIONS_KEY, 20);

        String api = "tcp://" + url.getHost() + ":" + url.getPort();

        final HproseTcpClient hproseHttpClient = new HproseTcpClient(api);
        HproseHttpClient.setThreadPool(Executors.newFixedThreadPool(connections));
        hproseHttpClient.setRetry(0);
        hproseHttpClient.setTimeout(timeout);
        hproseHttpClients.add(hproseHttpClient);
        return hproseHttpClient.useService(type, type.getName());
    }

    @Override
    public void destroy() {
        for (HproseTcpServer hproseTcpServer : hproseHttpServiceMap.values()) {
            try {
                hproseTcpServer.stop();
            } catch (Exception e) {

            }
        }
        for (HproseTcpClient hproseTcpClient : hproseHttpClients) {
            try {
                hproseTcpClient.close();
            } catch (Exception e) {

            }
        }
    }

}
