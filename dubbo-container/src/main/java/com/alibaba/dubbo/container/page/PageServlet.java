/**
 * Project: dubbo.core.service.server-1.0.6-SNAPSHOT
 * 
 * File Created at 2010-1-19
 * $Id: InformationServlet.java 34872 2010-01-21 02:12:30Z william.liangf $
 * 
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.container.page;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;

/**
 * InformationServlet
 * 
 * @author william.liangf
 */
public class PageServlet extends HttpServlet {

    private static final long     serialVersionUID = -8370312705453328501L;

    protected static final Logger logger           = LoggerFactory.getLogger(PageServlet.class);

    protected final Random        random           = new Random();

    protected String              applicationName;

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    @SuppressWarnings("unchecked")
    protected final void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (! response.isCommitted()) {
            PrintWriter writer = response.getWriter();
            writer.println("<html><head><title>" + applicationName + " service server</title>");
            writer
                    .println("<style type=\"text/css\">html, body {margin: 10;padding: 0;background-color: #6D838C;font-family: Arial, Verdana;font-size: 12px;color: #FFFFFF;text-align: center;vertical-align: middle;word-break: break-all; } table {width: 800px;margin: 0px auto;border-collapse: collapse;border: 8px solid #FFFFFF; } thead tr {background-color: #253c46; } tbody tr {background-color: #8da5af; } th {padding-top: 4px;padding-bottom: 4px;font-size: 14px;height: 20px; } td {margin: 3px;padding: 3px;border: 2px solid #FFFFFF;height: 25px; } a {color: #FFFFFF;cursor: pointer;text-decoration: underline; } a:hover {text-decoration: none; }</style>");
            writer.println("</head><body>");
            String uri = request.getRequestURI();
            PageFactory informationProvider = PageManager.getInstance()
                    .getInformationProvider(uri);
            if (informationProvider != null) {
                Page information = informationProvider.getPage(request
                        .getParameterMap());
                if (information != null) {
                    writeMenu(request, writer, information.getNavigation());
                    writeTable(writer, information.getTitle(), information.getColumns(),
                            information.getRows());
                }
            } else {
                writer.println("<table>");
                writer.println("<thead>");
                writer.println("    <tr>");
                writer.println("        <th>Error</th>");
                writer.println("    </tr>");
                writer.println("</thead>");
                writer.println("<tbody>");
                writer.println("    <tr>");
                writer.println("        <td>");
                writer.println("            Not found " + uri + " page. Please goto <a href=\"/\">Home</a> page.");
                writer.println("        </td>");
                writer.println("    </tr>");
                writer.println("</tbody>");
                writer.println("</table>");
                writer.println("<br/>");
            }
            writer.println("</body></html>");
            writer.flush();
        }
    }

    protected final void writeMenu(HttpServletRequest request, PrintWriter writer, String nav) {
        writer.println("<table>");
        writer.println("<thead>");
        writer.println("    <tr>");
        for (PageFactory informationProvider : PageManager.getInstance().getInformationProviders()) {
            writer.println("        <th><a href=\"" + informationProvider.getUri() + "\">" + informationProvider.getName() + "</a></th>");
        }
        writer.println("    </tr>");
        writer.println("</thead>");
        writer.println("<tbody>");
        writer.println("    <tr>");
        writer.println("        <td style=\"text-align: left\" colspan=\"7\">");
        writer.println(nav);
        writer.println("        </td>");
        writer.println("    </tr>");
        writer.println("</tbody>");
        writer.println("</table>");
        writer.println("<br/>");
    }

    protected final void writeTable(PrintWriter writer, String title, List<String> columns,
                                    List<List<String>> rows) {
        int n = random.nextInt();
        int c = (columns == null ? (rows == null || rows.size() == 0 ? 0 : rows.get(0).size())
                : columns.size());
        int r = (rows == null ? 0 : rows.size());
        writer.println("<table>");
        writer.println("<thead>");
        writer.println("    <tr>");
        writer.println("        <th colspan=\"" + c + "\">" + title + "</th>");
        writer.println("    </tr>");
        if (columns != null && columns.size() > 0) {
            writer.println("    <tr>");
            for (int i = 0; i < columns.size(); i++) {
                String col = columns.get(i);
                if (col.endsWith(":")) {
                    col += " <input type=\"text\" id=\"in_"
                            + n
                            + "_"
                            + i
                            + "\" onkeyup=\"for (var i = 0; i < "
                            + r
                            + "; i ++) { var m = true; for (var j = 0; j < "
                            + columns.size()
                            + "; j ++) { if (document.getElementById('in_"
                            + n
                            + "_' + j)) { var iv = document.getElementById('in_"
                            + n
                            + "_' + j).value; var tv = document.getElementById('td_"
                            + n
                            + "_' + i + '_' + j).innerHTML; if (iv.length > 0 && (tv.length < iv.length || tv.indexOf(iv) == -1)) { m = false; break; } } } document.getElementById('tr_"
                            + n
                            + "_' + i).style.display = (m ? '' : 'none');}\" sytle=\"width: 100%\" />";
                }
                writer.println("        <td>" + col + "</td>");
            }
            writer.println("    </tr>");
        }
        writer.println("</thead>");
        if (rows != null && rows.size() > 0) {
            writer.println("<tbody>");
            int i = 0;
            for (Collection<String> row : rows) {
                writer.println("    <tr id=\"tr_" + n + "_" + i + "\">");
                int j = 0;
                for (String col : row) {
                    writer.println("        <td id=\"td_" + n + "_" + i + "_" + j
                            + "\" style=\"display: ;\">" + col + "</td>");
                    j++;
                }
                writer.println("    </tr>");
                i++;
            }
            writer.println("</tbody>");
        }
        writer.println("</table>");
        writer.println("<br/>");
    }

}
