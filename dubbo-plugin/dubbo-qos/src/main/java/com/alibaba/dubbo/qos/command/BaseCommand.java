package com.alibaba.dubbo.qos.command;

import com.alibaba.dubbo.common.extension.SPI;

/**
 * @author qinliujie
 * @date 2017/11/21
 */
@SPI
public interface BaseCommand {
    String execute(CommandContext commandContext,String[] args);
}
