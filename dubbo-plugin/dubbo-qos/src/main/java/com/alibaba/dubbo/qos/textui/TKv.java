/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.qos.textui;

import org.apache.commons.lang3.StringUtils;

import java.util.Scanner;

/**
 * KV
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

    private String filterEmptyLine(String content) {
        final StringBuilder sb = new StringBuilder();
        Scanner scanner = null;
        try {
            scanner = new Scanner(content);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line != null) {
                    // remove extra space at line's end
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
