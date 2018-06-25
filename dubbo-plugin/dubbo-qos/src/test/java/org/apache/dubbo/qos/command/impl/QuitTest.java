package org.apache.dubbo.qos.command.impl;

import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.qos.common.QosConstants;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class QuitTest {
    @Test
    public void testExecute() throws Exception {
        Quit quit = new Quit();
        String output = quit.execute(Mockito.mock(CommandContext.class), null);
        assertThat(output, equalTo(QosConstants.CLOSE));
    }
}
