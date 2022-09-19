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

package org.apache.dubbo.errorcode.reporter.impl;

import org.apache.dubbo.errorcode.extractor.MethodDefinition;
import org.apache.dubbo.errorcode.reporter.ReportResult;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Utility of generating string content.
 */
final class StringifyUtil {
    private StringifyUtil() {
        throw new UnsupportedOperationException("No instance of StringifyUtil for you! ");
    }

    static String generateIllegalInvocationString(ReportResult reportResult) {
        StringBuilder illegalInvocationReportStringBuilder = new StringBuilder();
        illegalInvocationReportStringBuilder.append("Illegal logger method invocations: ");
        illegalInvocationReportStringBuilder.append('\n');

        for (Map.Entry<String, List<MethodDefinition>> entry : reportResult.getIllegalInvocations().entrySet()) {

            String key = entry.getKey();

            illegalInvocationReportStringBuilder.append(
                key.substring(key.indexOf("org\\apache\\dubbo\\".replace('\\', File.separatorChar)))
                    .replace("\\", ".")
                    .replace(".class", "")
            );

            illegalInvocationReportStringBuilder.append(": ");

            for (int i = 0; i < entry.getValue().size(); i++) {

                MethodDefinition methodDefinition = entry.getValue().get(i);

                illegalInvocationReportStringBuilder
                    .append(methodDefinition.getClassName())
                    .append("#")
                    .append(methodDefinition.getMethodName())
                    .append(":")
                    .append(methodDefinition.getArguments());

                if (i != entry.getValue().size() - 1) {
                    illegalInvocationReportStringBuilder.append(", ");
                }
            }

            illegalInvocationReportStringBuilder.append('\n');
        }

        return illegalInvocationReportStringBuilder.toString();
    }
}
