package com.alibaba.dubbo.qos.textui;

import org.junit.Test;

/**
 * @author qinliujie
 * @date 2017/11/17
 */
public class TKvTest {
    @Test
    public void outPutTest() {

        final TKv tKv = new TKv(new TTable.ColumnDefine(TTable.Align.RIGHT), new TTable.ColumnDefine(10, false, TTable.Align.LEFT));
        tKv.add("KEY-1", "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        tKv.add("KEY-2", "1234567890");
        tKv.add("KEY-3", "1234567890");

        final TTable tTable = new TTable(new TTable.ColumnDefine[]{
                new TTable.ColumnDefine(),
                new TTable.ColumnDefine(20, false, TTable.Align.LEFT)
        });

        tTable.addRow("OPTIONS", tKv.rendering());

        System.out.println(tTable.rendering());
    }
}
