package org.apache.dubbo.rpc.protocol.dubbo.managemode;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.threadpool.ThreadlessExecutor;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture;
import org.apache.dubbo.remoting.transport.dispatcher.WrappedChannelHandler;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.fail;

class WrappedChannelHandlerTest {
    WrappedChannelHandler handler;
    URL url = URL.valueOf("dubbo://10.20.30.40:1234");
//    URL url = URL.valueOf("dubbo://10.20.30.40:1234");

    @BeforeEach
    public void setUp() throws Exception {
        url = url.setScopeModel(ApplicationModel.defaultModel());
//        addExtension("ext9", Ext9EmptyImpl.class);
        handler = new WrappedChannelHandler(new BizChannelHandler(true), url);
    }

    @Test
    void test_Execute_Error() throws RemotingException {

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

    @Test
    void testConnectBizError() throws RemotingException {
        Assertions.assertThrows(RemotingException.class, () -> handler.connected(new MockedChannel()));
    }

    @Test
    void testDisconnectBizError() throws RemotingException {
        Assertions.assertThrows(RemotingException.class, () -> handler.disconnected(new MockedChannel()));
    }

    @Test
    void testMessageReceivedBizError() throws RemotingException {
        Assertions.assertThrows(RemotingException.class, () -> handler.received(new MockedChannel(), ""));
    }

    @Test
    void testCaughtBizError() throws RemotingException {
        try {
            handler.caught(new MockedChannel(), new BizException());
            fail();
        } catch (Exception e) {
            Assertions.assertEquals(BizException.class, e.getCause().getClass());
        }
    }

    @Test
    void testGetExecutor() {
        ExecutorService sharedExecutorService = handler.getSharedExecutorService();
        Assertions.assertNotNull(sharedExecutorService);
        ExecutorService preferredExecutorService = handler.getPreferredExecutorService(new Object());
        Assertions.assertEquals(preferredExecutorService, sharedExecutorService);

        Response response = new Response(10);
        preferredExecutorService = handler.getPreferredExecutorService(response);
        Assertions.assertEquals(preferredExecutorService, sharedExecutorService);

        Channel channel = new MockedChannel();
        Request request = new Request(10);
        ExecutorService sharedExecutor = ExtensionLoader.getExtensionLoader(ExecutorRepository.class)
            .getDefaultExtension().createExecutorIfAbsent(url);

        DefaultFuture future = DefaultFuture.newFuture(channel, request, 1000, null);
        preferredExecutorService = handler.getPreferredExecutorService(response);
        Assertions.assertEquals(preferredExecutorService, sharedExecutor);
        future.cancel();

        ThreadlessExecutor executor = new ThreadlessExecutor();
        future = DefaultFuture.newFuture(channel, request, 1000, executor);
        preferredExecutorService = handler.getPreferredExecutorService(response);
        Assertions.assertEquals(preferredExecutorService, executor);
        future.cancel();
    }

    class BizChannelHandler extends MockedChannelHandler {
        private boolean invokeWithBizError;

        public BizChannelHandler(boolean invokeWithBizError) {
            super();
            this.invokeWithBizError = invokeWithBizError;
        }

        public BizChannelHandler() {
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
