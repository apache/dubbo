/**
 * Function: 分页封装类，控制分页
 * <p>
 * File Created at 2011-6-10
 * <p>
 * Copyright 2011 Alibaba.com Croporation Limited.
 * All rights reserved.
 */
package com.alibaba.dubbo.governance.web.util;

import java.io.Serializable;

/**
 * TODO Comment of Paginator
 *
 * @author guanghui.shigh
 */
public class Paginator implements Serializable, Cloneable {

    private static final long serialVersionUID = 3688506614705500726L;

    // 每页默认的项数; 默认:10
    int itemsPerPage = 10;

    // 滑动窗口默认的大小; 默认:7
    int sliderSize = 7;

    // 当前页面;
    int currentPage;

    // 当前页面;
    String path;

    // 总记录数
    int totalItems;

    // 总页数
    int totalPage;

    /**
     * 最简化的分页构造器。
     *
     * @param itemsPerPage 每页项数。
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
     * 完整的分页构造器。
     *
     * @param currentPage 。
     * @param totalItems(必须项) 记录总数，大于等于0
     * @param sliderSize
     * @param itemsPerPage 每页项数。
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
     * 取得指定大小的页码滑动窗口，并将当前页尽可能地放在滑动窗口的中间部位。例如: 总共有13页，当前页是第5页，取得一个大小为5的滑动窗口，将包括 3，4，5，6, 7这几个页码，第5页被放在中间。如果当前页是12，则返回页码为
     * 9，10，11，12，13。
     *
     * @return 包含页码的数组，如果指定滑动窗口大小小于1或总页数为0，则返回空数组。
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
     * 构造分页工具条
     */
    public String getPaginatorBar() {

        StringBuffer str = new StringBuffer("<div class=\"page\">");
        str.append("<script type=\"text/javascript\">function gotoPage(page){window.location.href=\"/" + path
                + "/pages/\" + page;}</script>");

        // 生成翻页部分
        // 1. 总记录数
        str.append("共" + this.totalItems + "条数据 &nbsp;&nbsp;");

        // 2. 页数： 当前页/总页数
        str.append("第" + this.currentPage + "页/共" + this.totalPage + "页&nbsp;&nbsp;");

        // 3. 首页,上一页
        if (this.currentPage > 1) {
            str.append("<a class=\"prev\" href=\"#\" onclick=\"gotoPage(1);\">首页</a>");
            str.append("<a class=\"prev\" href=\"#\" onclick=\"gotoPage(" + (this.currentPage - 1) + ");\">上一页</a>");
        } else {
            str.append("<a class=\"prev\" href=\"#\">首页</a>");
            str.append("<a class=\"prev\" href=\"#\">上一页</a>");
        }

        // 4 . 活动块
        int[] slider = getSlider();
        for (int i = 0; i < slider.length; i++) {
            if (slider[i] == this.currentPage) {
                str.append("<a class=\"num current_num\" href=\"#\">");
            } else {
                str.append("<a class=\"num\" href=\"#\" onclick=\"gotoPage(" + slider[i] + ");\">");
            }
            str.append(slider[i] + "</a>");
        }

        // 5 .下一页
        if (this.currentPage < this.totalPage) {
            str.append("<a class=\"prev\" href=\"#\" onclick=\"gotoPage(" + (this.currentPage + 1) + ");\">");
        } else {
            str.append("<a class=\"prev\" href=\"#\">");
        }
        str.append("下一页</a>&nbsp;&nbsp;");

        // 6. 跳转部分
        str.append("跳到第");
        str.append("<SELECT size=1 onchange=\"gotoPage(this.value);\">");
        for (int i = 1; i < this.totalPage + 1; i++) {
            if (i == this.currentPage) {
                str.append("<OPTION value=" + i + " selected>" + i + "</OPTION>");
            } else {
                str.append("<OPTION value=" + i + ">" + i + "</OPTION>");
            }
        }
        str.append("</SELECT>页");

        // 7. 隐藏条件
        str.append("</div>");
        return str.toString();
    }

    /**
     * 获得起始记录
     *
     * @return
     */
    public int getStartIndex() {
        return (this.currentPage - 1) * this.itemsPerPage + 1;
    }

}
