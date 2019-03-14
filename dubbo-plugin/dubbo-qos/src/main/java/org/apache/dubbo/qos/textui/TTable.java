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
package org.apache.dubbo.qos.textui;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.String.format;
import static org.apache.dubbo.common.utils.StringUtils.EMPTY;
import static org.apache.dubbo.common.utils.StringUtils.length;
import static org.apache.dubbo.common.utils.StringUtils.repeat;
import static org.apache.dubbo.common.utils.StringUtils.replace;

/**
 * Table
 */
public class TTable implements TComponent {

    // column definition
    private final ColumnDefine[] columnDefineArray;

    // border
    private final Border border = new Border();

    // padding
    private int padding;

    public TTable(ColumnDefine[] columnDefineArray) {
        this.columnDefineArray = null == columnDefineArray
                ? new ColumnDefine[0]
                : columnDefineArray;
    }

    public TTable(int columnNum) {
        this.columnDefineArray = new ColumnDefine[columnNum];
        for (int index = 0; index < this.columnDefineArray.length; index++) {
            columnDefineArray[index] = new ColumnDefine();
        }
    }


    @Override
    public String rendering() {
        final StringBuilder tableSB = new StringBuilder();

        // process width cache
        final int[] widthCacheArray = new int[getColumnCount()];
        for (int index = 0; index < widthCacheArray.length; index++) {
            widthCacheArray[index] = abs(columnDefineArray[index].getWidth());
        }

        final int rowCount = getRowCount();
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {

            final boolean isFirstRow = rowIndex == 0;
            final boolean isLastRow = rowIndex == rowCount - 1;

            // print first separation line
            if (isFirstRow
                    && border.has(Border.BORDER_OUTER_TOP)) {
                tableSB.append(drawSeparationLine(widthCacheArray)).append(System.lineSeparator());
            }

            // print inner separation lines
            if (!isFirstRow
                    && border.has(Border.BORDER_INNER_H)) {
                tableSB.append(drawSeparationLine(widthCacheArray)).append(System.lineSeparator());
            }

            // draw one line
            tableSB.append(drawRow(widthCacheArray, rowIndex));


            // print ending separation line
            if (isLastRow
                    && border.has(Border.BORDER_OUTER_BOTTOM)) {
                tableSB.append(drawSeparationLine(widthCacheArray)).append(System.lineSeparator());
            }

        }


        return tableSB.toString();
    }


    private String drawRow(int[] widthCacheArray, int rowIndex) {

        final StringBuilder rowSB = new StringBuilder();
        final Scanner[] scannerArray = new Scanner[getColumnCount()];
        try {
            boolean hasNextLine;
            do {

                hasNextLine = false;
                final StringBuilder segmentSB = new StringBuilder();

                for (int colIndex = 0; colIndex < getColumnCount(); colIndex++) {


                    final int width = widthCacheArray[colIndex];
                    final boolean isFirstColOfRow = colIndex == 0;
                    final boolean isLastColOfRow = colIndex == widthCacheArray.length - 1;

                    final String borderChar;
                    if (isFirstColOfRow
                            && border.has(Border.BORDER_OUTER_LEFT)) {
                        borderChar = "|";
                    } else if (!isFirstColOfRow
                            && border.has(Border.BORDER_INNER_V)) {
                        borderChar = "|";
                    } else {
                        borderChar = EMPTY;
                    }


                    if (null == scannerArray[colIndex]) {
                        scannerArray[colIndex] = new Scanner(
                                new StringReader(wrap(getData(rowIndex, columnDefineArray[colIndex]), width)));
                    }
                    final Scanner scanner = scannerArray[colIndex];

                    final String data;
                    if (scanner.hasNextLine()) {
                        data = scanner.nextLine();
                        hasNextLine = true;
                    } else {
                        data = EMPTY;
                    }

                    if (width > 0) {
                        final ColumnDefine columnDefine = columnDefineArray[colIndex];
                        final String dataFormat = getDataFormat(columnDefine, width, data);
                        final String paddingChar = repeat(" ", padding);
                        segmentSB.append(format(borderChar + paddingChar + dataFormat + paddingChar, data));
                    }

                    if (isLastColOfRow) {
                        if (border.has(Border.BORDER_OUTER_RIGHT)) {
                            segmentSB.append("|");
                        }
                        segmentSB.append(System.lineSeparator());
                    }

                }

                if (hasNextLine) {
                    rowSB.append(segmentSB);
                }

            } while (hasNextLine);

            return rowSB.toString();
        } finally {
            for (Scanner scanner : scannerArray) {
                if (null != scanner) {
                    scanner.close();
                }
            }
        }

    }

    private String getData(int rowIndex, ColumnDefine columnDefine) {
        return columnDefine.getRowCount() <= rowIndex
                ? EMPTY
                : columnDefine.rows.get(rowIndex);
    }

    private String getDataFormat(ColumnDefine columnDefine, int width, String data) {
        switch (columnDefine.align) {
            case MIDDLE: {
                final int length = length(data);
                final int diff = width - length;
                final int left = diff / 2;
                return repeat(" ", diff - left) + "%s" + repeat(" ", left);
            }
            case RIGHT: {
                return "%" + width + "s";
            }
            case LEFT:
            default: {
                return "%-" + width + "s";
            }
        }
    }

    /**
     * get row count
     */
    private int getRowCount() {
        int rowCount = 0;
        for (ColumnDefine columnDefine : columnDefineArray) {
            rowCount = max(rowCount, columnDefine.getRowCount());
        }
        return rowCount;
    }

    /**
     * position to last column
     */
    private int indexLastCol(final int[] widthCacheArray) {
        for (int colIndex = widthCacheArray.length - 1; colIndex >= 0; colIndex--) {
            final int width = widthCacheArray[colIndex];
            if (width <= 0) {
                continue;
            }
            return colIndex;
        }
        return 0;
    }

    /**
     * draw separation line
     */
    private String drawSeparationLine(final int[] widthCacheArray) {
        final StringBuilder separationLineSB = new StringBuilder();

        final int lastCol = indexLastCol(widthCacheArray);
        final int colCount = widthCacheArray.length;
        for (int colIndex = 0; colIndex < colCount; colIndex++) {
            final int width = widthCacheArray[colIndex];
            if (width <= 0) {
                continue;
            }

            final boolean isFirstCol = colIndex == 0;
            final boolean isLastCol = colIndex == lastCol;

            if (isFirstCol
                    && border.has(Border.BORDER_OUTER_LEFT)) {
                separationLineSB.append("+");
            }

            if (!isFirstCol
                    && border.has(Border.BORDER_INNER_V)) {
                separationLineSB.append("+");
            }

            separationLineSB.append(repeat("-", width + 2 * padding));

            if (isLastCol
                    && border.has(Border.BORDER_OUTER_RIGHT)) {
                separationLineSB.append("+");
            }

        }
        return separationLineSB.toString();
    }

    /**
     * Add a row
     */
    public TTable addRow(Object... columnDataArray) {

        if (null != columnDataArray) {
            for (int index = 0; index < columnDefineArray.length; index++) {
                final ColumnDefine columnDefine = columnDefineArray[index];
                if (index < columnDataArray.length
                        && null != columnDataArray[index]) {
                    columnDefine.rows.add(replaceTab(columnDataArray[index].toString()));
                } else {
                    columnDefine.rows.add(EMPTY);
                }
            }
        }

        return this;
    }


    /**
     * alignment
     */
    public enum Align {

        /**
         * left-alignment
         */
        LEFT,

        /**
         * right-alignment
         */
        RIGHT,

        /**
         * middle-alignment
         */
        MIDDLE
    }

    /**
     * column definition
     */
    public static class ColumnDefine {

        // column width
        private final int width;

        // whether to auto resize
        private final boolean isAutoResize;

        // alignment
        private final Align align;

        // data rows
        private final List<String> rows = new ArrayList<String>();

        public ColumnDefine(int width, boolean isAutoResize, Align align) {
            this.width = width;
            this.isAutoResize = isAutoResize;
            this.align = align;
        }

        public ColumnDefine(Align align) {
            this(0, true, align);
        }

        public ColumnDefine(int width) {
            this(width, false, Align.LEFT);
        }

        public ColumnDefine(int width, Align align) {
            this(width, false, align);
        }

        public ColumnDefine() {
            this(Align.LEFT);
        }

        /**
         * get current width
         *
         * @return width
         */
        public int getWidth() {

            // if not auto resize, return preset width
            if (!isAutoResize) {
                return width;
            }

            // if it's auto resize, then calculate the possible max width
            int maxWidth = 0;
            for (String data : rows) {
                maxWidth = max(width(data), maxWidth);
            }

            return maxWidth;
        }

        /**
         * get rows for the current column
         *
         * @return current column's rows
         */
        public int getRowCount() {
            return rows.size();
        }

    }

    /**
     * set padding
     *
     * @param padding padding
     */
    public TTable padding(int padding) {
        this.padding = padding;
        return this;
    }

    /**
     * get column count
     *
     * @return column count
     */
    public int getColumnCount() {
        return columnDefineArray.length;
    }


    /**
     * replace tab to four spaces
     *
     * @param string the original string
     * @return the replaced string
     */
    private static String replaceTab(String string) {
        return replace(string, "\t", "    ");
    }

    /**
     * visible width for the given string.
     *
     * for example: "abc\n1234"'s width is 4.
     *
     * @param string the given string
     * @return visible width
     */
    private static int width(String string) {
        int maxWidth = 0;
        try (Scanner scanner = new Scanner(new StringReader(string))) {
            while (scanner.hasNextLine()) {
                maxWidth = max(length(scanner.nextLine()), maxWidth);
            }
        }
        return maxWidth;
    }

    /**
     * get border
     *
     * @return table border
     */
    public Border getBorder() {
        return border;
    }

    /**
     * border style
     */
    public class Border {

        private int borders = BORDER_OUTER | BORDER_INNER;

        /**
         * border outer top
         */
        public static final int BORDER_OUTER_TOP = 1 << 0;

        /**
         * border outer right
         */
        public static final int BORDER_OUTER_RIGHT = 1 << 1;

        /**
         * border outer bottom
         */
        public static final int BORDER_OUTER_BOTTOM = 1 << 2;

        /**
         * border outer left
         */
        public static final int BORDER_OUTER_LEFT = 1 << 3;

        /**
         * inner border: horizon
         */
        public static final int BORDER_INNER_H = 1 << 4;

        /**
         * inner border: vertical
         */
        public static final int BORDER_INNER_V = 1 << 5;

        /**
         * outer border
         */
        public static final int BORDER_OUTER = BORDER_OUTER_TOP | BORDER_OUTER_BOTTOM | BORDER_OUTER_LEFT | BORDER_OUTER_RIGHT;

        /**
         * inner border
         */
        public static final int BORDER_INNER = BORDER_INNER_H | BORDER_INNER_V;

        /**
         * no border
         */
        public static final int BORDER_NON = 0;

        /**
         * whether has one of the specified border styles
         *
         * @param borderArray border styles
         * @return whether has one of the specified border styles
         */
        public boolean has(int... borderArray) {
            if (null == borderArray) {
                return false;
            }
            for (int b : borderArray) {
                if ((this.borders & b) == b) {
                    return true;
                }
            }
            return false;
        }

        /**
         * get border style
         *
         * @return border style
         */
        public int get() {
            return borders;
        }

        /**
         * set border style
         *
         * @param border border style
         * @return this
         */
        public Border set(int border) {
            this.borders = border;
            return this;
        }

        public Border add(int border) {
            return set(get() | border);
        }

        public Border remove(int border) {
            return set(get() ^ border);
        }

    }


    public static String wrap(String string, int width) {
        final StringBuilder sb = new StringBuilder();
        final char[] buffer = string.toCharArray();
        int count = 0;
        for (char c : buffer) {

            if (count == width) {
                count = 0;
                sb.append('\n');
                if (c == '\n') {
                    continue;
                }
            }

            if (c == '\n') {
                count = 0;
            } else {
                count++;
            }

            sb.append(c);

        }
        return sb.toString();
    }

}
