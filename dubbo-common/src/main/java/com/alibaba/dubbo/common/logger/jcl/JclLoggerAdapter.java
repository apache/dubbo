package com.alibaba.dubbo.common.logger.jcl;

import java.io.File;

import org.apache.commons.logging.LogFactory;

import com.alibaba.dubbo.common.logger.Level;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerAdapter;

public class JclLoggerAdapter implements LoggerAdapter {

	public Logger getLogger(String key) {
		return new JclLogger(LogFactory.getLog(key));
	}

    public Logger getLogger(Class<?> key) {
        return new JclLogger(LogFactory.getLog(key));
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
