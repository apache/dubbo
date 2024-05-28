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
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.MediaType;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import org.xml.sax.InputSource;

public class XmlCodec implements HttpMessageCodec {

    @Override
    public void encode(OutputStream os, Object data, Charset charset) throws EncodeException {
        try {
            Marshaller marshaller = JAXBContext.newInstance(data.getClass()).createMarshaller();
            try (OutputStreamWriter writer = new OutputStreamWriter(os, charset)) {
                marshaller.marshal(data, writer);
            }
        } catch (HttpStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new EncodeException("Error encoding xml", e);
        }
    }

    @Override
    public Object decode(InputStream is, Class<?> targetType, Charset charset) throws DecodeException {
        try {
            try (InputStreamReader reader = new InputStreamReader(is, charset)) {
                InputSource inputSource = new InputSource(reader);
                inputSource.setEncoding(charset.name());
                Source xmlSource = new SAXSource(newSAXParser().getXMLReader(), inputSource);
                JAXBContext context = JAXBContext.newInstance(targetType);
                Unmarshaller unmarshaller = context.createUnmarshaller();
                return unmarshaller.unmarshal(xmlSource);
            }
        } catch (HttpStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new DecodeException("Error decoding xml", e);
        }
    }

    private SAXParser newSAXParser() throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        spf.setXIncludeAware(false);
        return spf.newSAXParser();
    }

    @Override
    public MediaType mediaType() {
        return MediaType.APPLICATION_XML;
    }
}
