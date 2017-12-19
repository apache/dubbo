package com.alibaba.dubbo.qos.command.util;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.qos.command.BaseCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author qinliujie
 * @date 2017/11/21
 */
public class CommandHelper {
    public static boolean hasCommand(String commandName) {

        BaseCommand command = null;
        try {
            command = ExtensionLoader.getExtensionLoader(BaseCommand.class).getExtension(commandName);
        } catch (Throwable throwable) {
            return false;
        }

        return command != null;

    }

    public static List<Class<?>> getAllCommandClass(){
        final Set<String> commandList = ExtensionLoader.getExtensionLoader(BaseCommand.class).getSupportedExtensions();
        final List<Class<?>> classes = new ArrayList<Class<?>>();

        for (String commandName : commandList) {
            BaseCommand command = ExtensionLoader.getExtensionLoader(BaseCommand.class).getExtension(commandName);
            classes.add(command.getClass());
        }

        return classes;
    }


    public static Class<?> getCommandClass(String commandName){
        if (hasCommand(commandName)){
            return ExtensionLoader.getExtensionLoader(BaseCommand.class).getExtension(commandName).getClass();
        }else {
            return null;
        }
    }
}
