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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.ExtensionLoader;

public class ConfigUtils {
    
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

	private ConfigUtils() {}
	
}