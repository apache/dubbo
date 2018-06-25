package org.apache.dubbo.qos.protocol;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.server.Server;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class QosProtocolWrapperTest {
    private URL url = Mockito.mock(URL.class);
    private Invoker invoker = mock(Invoker.class);
    private Protocol protocol = mock(Protocol.class);
    private QosProtocolWrapper wrapper = new QosProtocolWrapper(protocol);
    private Server server = Server.getInstance();

    @Before
    public void setUp() throws Exception {
        when(url.getParameter(Constants.QOS_ENABLE, "true")).thenReturn("true");
        when(url.getParameter(Constants.QOS_PORT, "22222")).thenReturn("12345");
        when(url.getParameter(Constants.ACCEPT_FOREIGN_IP, "true")).thenReturn("false");
        when(invoker.getUrl()).thenReturn(url);
        when(url.getProtocol()).thenReturn(Constants.REGISTRY_PROTOCOL);
    }

    @After
    public void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stop();
        }
    }

    @Test
    public void testExport() throws Exception {
        wrapper.export(invoker);
        assertThat(server.isStarted(), is(true));
        assertThat(server.getPort(), is(12345));
        assertThat(server.isAcceptForeignIp(), is(false));
        verify(protocol).export(invoker);
    }

    @Test
    public void testRefer() throws Exception {
        wrapper.refer(BaseCommand.class, url);
        assertThat(server.isStarted(), is(true));
        assertThat(server.getPort(), is(12345));
        assertThat(server.isAcceptForeignIp(), is(false));
        verify(protocol).refer(BaseCommand.class, url);
    }
}
