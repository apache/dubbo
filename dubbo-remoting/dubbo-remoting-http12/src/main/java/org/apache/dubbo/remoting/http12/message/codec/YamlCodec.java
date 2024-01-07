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
package org.apache.dubbo.remoting.http12.message.codec;

import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.EncodeException;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.MediaType;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Iterator;

import org.yaml.snakeyaml.Yaml;

public class YamlCodec implements HttpMessageCodec {

    @Override
    public Object decode(InputStream is, Class<?> targetType, Charset charset) throws DecodeException {
        try (InputStreamReader reader = new InputStreamReader(is, charset)) {
            return new Yaml().loadAs(reader, targetType);
        } catch (Throwable t) {
            throw new DecodeException("Error decoding yaml", t);
        }
    }

    @Override
    public Object[] decode(InputStream is, Class<?>[] targetTypes, Charset charset) throws DecodeException {
        try (InputStreamReader reader = new InputStreamReader(is, charset)) {
            Yaml yaml = new Yaml();
            Iterator<Object> iterator = yaml.loadAll(reader).iterator();
            Object[] results = new Object[targetTypes.length];
            for (int i = 0; i < targetTypes.length; i++) {
                if (iterator.hasNext()) {
                    Object result = iterator.next();
                    Class<?> targetType = targetTypes[i];
                    if (targetType.isInstance(result)) {
                        results[i] = result;
                    } else {
                        results[i] = yaml.loadAs(yaml.dump(result), targetType);
                    }
                } else {
                    throw new DecodeException("Not enough YAML documents in the stream");
                }
            }
            return results;
        } catch (Throwable t) {
            throw new DecodeException("Error decoding yaml", t);
        }
    }

    @Override
    public void encode(OutputStream os, Object data, Charset charset) throws EncodeException {
        try (OutputStreamWriter writer = new OutputStreamWriter(os, charset)) {
            new Yaml().dump(data, writer);
        } catch (Throwable t) {
            throw new EncodeException("Error encoding yaml", t);
        }
    }

    @Override
    public void encode(OutputStream os, Object[] data, Charset charset) throws EncodeException {
        try (OutputStreamWriter writer = new OutputStreamWriter(os, charset)) {
            new Yaml().dump(data, writer);
        } catch (Throwable t) {
            throw new EncodeException("Error encoding yaml", t);
        }
    }

    @Override
    public MediaType mediaType() {
        return MediaType.APPLICATION_YAML;
    }
}
