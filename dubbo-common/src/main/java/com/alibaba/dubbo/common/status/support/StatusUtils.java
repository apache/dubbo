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
package com.alibaba.dubbo.common.status.support;

import com.alibaba.dubbo.common.status.Status;
import com.alibaba.dubbo.common.status.Status.Level;

import java.util.Map;

/**
 * StatusManager
 *
 * @author william.liangf
 */
public class StatusUtils {

    public static Status getSummaryStatus(Map<String, Status> statuses) {
        Level level = Level.OK;
        StringBuilder msg = new StringBuilder();
        for (Map.Entry<String, Status> entry : statuses.entrySet()) {
            String key = entry.getKey();
            Status status = entry.getValue();
            Level l = status.getLevel();
            if (Level.ERROR.equals(l)) {
                level = Level.ERROR;
                if (msg.length() > 0) {
                    msg.append(",");
                }
                msg.append(key);
            } else if (Level.WARN.equals(l)) {
                if (!Level.ERROR.equals(level)) {
                    level = Level.WARN;
                }
                if (msg.length() > 0) {
                    msg.append(",");
                }
                msg.append(key);
            }
        }
        return new Status(level, msg.toString());
    }

}