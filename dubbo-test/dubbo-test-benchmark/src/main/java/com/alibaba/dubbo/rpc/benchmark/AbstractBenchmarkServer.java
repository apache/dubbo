package com.alibaba.dubbo.rpc.benchmark;

/**
 * nfs-rpc Apache License http://code.google.com/p/nfs-rpc (c) 2011
 */
import java.text.SimpleDateFormat;
import java.util.Date;

import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;
import com.alibaba.dubbo.remoting.exchange.Exchangers;
import com.alibaba.dubbo.remoting.exchange.support.ExchangeHandlerAdapter;

/**
 * Abstract benchmark server Usage: BenchmarkServer listenPort maxThreads responseSize
 * 
 * @author <a href="mailto:bluedavy@gmail.com">bluedavy</a>
 */
public abstract class AbstractBenchmarkServer {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public void run(String[] args) throws Exception {
        if (args == null || args.length != 5) {
            throw new IllegalArgumentException(
                                               "must give three args: listenPort | maxThreads | responseSize | transporter | serialization");
        }
        int listenPort = Integer.parseInt(args[0]);
        int maxThreads = Integer.parseInt(args[1]);
        final int responseSize = Integer.parseInt(args[2]);
        String transporter = args[3];
        String serialization = args[4];
        System.out.println(dateFormat.format(new Date()) + " ready to start server,listenPort is: " + listenPort
                           + ",maxThreads is:" + maxThreads + ",responseSize is:" + responseSize
                           + " bytes,transporter is:" + transporter + ",serialization is:" + serialization);
        StringBuilder url = new StringBuilder();
        url.append("exchange://0.0.0.0:");
        url.append(listenPort);
        url.append("?transporter=");
        url.append(transporter);
        url.append("&serialization=");
        url.append(serialization);
        url.append("&threads=");
        url.append(maxThreads);
        Exchangers.bind(url.toString(), new ExchangeHandlerAdapter() {

            public Object reply(ExchangeChannel channel, Object message) throws RemotingException {
                return new ResponseObject(responseSize); // 发送响应
            }
        });
    }
}
