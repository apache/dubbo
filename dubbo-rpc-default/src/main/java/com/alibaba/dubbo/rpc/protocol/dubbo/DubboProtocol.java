/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.protocol.dubbo;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.Transporter;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.remoting.exchange.ExchangeHandler;
import com.alibaba.dubbo.remoting.exchange.ExchangeServer;
import com.alibaba.dubbo.remoting.exchange.Exchangers;
import com.alibaba.dubbo.remoting.exchange.support.ExchangeHandlerAdapter;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.RpcConstants;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.protocol.AbstractProtocol;

/**
 * dubbo protocol support.
 *
 * @author qian.lei
 * @author william.liangf
 * @author chao.liuc
 */
@Extension(DubboProtocol.NAME)
public class DubboProtocol extends AbstractProtocol {

    public static final String NAME = "dubbo";

    public static final String COMPATIBLE_CODEC_NAME = "dubbo1compatible";
    
    public static final int DEFAULT_PORT = 20880;
    
    public final ReentrantLock lock = new ReentrantLock();
    
    private final Map<String, ExchangeServer> serverMap = new ConcurrentHashMap<String, ExchangeServer>(); // <host:port,Exchanger>
    
    private final Map<String, ExchangeClient> clientMap = new ConcurrentHashMap<String, ExchangeClient>(); // <host:port,Exchanger>
    
    //consumer side export a stub service for dispatching event
    //servicekey-stubmethods
    private final ConcurrentMap<String, String> stubServiceMethodsMap = new ConcurrentHashMap<String, String>();

    private ExchangeHandler requestHandler = new ExchangeHandlerAdapter() {
        private boolean isClientSide(Channel channel) {
            InetSocketAddress address = channel.getRemoteAddress();
            URL url = channel.getUrl();
            return url.getPort() == address.getPort() && 
                        NetUtils.filterLocalHost(channel.getUrl().getHost())
                        .equals(NetUtils.filterLocalHost(address.getAddress().getHostAddress()));
        }
        
        public Object reply(ExchangeChannel channel, Object message) throws RemotingException {
            if (message instanceof Invocation) {
                boolean isCallBackServiceInvoke = false;
                boolean isStubServiceInvoke = false;
                Invocation inv = (Invocation) message;
                int port = channel.getLocalAddress().getPort();
                String path = inv.getAttachments().get(Constants.PATH_KEY);
                //如果是客户端的回调服务.
                isStubServiceInvoke = Boolean.TRUE.toString().equals(inv.getAttachments().get(RpcConstants.STUB_EVENT_KEY));
                if (isStubServiceInvoke){
                    port = channel.getRemoteAddress().getPort();
                }
                //callback
                isCallBackServiceInvoke = isClientSide(channel) && !isStubServiceInvoke;
                if(isCallBackServiceInvoke){
                    path = inv.getAttachments().get(Constants.PATH_KEY)+"."+inv.getAttachments().get(RpcConstants.CALLBACK_SERVICE_KEY);
                }
                String serviceKey = serviceKey(port, path, inv.getAttachments().get(Constants.VERSION_KEY), inv.getAttachments().get(Constants.GROUP_KEY));
    
                DubboExporter<?> exporter = (DubboExporter<?>) exporterMap.get(serviceKey);
                
                if (exporter == null)
                    throw new RemotingException(channel, "Not found exported service: " + serviceKey + " in " + exporterMap.keySet() + ", may be version or group mismatch " + ", channel: consumer: " + channel.getRemoteAddress() + " --> provider: " + channel.getLocalAddress() + ", message:"+message);
    
                if (isCallBackServiceInvoke){
                    String methodsStr = exporter.getInvoker().getUrl().getParameters().get("methods");
                    boolean hasMethod = false;
                    if (methodsStr == null || methodsStr.indexOf(",") == -1){
                        hasMethod = inv.getMethodName().equals(methodsStr);
                    } else {
                        String[] methods = methodsStr.split(",");
                        for (String method : methods){
                            if (inv.getMethodName().equals(method)){
                                hasMethod = true;
                                break;
                            }
                        }
                    }
                    if (!hasMethod){
                        logger.warn(new IllegalStateException("The methodName "+inv.getMethodName()+" not found in callback service interface ,invoke will be ignored. please update the api interface. url is:" + exporter.getInvoker().getUrl()) +" ,invocation is :"+inv );
                        return null;
                    }
                }
                RpcContext.getContext().setRemoteAddress(channel.getRemoteAddress());
                return exporter.getInvoker().invoke(inv);
            }
            throw new RemotingException(channel, "Unsupported request: " + message == null ? null : (message.getClass().getName() + ": " + message) + ", channel: consumer: " + channel.getRemoteAddress() + " --> provider: " + channel.getLocalAddress());
        }

        @Override
        public void received(Channel channel, Object message) throws RemotingException {
            if (message instanceof Invocation) {
                reply((ExchangeChannel) channel, message);
            } else {
                super.received(channel, message);
            }
        }

        @Override
        public void connected(Channel channel) throws RemotingException {
            invoke(channel, RpcConstants.ON_CONNECT_KEY);
        }

        @Override
        public void disconnected(Channel channel) throws RemotingException {
            if(logger.isInfoEnabled()){
                logger.info("disconected from "+ channel.getRemoteAddress() + ",url:" + channel.getUrl());
            }
            invoke(channel, RpcConstants.ON_DISCONNECT_KEY);
        }
        
        private void invoke(Channel channel, String methodKey) {
            Invocation invocation = createInvocation(channel, channel.getUrl(), methodKey);
            if (invocation != null) {
                try {
                    received(channel, invocation);
                } catch (Throwable t) {
                    logger.warn("Failed to invoke event method " + invocation.getMethodName() + "(), cause: " + t.getMessage(), t);
                }
            }
        }
        
        private Invocation createInvocation(Channel channel, URL url, String methodKey) {
            String method = url.getParameter(methodKey);
            if (method == null || method.length() == 0) {
                return null;
            }
            RpcInvocation invocation = new RpcInvocation(method, new Class<?>[0], new Object[0]);
            invocation.setAttachment(Constants.PATH_KEY, url.getPath());
            invocation.setAttachment(Constants.GROUP_KEY, url.getParameter(Constants.GROUP_KEY));
            invocation.setAttachment(Constants.INTERFACE_KEY, url.getParameter(Constants.INTERFACE_KEY));
            invocation.setAttachment(Constants.VERSION_KEY, url.getParameter(Constants.VERSION_KEY));
            if (url.getParameter(RpcConstants.STUB_EVENT_KEY, false)){
                invocation.setAttachment(RpcConstants.STUB_EVENT_KEY, Boolean.TRUE.toString());
            }
            return invocation;
        }
    };
    
    private static DubboProtocol INSTANCE;

    public DubboProtocol() {
        INSTANCE = this;
    }
    
    public static DubboProtocol getDubboProtocol() {
        if (INSTANCE == null) {
            ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(DubboProtocol.NAME); // load
        }
        return INSTANCE;
    }

    public Collection<ExchangeServer> getServers() {
        return Collections.unmodifiableCollection(serverMap.values());
    }

    public Collection<Exporter<?>> getExporters() {
        return Collections.unmodifiableCollection(exporterMap.values());
    }

    public Collection<Invoker<?>> getInvokers() {
        return Collections.unmodifiableCollection(invokers);
    }

    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        URL url = invoker.getUrl().addParameterIfAbsent(Constants.DOWNSTREAM_CODEC_KEY, DubboCodec.NAME);
        // find server.
        String key = url.getAddress();
        //client 也可以暴露一个只有server可以调用的服务。
        boolean isServer = url.getParameter(RpcConstants.IS_SERVER_KEY,true);
        if (isServer && ! serverMap.containsKey(key)) {
            serverMap.put(key, initServer(url));
        }
        // export service.
        key = serviceKey(url);
        DubboExporter<T> exporter = new DubboExporter<T>(invoker, key, exporterMap);
        exporterMap.put(key, exporter);
        
        //export an stub service for dispaching event
        Boolean isStubSupportEvent = url.getParameter(RpcConstants.STUB_EVENT_KEY,RpcConstants.DEFAULT_STUB_EVENT);
        Boolean isCallbackservice = url.getParameter(RpcConstants.IS_CALLBACK_SERVICE, false);
        if (isStubSupportEvent && !isCallbackservice){
            String stubServiceMethods = url.getParameter(RpcConstants.STUB_EVENT_METHODS_KEY);
            if (stubServiceMethods == null || stubServiceMethods.length() == 0 ){
                if (logger.isWarnEnabled()){
                    logger.warn( new IllegalStateException("consumer ["+url.getParameter(Constants.INTERFACE_KEY)+"], has set stubproxy support event ,but no stub methods founded."));
                }
            } else {
                stubServiceMethodsMap.put(url.getServiceKey(), stubServiceMethods);
            }
        }
        return exporter;
    }
    
    private ExchangeServer initServer(URL url) {
        String str = url.getParameter(Constants.SERVER_KEY, Constants.DEFAULT_REMOTING_SERVER);

        if (str != null && str.length() > 0 && ! ExtensionLoader.getExtensionLoader(Transporter.class).hasExtension(str))
            throw new RpcException("Unsupported server type: " + str);

        url = url.addParameter(Constants.CODEC_KEY, Version.isCompatibleVersion() ? COMPATIBLE_CODEC_NAME : DubboCodec.NAME);
        ExchangeServer server;
        try {
            server = Exchangers.bind(url, requestHandler);
        } catch (RemotingException e) {
            throw new RpcException("Fail to start server(url: " + url + ") " + e.getMessage(), e);
        }
        str = url.getParameter(Constants.CLIENT_KEY);
        if (str != null && str.length() > 0) {
            Set<String> supportedTypes = ExtensionLoader.getExtensionLoader(Transporter.class).getSupportedExtensions();
            if (!supportedTypes.contains(str)) {
                throw new RpcException("Unsupported client type: " + str);
            }
        }
        return server;
    }

    public <T> Invoker<T> refer(Class<T> serviceType, URL url) throws RpcException {
        // find client.
        int channels = url.getPositiveParameter(Constants.CONNECTIONS_KEY, 1);
        ExchangeClient[] clients = new ExchangeClient[channels];
        if ( channels == 1){
            clients[0]  = getOrInitClient(url);
        } else {
            for (int i = 0; i < clients.length; i++) {
                //多连接的情况下,不能共享连接
                clients[i] = initClient(url);
            }
        }
        // create rpc invoker.
        DubboInvoker<T> invoker = new DubboInvoker<T>(serviceType, url, clients);
        invokers.add(invoker);
        return invoker;
    }
    
    private ExchangeClient getOrInitClient(URL url){
        boolean connect_per_service = url.getParameter(RpcConstants.SERVICE_SHARECONNECT_KEY, RpcConstants.SERVICE_SHARECONNECT_DEFAULT);
        
        if (connect_per_service ){
            return initClient(url);
        }
        String key = url.getAddress();
        ExchangeClient client = clientMap.get(key);
        if ( client != null ){
            if ( !client.isClosed()){
                return new ReferenceCountExchangeClient(client, clientMap);
            } else {
                try {
                    logger.warn(new IllegalStateException("client is closed,but stay in clientmap .client :"+ client));
                    clientMap.remove(key);
                    client.close();
                } catch (Throwable e) {
                    logger.warn(e);
                }
            }
        }
        client = initClient(url);
        clientMap.put(key, client);
        
        lock.lock();
        try {
            AtomicInteger count = (AtomicInteger)client.getAttribute(RpcConstants.CLIENT_REFERENCE_COUNT);
            if ((count == null)){
                client.setAttribute(RpcConstants.CLIENT_REFERENCE_COUNT, new AtomicInteger(1));
            } else {
                count.getAndIncrement();
            }
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        } finally{
            lock.unlock();
        }
        return new ReferenceCountExchangeClient(client, clientMap); 
    }

    private ExchangeClient initClient(URL url) {
        // client type setting.
        String str = url.getParameter(Constants.CLIENT_KEY, url.getParameter(Constants.SERVER_KEY, Constants.DEFAULT_REMOTING_CLIENT));

        String version = url.getParameter(Constants.DUBBO_VERSION_KEY);
        boolean compatible = (version != null && version.startsWith("1.0."));
        url = url.addParameter(Constants.CODEC_KEY, Version.isCompatibleVersion() && compatible ? COMPATIBLE_CODEC_NAME : DubboCodec.NAME);
        
        // BIO存在严重性能问题，暂时不允许使用
        if (str != null && str.length() > 0 && ! ExtensionLoader.getExtensionLoader(Transporter.class).hasExtension(str)) {
            throw new RpcException("Unsupported client type: " + str + "," +
                    " supported client type is " + StringUtils.join(ExtensionLoader.getExtensionLoader(Transporter.class).getSupportedExtensions(), " "));
        }
        
        ExchangeClient client ;
        try {
            //设置连接应该是lazy的 
            if (url.getParameter(RpcConstants.LAZY_CONNECT_KEY, false)){
                client = new LazyConnectExchangeClient(url ,requestHandler);
            } else {
                client = Exchangers.connect(url ,requestHandler);
            }
        } catch (RemotingException e) {
            throw new RpcException("Fail to create remoting client for service(" + url
                    + "): " + e.getMessage(), e);
        }
        return client;
    }

    public void destroy() {
        super.destroy();
        for (String key : new ArrayList<String>(serverMap.keySet())) {
            ExchangeServer server = serverMap.remove(key);
            if (server != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Close dubbo server: " + server.getLocalAddress());
                    }
                    server.close(getServerShutdownTimeout());
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }
        for (String key : new ArrayList<String>(clientMap.keySet())) {
            ExchangeClient client = clientMap.remove(key);
            if (client != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Close dubbo connect: " + client.getLocalAddress() + "-->" + client.getRemoteAddress());
                    }
                    client.close();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }
        stubServiceMethodsMap.clear();
    }
}