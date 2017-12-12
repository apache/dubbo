package com.alibaba.dubbo.qos.textui;

import org.apache.commons.lang3.StringUtils;

import java.util.Scanner;

/**
 * KV排版控件
 * Created by oldmanpushcart@gmail.com on 15/5/9.
 */
public class TKv implements TComponent {

    private final TTable tTable;

    public TKv() {
        this.tTable = new TTable(new TTable.ColumnDefine[]{
                new TTable.ColumnDefine(TTable.Align.RIGHT),
                new TTable.ColumnDefine(TTable.Align.RIGHT),
                new TTable.ColumnDefine(TTable.Align.LEFT)
        })
                .padding(0);
        this.tTable.getBorder().set(TTable.Border.BORDER_NON);
    }

    public TKv(TTable.ColumnDefine keyColumnDefine, TTable.ColumnDefine valueColumnDefine) {
        this.tTable = new TTable(new TTable.ColumnDefine[]{
                keyColumnDefine,
                new TTable.ColumnDefine(TTable.Align.RIGHT),
                valueColumnDefine
        })
                .padding(0);
        this.tTable.getBorder().set(TTable.Border.BORDER_NON);
    }

    public TKv add(final Object key, final Object value) {
        tTable.addRow(key, " : ", value);
        return this;
    }

    @Override
    public String rendering() {
        return filterEmptyLine(tTable.rendering());
    }


    /*
     * 出现多余的空行的原因是，KVview在输出时，会补全空格到最长的长度。所以在"yyyyy”后面会多出来很多的空格。
     * 再经过TableView的固定列处理，多余的空格就会在一行里放不下，输出成两行（第二行前面是空格）
     *
     * @see https://github.com/oldmanpushcart/greys-anatomy/issues/82
     */
    private String filterEmptyLine(String content) {
        final StringBuilder sb = new StringBuilder();
        Scanner scanner = null;
        try {
            scanner = new Scanner(content);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line != null) {
                    //清理一行后面多余的空格
                    line = StringUtils.stripEnd(line, " ");
                    if (line.isEmpty()) {
                        line = " ";
                    }
                }
                sb.append(line).append('\n');
            }
        } finally {
            if (null != scanner) {
                scanner.close();
            }
        }

        return sb.toString();
    }
}
