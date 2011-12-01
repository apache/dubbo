/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common.utils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;

/**
 * @author ding.lid
 * @author william.liangf
 */
public class ConfigUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigUtils.class);
    
    public static boolean isNotEmpty(String value) {
        return ! isEmpty(value);
    }
	
	public static boolean isEmpty(String value) {
		return value == null || value.length() == 0 
    			|| "null".equalsIgnoreCase(value) 
    			|| "false".equalsIgnoreCase(value) 
    			|| "N/A".equalsIgnoreCase(value);
	}
	
	public static boolean isDefault(String value) {
		return "true".equalsIgnoreCase(value) 
				|| "default".equalsIgnoreCase(value);
	}
	
	public static List<String> mergeValues(Class<?> type, String cfg, List<String> def) {
	    List<String> defaults = new ArrayList<String>();
        if (def != null) {
            for (String name : def) {
                if (ExtensionLoader.getExtensionLoader(type).hasExtension(name)) {
                    defaults.add(name);
                }
            }
        }
	    List<String> names = new ArrayList<String>();
        if (! names.contains(Constants.REMOVE_VALUE_PREFIX + Constants.DEFAULT_KEY)) {
            int i = names.indexOf(Constants.DEFAULT_KEY);
            if (i > 0) {
                names.addAll(i, defaults);
            } else {
                names.addAll(defaults);
            }
            names.remove(Constants.DEFAULT_KEY);
        }
        String[] configs = cfg == null ? new String[0] : Constants.COMMA_SPLIT_PATTERN.split(cfg);
        for (String config : configs) {
            if(config != null && config.length() > 0) {
                String[] fs = Constants.COMMA_SPLIT_PATTERN.split(config);
                names.addAll(Arrays.asList(fs));
            }
        }
        for (String name : new ArrayList<String>(names)) {
            if (name.startsWith(Constants.REMOVE_VALUE_PREFIX)) {
                names.remove(name);
                names.remove(name.substring(1));
            }
        }
        return names;
	}

    private static volatile Properties PROPERTIES;
    
    public static Properties getProperties() {
        if (PROPERTIES == null) {
            synchronized (ConfigUtils.class) {
                if (PROPERTIES == null) {
                    PROPERTIES = ConfigUtils.loadProperties(Constants.DUBBO_PROPERTIES, false);
                }
            }
        }
        return PROPERTIES;
    }
    
    public static void addProperties(Properties properties) {
        if (properties != null) {
            getProperties().putAll(properties);
        }
    }
    
	public static String getProperty(String key) {
	    return getProperty(key, null);
	}
	
    public static String getProperty(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value != null && value.length() > 0) {
            return value;
        }
        return getProperties().getProperty(key, defaultValue);
    }
    
	/**
	 * Load properties file to {@link Properties} from class path.
	 * 
	 * @param fileName properties file name. for example: <code>dubbo.properties</code>, <code>METE-INF/conf/foo.properties</code>
	 * @param allowMultiFile if <code>false</code>, throw {@link IllegalStateException} when found multi file on the class path. 
	 * @return loaded {@link Properties} content, merge multi properties file if found multi file 
	 * @throws IllegalStateException not allow multi-file, but multi-file exsit on class path.
	 */
    public static Properties loadProperties(String fileName, boolean allowMultiFile) {
        Properties properties = new Properties();
        
        List<java.net.URL> list = new ArrayList<java.net.URL>();
        try {
            Enumeration<java.net.URL> urls = ClassHelper.getClassLoader().getResources(fileName);
            list = new ArrayList<java.net.URL>();
            while (urls.hasMoreElements()) {
                list.add(urls.nextElement());
            }
        }
        catch (Throwable t) {
            logger.warn("Fail to load " + fileName + " file: " + t.getMessage(), t);
        }
        
        if(list.size() == 0) {
            logger.warn("No " + fileName + " found on the class path.");
            return properties;
        }
        if(!allowMultiFile && list.size() > 1) {
            String errMsg = String.format("only 1 %s file is expected, but %d dubbo.properties files found on class path: %s",
                    fileName, list.size(), list.toString());
            logger.error(errMsg);
            throw new IllegalStateException(errMsg);
        }
        
        logger.info("load " + fileName + " properties file from " + list);

        for(java.net.URL url : list) {
            try {
                Properties p = new Properties();
                InputStream input = url.openStream();
                if (input != null) {
                    try {
                        p.load(input);
                        properties.putAll(p);
                    } finally {
                        try {
                            input.close();
                        } catch (Throwable t) {}
                    }
                }
            } catch (Throwable e) {
                logger.warn("Fail to load " + fileName + " file from " + url + "(ingore this file): " + e.getMessage(), e);
            }
        }
        
        return properties;
    }

	private ConfigUtils() {}
	
}