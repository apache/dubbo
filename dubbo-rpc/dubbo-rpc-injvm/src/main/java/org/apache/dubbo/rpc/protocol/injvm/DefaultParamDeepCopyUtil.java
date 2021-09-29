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
package org.apache.dubbo.rpc.protocol.injvm;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.remoting.Constants;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DefaultParamDeepCopyUtil implements ParamDeepCopyUtil {
    private static final Logger logger = LoggerFactory.getLogger(DefaultParamDeepCopyUtil.class);

    public final static String NAME = "default";

    @Override
    @SuppressWarnings({"unchecked"})
    public <T> T copy(URL url, Object src, Class<T> targetClass) {
        Serialization serialization = url.getOrDefaultFrameworkModel().getExtensionLoader(Serialization.class).getExtension(
            url.getParameter(Constants.SERIALIZATION_KEY, Constants.DEFAULT_REMOTING_SERIALIZATION));

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ObjectOutput objectOutput = serialization.serialize(url, outputStream);
            objectOutput.writeObject(src);
            objectOutput.flushBuffer();

            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
                ObjectInput objectInput = serialization.deserialize(url, inputStream);
                return objectInput.readObject(targetClass);
            } catch (ClassNotFoundException | IOException e) {
                logger.error("Unable to deep copy parameter to target class.", e);
            }

        } catch (IOException e) {
            logger.error("Unable to deep copy parameter to target class.", e);
        }


        if (src.getClass().equals(targetClass)) {
            return (T) src;
        } else {
            return null;
        }
    }
}
