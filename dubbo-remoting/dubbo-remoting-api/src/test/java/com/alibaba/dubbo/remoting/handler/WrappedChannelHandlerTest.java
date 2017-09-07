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
package com.alibaba.dubbo.remoting.handler;


import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.transport.dispatcher.WrappedChannelHandler;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.fail;

public class WrappedChannelHandlerTest {
    WrappedChannelHandler handler;
    URL url = URL.valueOf("test://10.20.30.40:1234");

    @Before
    public void setUp() throws Exception {
        handler = new WrappedChannelHandler(new BizChannelHander(true), url);
    }

    @Test
    public void test_Execute_Error() throws RemotingException {

    }


    protected Object getField(Object obj, String fieldName, int parentdepth) {
        try {
            Class<?> clazz = obj.getClass();
            Field field = null;
            for (int i = 0; i <= parentdepth && field == null; i++) {
                Field[] fields = clazz.getDeclaredFields();
                for (Field f : fields) {
                    if (f.getName().equals(fieldName)) {
                        field = f;
                        break;
                    }
                }
                clazz = clazz.getSuperclass();
            }
            if (field != null) {
                field.setAccessible(true);
                return field.get(obj);
            } else {
                throw new NoSuchFieldException();
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test(expected = RemotingException.class)
    public void test_Connect_Biz_Error() throws RemotingException {
        handler.connected(new MockedChannel());
    }

    ;

    @Test(expected = RemotingException.class)
    public void test_Disconnect_Biz_Error() throws RemotingException {
        handler.disconnected(new MockedChannel());
    }

    @Test(expected = RemotingException.class)
    public void test_MessageReceived_Biz_Error() throws RemotingException {
        handler.received(new MockedChannel(), "");
    }

    @Test
    public void test_Caught_Biz_Error() throws RemotingException {
        try {
            handler.caught(new MockedChannel(), new BizException());
            fail();
        } catch (Exception e) {
            Assert.assertEquals(BizException.class, e.getCause().getClass());
        }
    }

    class BizChannelHander extends MockedChannelHandler {
        private boolean invokeWithBizError;

        public BizChannelHander(boolean invokeWithBizError) {
            super();
            this.invokeWithBizError = invokeWithBizError;
        }

        public BizChannelHander() {
            super();
        }

        @Override
        public void connected(Channel channel) throws RemotingException {
            if (invokeWithBizError) {
                throw new RemotingException(channel, "test connect biz error");
            }
            sleep(20);
        }

        @Override
        public void disconnected(Channel channel) throws RemotingException {
            if (invokeWithBizError) {
                throw new RemotingException(channel, "test disconnect biz error");
            }
            sleep(20);
        }

        @Override
        public void received(Channel channel, Object message) throws RemotingException {
            if (invokeWithBizError) {
                throw new RemotingException(channel, "test received biz error");
            }
            sleep(20);
        }
    }

    class BizException extends RuntimeException {
        private static final long serialVersionUID = -7541893754900723624L;
    }
}