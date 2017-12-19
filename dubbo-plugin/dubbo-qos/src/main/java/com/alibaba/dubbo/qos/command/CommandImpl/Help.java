package com.alibaba.dubbo.qos.command.CommandImpl;

import com.alibaba.dubbo.qos.command.BaseCommand;
import com.alibaba.dubbo.qos.command.CommandContext;
import com.alibaba.dubbo.qos.command.annotation.Cmd;
import com.alibaba.dubbo.qos.command.util.CommandHelper;
import com.alibaba.dubbo.qos.textui.TTable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author qinliujie
 * @date 2017/11/21
 */
@Cmd(name = "help", summary = "help command", example = {
        "help",
        "help online"
})
public class Help implements BaseCommand {
    @Override
    public String execute(CommandContext commandContext, String[] args) {
        if (args != null && args.length > 0) {
            return commandHelp(args[0]);
        } else {
            return mainHelp();
        }

    }


    private String commandHelp(String commandName) {

        if (!CommandHelper.hasCommand(commandName)) {
            return "no such command:" + commandName;
        }

        Class<?> clazz = CommandHelper.getCommandClass(commandName);

        final Cmd cmd = clazz.getAnnotation(Cmd.class);
        final TTable tTable = new TTable(new TTable.ColumnDefine[]{
                new TTable.ColumnDefine(TTable.Align.RIGHT),
                new TTable.ColumnDefine(80, false, TTable.Align.LEFT)
        });

        tTable.addRow("COMMAND NAME", commandName);

        if (null != cmd.example()) {
            tTable.addRow("EXAMPLE", drawExample(cmd));
        }

        return tTable.padding(1).rendering();
    }

    private String drawExample(Cmd cmd) {
        final StringBuilder drawExampleStringBuilder = new StringBuilder();
        for (String example : cmd.example()) {
            drawExampleStringBuilder.append(example).append("\n");
        }
        return drawExampleStringBuilder.toString();
    }

    /*
     * 输出主帮助菜单
     */
    private String mainHelp() {

        final TTable tTable = new TTable(new TTable.ColumnDefine[]{
                new TTable.ColumnDefine(TTable.Align.RIGHT),
                new TTable.ColumnDefine(80, false, TTable.Align.LEFT)
        });

        final List<Class<?>> classes = CommandHelper.getAllCommandClass();

        Collections.sort(classes, new Comparator<Class<?>>() {

            @Override
            public int compare(Class<?> o1, Class<?> o2) {
                final Integer o1s = o1.getAnnotation(Cmd.class).sort();
                final Integer o2s = o2.getAnnotation(Cmd.class).sort();
                return o1s.compareTo(o2s);
            }

        });
        for (Class<?> clazz : classes) {

            if (clazz.isAnnotationPresent(Cmd.class)) {
                final Cmd cmd = clazz.getAnnotation(Cmd.class);
                tTable.addRow(cmd.name(), cmd.summary());
            }

        }

        return tTable.padding(1).rendering();
    }
}
