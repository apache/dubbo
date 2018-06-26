package org.apache.dubbo.qos.command.util;

import org.apache.dubbo.qos.command.GreetingCommand;
import org.apache.dubbo.qos.command.impl.Help;
import org.apache.dubbo.qos.command.impl.Ls;
import org.apache.dubbo.qos.command.impl.Offline;
import org.apache.dubbo.qos.command.impl.Online;
import org.apache.dubbo.qos.command.impl.Quit;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CommandHelperTest {
    @Test
    public void testHasCommand() throws Exception {
        assertTrue(CommandHelper.hasCommand("greeting"));
        assertFalse(CommandHelper.hasCommand("not-exiting"));
    }

    @Test
    public void testGetAllCommandClass() throws Exception {
        List<Class<?>> classes = CommandHelper.getAllCommandClass();
        assertThat(classes, containsInAnyOrder(GreetingCommand.class, Help.class, Ls.class, Offline.class, Online.class, Quit.class));
    }

    @Test
    public void testGetCommandClass() throws Exception {
        assertThat(CommandHelper.getCommandClass("greeting"), equalTo(GreetingCommand.class));
        assertNull(CommandHelper.getCommandClass("not-exiting"));
    }
}
