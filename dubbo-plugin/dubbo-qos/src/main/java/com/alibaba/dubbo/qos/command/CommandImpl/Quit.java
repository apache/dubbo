package com.alibaba.dubbo.qos.command.CommandImpl;

import com.alibaba.dubbo.qos.command.BaseCommand;
import com.alibaba.dubbo.qos.command.CommandContext;
import com.alibaba.dubbo.qos.command.annotation.Cmd;
import com.alibaba.dubbo.qos.common.Constants;

/**
 * @author qinliujie
 * @date 2017/11/21
 */
@Cmd(name = "quit",summary = "quit telnet console")
public class Quit implements BaseCommand {
    @Override
    public String execute(CommandContext commandContext, String[] args) {
        return Constants.CLOSE;
    }
}
