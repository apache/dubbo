package org.apache.dubbo.qos.command.impl;

import org.apache.dubbo.qos.command.CommandContext;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class HelpTest {
    @Test
    public void testMainHelp() throws Exception {
        Help help = new Help();
        String output = help.execute(Mockito.mock(CommandContext.class), null);
        assertThat(output, containsString("greeting"));
        assertThat(output, containsString("help"));
        assertThat(output, containsString("ls"));
        assertThat(output, containsString("online"));
        assertThat(output, containsString("offline"));
        assertThat(output, containsString("quit"));
    }

    @Test
    public void testGreeting() throws Exception {
        Help help = new Help();
        String output = help.execute(Mockito.mock(CommandContext.class), new String[]{"greeting"});
        assertThat(output, containsString("COMMAND NAME"));
        assertThat(output, containsString("greeting"));
        assertThat(output, containsString("EXAMPLE"));
        assertThat(output, containsString("greeting dubbo"));
    }
}
