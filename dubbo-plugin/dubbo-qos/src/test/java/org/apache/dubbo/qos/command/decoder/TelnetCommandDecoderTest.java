package org.apache.dubbo.qos.command.decoder;

import org.apache.dubbo.qos.command.CommandContext;
import org.junit.Test;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TelnetCommandDecoderTest {
    @Test
    public void testDecode() throws Exception {
        CommandContext context = TelnetCommandDecoder.decode("test a b");
        assertThat(context.getCommandName(), equalTo("test"));
        assertThat(context.isHttp(), is(false));
        assertThat(context.getArgs(), arrayContaining("a", "b"));
    }
}
