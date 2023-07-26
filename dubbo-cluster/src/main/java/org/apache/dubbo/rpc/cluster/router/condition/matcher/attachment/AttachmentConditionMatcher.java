/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.cluster.router.condition.matcher.attachment;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.cluster.router.condition.matcher.AbstractConditionMatcher;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CLUSTER_FAILED_EXEC_CONDITION_ROUTER;

/**
 * analysis the arguments in the rule.
 * Examples would be like this:
 * "attachments[foo]=bar", whenCondition is that the attachment value of 'foo' is equal to 'bar'.
 */
@Activate
public class AttachmentConditionMatcher extends AbstractConditionMatcher {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AttachmentConditionMatcher.class);
    private static final Pattern ATTACHMENTS_PATTERN = Pattern.compile("attachments\\[(.+)\\]");

    public AttachmentConditionMatcher(String key, ModuleModel model) {
        super(key, model);
    }

    @Override
    protected String getValue(Map<String, String> sample, URL url, Invocation invocation) {
        try {
            // split the rule
            String[] expressArray = key.split("\\.");
            String argumentExpress = expressArray[0];
            final Matcher matcher = ATTACHMENTS_PATTERN.matcher(argumentExpress);
            if (!matcher.find()) {
                return DOES_NOT_FOUND_VALUE;
            }

            //extract the argument index
            String attachmentKey = matcher.group(1);
            if (StringUtils.isEmpty(attachmentKey)) {
                return DOES_NOT_FOUND_VALUE;
            }

            //extract the argument value
            return invocation.getAttachment(attachmentKey);
        } catch (Exception e) {
            logger.warn(CLUSTER_FAILED_EXEC_CONDITION_ROUTER,
                "condition state router attachment match failed",
                "",
                "Invalid match condition: " + key,
                e);
        }
        return DOES_NOT_FOUND_VALUE;
    }
}
