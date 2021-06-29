package org.apache.dubbo.qos.command.impl;

import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.qos.command.annotation.Cmd;

import java.util.Arrays;

@Cmd(name = "pwd", summary = "Print working default service.", example = {
    "pwd"
})
public class PwdTelnet implements BaseCommand {
    @Override
    public String execute(CommandContext commandContext, String[] args) {
        if (args.length > 0) {
            return "Unsupported parameter " + Arrays.toString(args) + " for pwd.";
        }
        String service = commandContext.getRemote().attr(ChangeTelnet.SERVICE_KEY).get();
        StringBuilder buf = new StringBuilder();
        if (service == null || service.length() == 0) {
            buf.append("/");
        } else {
            buf.append(service);
        }
        return buf.toString();
    }
}
