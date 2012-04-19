package com.alibaba.dubbo.common.logger.slf4j;

import java.io.File;

import com.alibaba.dubbo.common.logger.Level;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerAdapter;

public class Slf4jLoggerAdapter implements LoggerAdapter {

	public Logger getLogger(String key) {
		return new Slf4jLogger(org.slf4j.LoggerFactory.getLogger(key));
	}

    public Logger getLogger(Class<?> key) {
        return new Slf4jLogger(org.slf4j.LoggerFactory.getLogger(key));
    }

    public void setLevel(Level level) {
        // TODO Auto-generated method stub
        
    }

    public Level getLevel() {
        // TODO Auto-generated method stub
        return null;
    }

    public File getFile() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setFile(File file) {
        // TODO Auto-generated method stub
        
    }

}
