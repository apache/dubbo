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
package com.alibaba.dubbo.container.page;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PageServlet
 */
public class PageServlet extends HttpServlet {

    protected static final Logger logger = LoggerFactory.getLogger(PageServlet.class);
    private static final long serialVersionUID = -8370312705453328501L;
    private static PageServlet INSTANCE;
    protected final Random random = new Random();
    protected final Map<String, PageHandler> pages = new ConcurrentHashMap<String, PageHandler>();
    protected final List<PageHandler> menus = new ArrayList<PageHandler>();

    public static PageServlet getInstance() {
        return INSTANCE;
    }

    public List<PageHandler> getMenus() {
        return Collections.unmodifiableList(menus);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        INSTANCE = this;
        String config = getServletConfig().getInitParameter("pages");
        Collection<String> names;
        if (config != null && config.length() > 0) {
            names = Arrays.asList(Constants.COMMA_SPLIT_PATTERN.split(config));
        } else {
            names = ExtensionLoader.getExtensionLoader(PageHandler.class).getSupportedExtensions();
        }
        for (String name : names) {
            PageHandler handler = ExtensionLoader.getExtensionLoader(PageHandler.class).getExtension(name);
            pages.put(ExtensionLoader.getExtensionLoader(PageHandler.class).getExtensionName(handler), handler);
            Menu menu = handler.getClass().getAnnotation(Menu.class);
            if (menu != null) {
                menus.add(handler);
            }
        }
        Collections.sort(menus, new MenuComparator());
    }

    @Override
    protected final void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected final void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!response.isCommitted()) {
            PrintWriter writer = response.getWriter();
            String uri = request.getRequestURI();
            boolean isHtml = false;
            if (uri == null || uri.length() == 0 || "/".equals(uri)) {
                uri = "index";
                isHtml = true;
            } else {
                if (uri.startsWith("/")) {
                    uri = uri.substring(1);
                }
                if (uri.endsWith(".html")) {
                    uri = uri.substring(0, uri.length() - ".html".length());
                    isHtml = true;
                }
            }
            if (uri.endsWith("favicon.ico")) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            ExtensionLoader<PageHandler> pageHandlerLoader = ExtensionLoader.getExtensionLoader(PageHandler.class);
            PageHandler pageHandler = pageHandlerLoader.hasExtension(uri) ? pageHandlerLoader.getExtension(uri) : null;
            if (isHtml) {
                writer.println("<html><head><title>Dubbo</title>");
                writer.println("<style type=\"text/css\">html, body {margin: 10;padding: 0;background-color: #6D838C;font-family: Arial, Verdana;font-size: 12px;color: #FFFFFF;text-align: center;vertical-align: middle;word-break: break-all; } table {width: 90%; margin: 0px auto;border-collapse: collapse;border: 8px solid #FFFFFF; } thead tr {background-color: #253c46; } tbody tr {background-color: #8da5af; } th {padding-top: 4px;padding-bottom: 4px;font-size: 14px;height: 20px; } td {margin: 3px;padding: 3px;border: 2px solid #FFFFFF;font-size: 14px;height: 25px; } a {color: #FFFFFF;cursor: pointer;text-decoration: underline; } a:hover {text-decoration: none; }</style>");
                writer.println("</head><body>");
            }
            if (pageHandler != null) {
                Page page = null;
                try {
                    String query = request.getQueryString();
                    page = pageHandler.handle(URL.valueOf(request.getRequestURL().toString()
                            + (query == null || query.length() == 0 ? "" : "?" + query)));
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                    String msg = t.getMessage();
                    if (msg == null) {
                        msg = StringUtils.toString(t);
                    }
                    if (isHtml) {
                        writer.println("<table>");
                        writer.println("<thead>");
                        writer.println("    <tr>");
                        writer.println("        <th>Error</th>");
                        writer.println("    </tr>");
                        writer.println("</thead>");
                        writer.println("<tbody>");
                        writer.println("    <tr>");
                        writer.println("        <td>");
                        writer.println("            " + msg.replace("<", "&lt;").replace(">", "&lt;").replace("\n", "<br/>"));
                        writer.println("        </td>");
                        writer.println("    </tr>");
                        writer.println("</tbody>");
                        writer.println("</table>");
                        writer.println("<br/>");
                    } else {
                        writer.println(msg);
                    }
                }
                if (page != null) {
                    if (isHtml) {
                        String nav = page.getNavigation();
                        if (nav == null || nav.length() == 0) {
                            nav = ExtensionLoader.getExtensionLoader(PageHandler.class).getExtensionName(pageHandler);
                            nav = nav.substring(0, 1).toUpperCase() + nav.substring(1);
                        }
                        if (!"index".equals(uri)) {
                            nav = "<a href=\"/\">Home</a> &gt; " + nav;
                        }
                        writeMenu(request, writer, nav);
                        writeTable(writer, page.getTitle(), page.getColumns(),
                                page.getRows());
                    } else {
                        if (page.getRows().size() > 0 && page.getRows().get(0).size() > 0) {
                            writer.println(page.getRows().get(0).get(0));
                        }
                    }
                }
            } else {
                if (isHtml) {
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
                } else {
                    writer.println("Not found " + uri + " page.");
                }
            }
            if (isHtml) {
                writer.println("</body></html>");
            }
            writer.flush();
        }
    }

    protected final void writeMenu(HttpServletRequest request, PrintWriter writer, String nav) {
        writer.println("<table>");
        writer.println("<thead>");
        writer.println("    <tr>");
        for (PageHandler handler : menus) {
            String uri = ExtensionLoader.getExtensionLoader(PageHandler.class).getExtensionName(handler);
            Menu menu = handler.getClass().getAnnotation(Menu.class);
            writer.println("        <th><a href=\"" + uri + ".html\">" + menu.name() + "</a></th>");
        }
        writer.println("    </tr>");
        writer.println("</thead>");
        writer.println("<tbody>");
        writer.println("    <tr>");
        writer.println("        <td style=\"text-align: left\" colspan=\"" + menus.size() + "\">");
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
