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
package com.alibaba.dubbo.governance.web.util;

import java.io.Serializable;

/**
 * TODO Comment of Paginator
 *
 */
public class Paginator implements Serializable, Cloneable {

    private static final long serialVersionUID = 3688506614705500726L;

    // The default number of items per page; default is 10
    int itemsPerPage = 10;

    // Sliding window default size; default: 7
    int sliderSize = 7;

    // The current page.
    int currentPage;

    // The current page.
    String path;

    // total mumber of items
    int totalItems;

    // total number of pages
    int totalPage;

    /**
     * The most simple paging constructor.
     *
     * @param currentPage
     * @param totalItems
     * @param path
     */
    public Paginator(int currentPage, int totalItems, String path) {
        initPagination(currentPage, totalItems, 0, 0, path);
    }

    public Paginator(String currentPage, int totalItems, String path) {
        int currentPageTemp = 1;
        if (!(currentPage == null || currentPage.equals(""))) {
            currentPageTemp = Integer.parseInt(currentPage);
        }
        initPagination(currentPageTemp, totalItems, 0, 0, path);
    }

    /**
     * Complete paging constructor.
     *
     * @param currentPageT
     * @param totalItemsT
     * @param sliderSizeT
     * @param itemsPerPageT
     * @param path
     */
    public void initPagination(int currentPageT, int totalItemsT, int sliderSizeT, int itemsPerPageT, String path) {
        this.totalItems = (totalItemsT > 0) ? totalItemsT : 0;
        this.sliderSize = (sliderSizeT > 0) ? sliderSizeT : sliderSize;
        this.itemsPerPage = (itemsPerPageT > 0) ? itemsPerPageT : itemsPerPage;
        this.totalPage = totalItems / itemsPerPage + (totalItems % itemsPerPage == 0 ? 0 : 1);
        this.currentPage = (currentPageT > 0) ? currentPageT : 1;
        this.currentPage = currentPage < totalPage ? currentPage : totalPage;
        this.currentPage = (currentPage == 0) ? 1 : currentPage;
        this.path = path;
    }

    public int getItemsPerPage() {
        return this.itemsPerPage;
    }

    /**
     * Get a sliding window of fixed size, and the current page should lie in the middle of the sliding window.
     * For example: a total of 13 pages, the current page is page 5, a size of 5 sliding window should consists of 3,4,5,6,7, page 5 is placed in the middle. If the current page is 12, the return page number should be 9, 10, 11, 12, 13.
     *
     * @return An array containing page numbers, or an empty array if the specified sliding window size is less than 1 or the total number of pages is zero.
     */
    public int[] getSlider() {
        int width = sliderSize;
        if ((totalItems < 1)) {
            return new int[0];

        } else {
            if (width > totalPage) {
                width = totalPage;
            }

            int[] slider = new int[width];

            int startPage = currentPage - ((width - 1) / 2);

            if (startPage < 1) {
                startPage = 1;
            }

            if (((startPage + width) - 1) > totalPage) {
                startPage = totalPage - width + 1;
            }

            for (int i = 0; i < width; i++) {
                slider[i] = startPage + i;
            }
            return slider;
        }
    }

    /**
     * Construction pagination toolbar
     */
    public String getPaginatorBar() {

        StringBuffer str = new StringBuffer("<div class=\"page\">");
        str.append("<script type=\"text/javascript\">function gotoPage(page){window.location.href=\"/" + path
                + "/pages/\" + page;}</script>");

        // generate flip section
        // The total number of records
        str.append("total items: " + this.totalItems + "&nbsp;&nbsp;");

        // 2. Pages: current page / total pages
        str.append("page " + this.currentPage + " of " + this.totalPage + "nbsp;&nbsp;");

        // 3. Home, Previous
        if (this.currentPage > 1) {
            str.append("<a class=\"prev\" href=\"#\" onclick=\"gotoPage(1);\">Home</a>");
            str.append("<a class=\"prev\" href=\"#\" onclick=\"gotoPage(" + (this.currentPage - 1) + ");\">Previous</a>");
        } else {
            str.append("<a class=\"prev\" href=\"#\">Home</a>");
            str.append("<a class=\"prev\" href=\"#\">Previous</a>");
        }

        // 4. Activity block
        int[] slider = getSlider();
        for (int i = 0; i < slider.length; i++) {
            if (slider[i] == this.currentPage) {
                str.append("<a class=\"num current_num\" href=\"#\">");
            } else {
                str.append("<a class=\"num\" href=\"#\" onclick=\"gotoPage(" + slider[i] + ");\">");
            }
            str.append(slider[i] + "</a>");
        }

        // 5. Next page
        if (this.currentPage < this.totalPage) {
            str.append("<a class=\"prev\" href=\"#\" onclick=\"gotoPage(" + (this.currentPage + 1) + ");\">");
        } else {
            str.append("<a class=\"prev\" href=\"#\">");
        }
        str.append("Next</a>&nbsp;&nbsp;");

        // 6. Jump section
        str.append("jump to page ");
        str.append("<SELECT size=1 onchange=\"gotoPage(this.value);\">");
        for (int i = 1; i < this.totalPage + 1; i++) {
            if (i == this.currentPage) {
                str.append("<OPTION value=" + i + " selected>" + i + "</OPTION>");
            } else {
                str.append("<OPTION value=" + i + ">" + i + "</OPTION>");
            }
        }
        str.append("</SELECT>");

        // 7. Implicit conditions
        str.append("</div>");
        return str.toString();
    }

    /**
     * Get the initial record
     *
     * @return
     */
    public int getStartIndex() {
        return (this.currentPage - 1) * this.itemsPerPage + 1;
    }

}
