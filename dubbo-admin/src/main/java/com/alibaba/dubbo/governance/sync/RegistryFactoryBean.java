/**
 * Project: dubbo.registry.console-2.2.0-SNAPSHOT
 * 
 * File Created at Mar 21, 2012
 * $Id: RegistryFactoryBean.java 182733 2012-06-28 07:39:47Z tony.chenl $
 * 
 * Copyright 1999-2100 Alibaba.com Corporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.governance.sync;

import java.util.regex.Pattern;

import org.springframework.beans.factory.FactoryBean;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.registry.RegistryFactory;

/**
 * @author ding.lid
 */
public class RegistryFactoryBean implements FactoryBean {

    static final String SERVER_URL = "%s?check=false&file=false";
    
    static final Pattern SEPERATOR = Pattern.compile("\\s*;\\s*");
    
    private URL url;
    
    public void setServerAddress(String address){
        if(address == null || (address = address.trim()).length() == 0) {
            throw new IllegalArgumentException("server address is empty!!");
        }
        
        String[] split = SEPERATOR.split(address);
        
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for(String s : split) {
            if(isFirst) {
                isFirst = false;
            }
            else {
                sb.append(";");
            }
            sb.append(String.format(SERVER_URL, s));
        }
        
        this.url = URL.valueOf(sb.toString());
    }
    
    public void setUrl(String url) {
        this.url = URL.valueOf(url);
    }

    public Object getObject() throws Exception {
        return ExtensionLoader.getExtensionLoader(RegistryFactory.class).getExtension(url.getProtocol()).getRegistry(url);
    }

    public Class<Registry> getObjectType() {
        return Registry.class;
    }
    
    public boolean isSingleton() {
        return true;
    }
}
