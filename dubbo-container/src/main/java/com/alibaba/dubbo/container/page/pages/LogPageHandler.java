/**
 * Project: dubbo.core.service.server-1.0.6-SNAPSHOT
 * 
 * File Created at 2010-1-19
 * $Id: LogInformationProvider.java 34672 2010-01-19 06:25:44Z william.liangf $
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
package com.alibaba.dubbo.container.page.pages;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;

import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;

/**
 * LogPageHandler
 * 
 * @author william.liangf
 */
@Extension("log")
public class LogPageHandler implements PageHandler {

    private static final int SHOW_LOG_LENGTH = 30000;

    private File            file;
    
    @SuppressWarnings("unchecked")
	public LogPageHandler() {
	    try {
			org.apache.log4j.Logger logger = LogManager.getRootLogger();
	        if (logger != null) {
	            Enumeration<Appender> appenders = logger.getAllAppenders();
	            if (appenders != null) {
	                while (appenders.hasMoreElements()) {
	                    Appender appender = appenders.nextElement();
	                    if (appender instanceof FileAppender) {
	                        FileAppender fileAppender = (FileAppender)appender;
	                        String filename = fileAppender.getFile();
	                        file = new File(filename);
	                        break;
	                    }
	                }
	            }
	        }
	    } catch (Throwable t) {
	    }
    }

    public Page handle(URL url) {
        long size = 0;
		String content = "";
		String modified = "Not exist";
		if (file != null && file.exists()) {
			try {
				FileInputStream fis = new FileInputStream(file);
				FileChannel channel = fis.getChannel();
				size = channel.size();
				ByteBuffer bb;
				if (size <= SHOW_LOG_LENGTH) {
					bb = ByteBuffer.allocate((int) size);
					channel.read(bb, 0);
				} else {
					int pos = (int) (size - SHOW_LOG_LENGTH);
					bb = ByteBuffer.allocate(SHOW_LOG_LENGTH);
					channel.read(bb, pos);
				}
				bb.flip();
				content = new String(bb.array()).replace("<", "&lt;")
						.replace(">", "&gt;").replace("\n", "<br/><br/>");
				modified = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
						.format(new Date(file.lastModified()));
			} catch (IOException e) {
			}
		}
		Level level = LogManager.getRootLogger().getLevel();
        List<List<String>> rows = new ArrayList<List<String>>();
        List<String> row = new ArrayList<String>();
        row.add(content);
        rows.add(row);
        return new Page("Home", "Log",  new String[] {(file == null ? "" : file.getName()) + ", " + size + " bytes, " + modified + ", " + level}, rows);
    }

}
