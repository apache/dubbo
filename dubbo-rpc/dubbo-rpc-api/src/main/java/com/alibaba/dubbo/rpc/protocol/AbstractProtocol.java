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
package com.alibaba.dubbo.rpc.protocol;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.support.ProtocolUtils;

/**
 * abstract ProtocolSupport.
 * 
 * @author qian.lei
 * @author william.liangf
 */
public abstract class AbstractProtocol implements Protocol {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected final Map<String, Exporter<?>> exporterMap = new ConcurrentHashMap<String, Exporter<?>>();

	//TODO SOFEREFENCE
    protected final Set<Invoker<?>> invokers = new ConcurrentHashSet<Invoker<?>>();
    
	protected static String serviceKey(URL url) {
	    return ProtocolUtils.serviceKey(url);
	}

	protected static String serviceKey(int port, String serviceName, String serviceVersion, String serviceGroup) {
		return ProtocolUtils.serviceKey(port, serviceName, serviceVersion, serviceGroup);
	}
	
	public void destroy() {
	    for (Invoker<?> invoker : invokers){
	        if (invoker != null) {
	            invokers.remove(invoker);
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Destroy reference: " + invoker.getUrl());
                    }
                    invoker.destroy();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
	    }
	    for (String key : new ArrayList<String>(exporterMap.keySet())) {
            Exporter<?> exporter = exporterMap.remove(key);
            if (exporter != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Unexport service: " + exporter.getInvoker().getUrl());
                    }
                    exporter.unexport();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }
	}
	@SuppressWarnings("deprecation")
    protected static int getServerShutdownTimeout() {
        int timeout = Constants.DEFAULT_SERVER_SHUTDOWN_TIMEOUT;
        String value = ConfigUtils.getProperty(Constants.SHUTDOWN_WAIT_KEY);
        if (value != null && value.length() > 0) {
            try{
                timeout = Integer.parseInt(value);
            }catch (Exception e) {
            }        
        } else {
            value = ConfigUtils.getProperty(Constants.SHUTDOWN_WAIT_SECONDS_KEY);
            if (value != null && value.length() > 0) {
                try{
                    timeout = Integer.parseInt(value) * 1000;
                }catch (Exception e) {
                }        
            }
        }
        
        return timeout;
    }
}