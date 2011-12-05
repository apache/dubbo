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
package com.alibaba.dubbo.remoting.transport.netty;

import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Test;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.utils.DubboAppender;
import com.alibaba.dubbo.common.utils.LogUtil;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.Client;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.Server;
import com.alibaba.dubbo.remoting.exchange.Exchangers;
import com.alibaba.dubbo.remoting.exchange.support.ExchangeHandlerAdapter;

/**
 * 客户端重连测试
 * @author chao.liuc
 *
 */
public class ClientReconnectTest {
    @Test
    public void testReconnect() throws RemotingException, InterruptedException{
        {
            int port = NetUtils.getAvailablePort();
            Client client = startClient(port, 200);
            Assert.assertEquals(false, client.isConnected());
            Server server = startServer(port);
            for (int i = 0; i < 100 && ! client.isConnected(); i++) {
                Thread.sleep(10);
            }
            Assert.assertEquals(true, client.isConnected());
            client.close(2000);
            server.close(2000);
        }
        {
            int port = NetUtils.getAvailablePort();
            Client client = startClient(port, 20000);
            Assert.assertEquals(false, client.isConnected());
            Server server = startServer(port);
            for(int i=0;i<5;i++){
                Thread.sleep(200);
            }
            Assert.assertEquals(false, client.isConnected());
            client.close(2000);
            server.close(2000);
        }
    }
    
    /**
     * 重连日志的校验
     */
    @Test
    public void testReconnectNoLog() throws RemotingException, InterruptedException{
        int port = NetUtils.getAvailablePort();
        DubboAppender.doStart();
        String url = "exchange://127.0.0.2:"+port + "/client.reconnect.test?check=false&"
        +Constants.RECONNECT_KEY+"="+1 ; //1ms reconnect,保证有足够频率的重连
        try{
            Exchangers.connect(url);
        }catch (Exception e) {
            //do nothing
        }
        Thread.sleep(1000);//重连线程的运行
        Assert.assertEquals("no error message ", 0 , LogUtil.findMessage(Level.ERROR, "client reconnect to "));
        Assert.assertEquals("no warn message ", 0 , LogUtil.findMessage(Level.WARN, "client reconnect to "));
        DubboAppender.doStop();
    }
  
    /**
     * 重连日志的校验，不能一直抛出error日志.
     */
    @Test
    public void testReconnectErrorLog() throws RemotingException, InterruptedException{
        int port = NetUtils.getAvailablePort();
        DubboAppender.doStart();
        String url = "exchange://127.0.0.3:"+port + "/client.reconnect.test?check=false&"
        +Constants.RECONNECT_KEY+"="+1 + //1ms reconnect,保证有足够频率的重连
        "&"+Constants.SHUTDOWN_TIMEOUT_KEY+ "=0";//shutdown时间足够短，确保error日志输出
        try{
            Exchangers.connect(url);
        }catch (Exception e) {
            //do nothing
        }
        Thread.sleep(1000);//重连线程的运行
        Assert.assertEquals("only one error message ", 1 , LogUtil.findMessage(Level.ERROR, "client reconnect to "));
        Assert.assertEquals("no warn message ", 0 , LogUtil.findMessage(Level.WARN, "client reconnect to "));
        DubboAppender.doStop();
    }
    
    /**
     * 重连日志的校验
     */
    @Test
    public void testReconnectWaringLog() throws RemotingException, InterruptedException{
        int port = NetUtils.getAvailablePort();
        DubboAppender.doStart();
        String url = "exchange://127.0.0.4:"+port + "/client.reconnect.test?check=false&"
        +Constants.RECONNECT_KEY+"="+1 //1ms reconnect,保证有足够频率的重连
        +"&"+Constants.SHUTDOWN_TIMEOUT_KEY+ "=1"//shutdown时间足够短，确保error日志输出
        +"&reconnect.waring.period=100";//每隔多少warning记录一次
        try{
            Exchangers.connect(url);
        }catch (Exception e) {
            //do nothing
        }
        Thread.sleep(1000);//重连线程的运行
        int count =  LogUtil.findMessage(Level.WARN, "client reconnect to ") ;
        Assert.assertTrue("warning message count must > 2, real :"+count, count> 2);
        DubboAppender.doStop();
    }
    
    public Client startClient(int port , int reconnectPeriod) throws RemotingException{
        final String url = "exchange://127.0.0.1:"+port + "/client.reconnect.test?check=false&"+Constants.RECONNECT_KEY+"="+reconnectPeriod;
        return Exchangers.connect(url);
    }
    
    public Server startServer(int port) throws RemotingException{
        final String url = "exchange://127.0.0.1:"+port +"/client.reconnect.test";
        return Exchangers.bind(url, new HandlerAdapter());
    }
    
    static class HandlerAdapter extends ExchangeHandlerAdapter{
        public void connected(Channel channel) throws RemotingException {
        }
        public void disconnected(Channel channel) throws RemotingException {
        }
        public void caught(Channel channel, Throwable exception) throws RemotingException {
        }
    }
}