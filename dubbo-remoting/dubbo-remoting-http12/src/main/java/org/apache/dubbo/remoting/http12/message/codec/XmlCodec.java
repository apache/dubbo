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

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.EncodeException;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.MediaType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;

import org.xml.sax.InputSource;

/**
 * Copy from {@link org.apache.dubbo.rpc.protocol.rest.message.codec}
 */
@Activate
public class XmlCodec implements HttpMessageCodec {

    @Override
    public void encode(OutputStream outputStream, Object data) throws EncodeException {
        try {
            Marshaller marshaller = JAXBContext.newInstance(data.getClass()).createMarshaller();
            marshaller.marshal(data, outputStream);
        } catch (Exception e) {
            throw new EncodeException(e);
        }
    }

    @Override
    public Object decode(InputStream inputStream, Class<?> targetType) throws DecodeException {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            // Do unmarshall operation
            Source xmlSource = new SAXSource(
                    spf.newSAXParser().getXMLReader(),
                    new InputSource(
                            new StringReader(toByteArrayStream(inputStream).toString())));
            JAXBContext context = JAXBContext.newInstance(targetType);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return unmarshaller.unmarshal(xmlSource);
        } catch (Exception e) {
            throw new DecodeException(e);
        }
    }

    @Override
    public MediaType contentType() {
        return MediaType.APPLICATION_XML;
    }
}
