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
package org.apache.dubbo.metadata;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.model.BuiltinServiceDetector;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_CLASS_NOT_FOUND;

public class MetadataServiceV2Detector implements BuiltinServiceDetector {

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(MetadataServiceV2Detector.class);

    public static final String NAME = "metadataV2";

    @Override
    public Class<?> getService() {
        if (!support()) {
            logger.warn(
                    COMMON_CLASS_NOT_FOUND,
                    "",
                    "",
                    "To use MetadataServiceV2, Protobuf dependencies are required. Fallback to MetadataService(V1).");
            return null;
        }
        return org.apache.dubbo.metadata.MetadataServiceV2.class;
    }

    public static boolean support() {
        try {
            Class.forName("com.google.protobuf.Message");
        } catch (ClassNotFoundException classNotFoundException) {
            return false;
        }
        return true;
    }
}
