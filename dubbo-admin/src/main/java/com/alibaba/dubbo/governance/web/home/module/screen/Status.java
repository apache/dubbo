/*
 * Copyright 2011 Alibaba.com All right reserved. This software is the
 * confidential and proprietary information of Alibaba.com ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Alibaba.com.
 */
package com.alibaba.dubbo.governance.web.home.module.screen;

import com.alibaba.dubbo.common.status.Status.Level;
import com.alibaba.dubbo.common.status.StatusChecker;
import com.alibaba.dubbo.registry.common.StatusManager;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author tony.chenl
 */
public class Status {
    private static final Pattern OK_PATTERN = Pattern.compile("o(k)", Pattern.CASE_INSENSITIVE);
    @Autowired
    private HttpServletResponse response;

    public static String filterOK(String message) {
        if (message == null)
            return "";
        // 避免ok关键字，用数字0代替字母o
        return OK_PATTERN.matcher(message).replaceAll("0$1");
    }

    public void execute(Map<String, Object> context) throws Exception {
        //FIXME cache监控存在性能问题 汇总页面去掉
        Map<String, com.alibaba.dubbo.common.status.Status> statuses = StatusManager.getInstance().getStatusList(new String[]{"cache"});
        com.alibaba.dubbo.common.status.Status status = StatusManager.getInstance().getStatusSummary(statuses);
        Level level = status.getLevel();
        if (!com.alibaba.dubbo.common.status.Status.Level.OK.equals(level)) {
            context.put("message", level
                    + new SimpleDateFormat(" [yyyy-MM-dd HH:mm:ss] ").format(new Date())
                    + filterOK(status.getMessage()));
        } else {
            context.put("message", level.toString());
        }
        PrintWriter writer = response.getWriter();
        writer.print(context.get("message").toString());
        writer.flush();
    }

    public void setStatusHandlers(Collection<StatusChecker> statusHandlers) {
        StatusManager.getInstance().addStatusHandlers(statusHandlers);
    }

}
