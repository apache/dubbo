package org.apache.dubbo.qos.command.impl;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.DubboShutdownHook;
import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.qos.command.annotation.Cmd;

@Cmd(name = "shutdown", summary = "Shutdown Dubbo Application.", example = {
    "shutdown -t <milliseconds>"
})
public class ShutdownTelnet implements BaseCommand {
    @Override
    public String execute(CommandContext commandContext, String[] args) {

        int sleepMilliseconds = 0;
        if (args != null && args.length > 0) {
            if (args.length == 2 && "-t".equals(args[0]) && StringUtils.isInteger(args[1])) {
                sleepMilliseconds = Integer.parseInt(args[1]);
            } else {
                return "Invalid parameter,please input like shutdown -t 10000";
            }
        }
        long start = System.currentTimeMillis();
        if (sleepMilliseconds > 0) {
            try {
                Thread.sleep(sleepMilliseconds);
            } catch (InterruptedException e) {
                return "Failed to invoke shutdown command, cause: " + e.getMessage();
            }
        }
        StringBuilder buf = new StringBuilder();
        DubboShutdownHook.getDubboShutdownHook().unregister();
        DubboShutdownHook.getDubboShutdownHook().doDestroy();
        long end = System.currentTimeMillis();
        buf.append("Application has shutdown successfully");
        buf.append("\r\nelapsed: ");
        buf.append(end - start);
        buf.append(" ms.");
        return buf.toString();
    }
}
