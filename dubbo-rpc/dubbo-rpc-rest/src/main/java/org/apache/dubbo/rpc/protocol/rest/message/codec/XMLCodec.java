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
package org.apache.dubbo.rpc.protocol.rest.message.codec;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.rpc.protocol.rest.message.HttpMessageCodec;
import org.apache.dubbo.rpc.protocol.rest.message.MediaTypeMatcher;
import org.xml.sax.InputSource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Activate("xml")
public class XMLCodec implements HttpMessageCodec<byte[], OutputStream> {


    @Override
    public Object decode(byte[] body, Class targetType) throws Exception {

        try (InputStream entityStream = new ByteArrayInputStream(body);) {
            Unmarshaller unmarshaller = JAXBContext.newInstance(targetType).createUnmarshaller();
            InputSource is = new InputSource(entityStream);
            is.setEncoding(StandardCharsets.UTF_8.name());
            StreamSource source = new StreamSource(new InputStreamReader(entityStream, StandardCharsets.UTF_8));
            source.setInputStream(entityStream);
            return unmarshaller.unmarshal(source);
        } catch (Throwable throwable) {
            throw throwable;
        }

    }

    @Override
    public boolean contentTypeSupport(MediaType mediaType, Class targetType) {
        return MediaTypeMatcher.TEXT_XML.mediaSupport(mediaType);
    }


    @Override
    public void encode(OutputStream outputStream, Object unSerializedBody, URL url) throws Exception {
        Marshaller marshaller = JAXBContext.newInstance(unSerializedBody.getClass()).createMarshaller();
        marshaller.marshal(unSerializedBody, outputStream);
        outputStream.write((byte[]) unSerializedBody);
    }
}
