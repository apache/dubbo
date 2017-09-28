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
package com.alibaba.dubbo.config.spring.status;

import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.status.Status;
import com.alibaba.dubbo.common.status.StatusChecker;
import com.alibaba.dubbo.config.spring.ServiceBean;

import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;

import java.lang.reflect.Method;

/**
 * SpringStatusChecker
 *
 * @author william.liangf
 */
@Activate
public class SpringStatusChecker implements StatusChecker {

    private static final Logger logger = LoggerFactory.getLogger(SpringStatusChecker.class);

    public Status check() {
        ApplicationContext context = ServiceBean.getSpringContext();
        if (context == null) {
            return new Status(Status.Level.UNKNOWN);
        }
        Status.Level level = Status.Level.OK;
        if (context instanceof Lifecycle) {
            if (((Lifecycle) context).isRunning()) {
                level = Status.Level.OK;
            } else {
                level = Status.Level.ERROR;
            }
        } else {
            level = Status.Level.UNKNOWN;
        }
        StringBuilder buf = new StringBuilder();
        try {
            Class<?> cls = context.getClass();
            Method method = null;
            while (cls != null && method == null) {
                try {
                    method = cls.getDeclaredMethod("getConfigLocations", new Class<?>[0]);
                } catch (NoSuchMethodException t) {
                    cls = cls.getSuperclass();
                }
            }
            if (method != null) {
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                String[] configs = (String[]) method.invoke(context, new Object[0]);
                if (configs != null && configs.length > 0) {
                    for (String config : configs) {
                        if (buf.length() > 0) {
                            buf.append(",");
                        }
                        buf.append(config);
                    }
                }
            }
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
        return new Status(level, buf.toString());
    }

}