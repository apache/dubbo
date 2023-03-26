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
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import java.io.OutputStream;
import java.io.StringReader;

/**
 *  body content-type is xml
 */
@Activate("xml")
public class XMLCodec implements HttpMessageCodec<byte[], OutputStream> {


    @Override
    public Object decode(byte[] body, Class<?> targetType) throws Exception {


        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        // Do unmarshall operation
        Source xmlSource = new SAXSource(spf.newSAXParser().getXMLReader(), new InputSource(new StringReader(new String(body))));

        JAXBContext context = JAXBContext.newInstance(targetType);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        return unmarshaller.unmarshal(xmlSource);

    }

    @Override
    public boolean contentTypeSupport(MediaType mediaType, Class<?> targetType) {
        return MediaTypeMatcher.TEXT_XML.mediaSupport(mediaType);
    }

    @Override
    public boolean typeSupport(Class<?> targetType) {
        return false;
    }

    @Override
    public MediaType contentType() {
        return MediaType.TEXT_XML;
    }


    @Override
    public void encode(OutputStream outputStream, Object unSerializedBody, URL url) throws Exception {
        Marshaller marshaller = JAXBContext.newInstance(unSerializedBody.getClass()).createMarshaller();
        marshaller.marshal(unSerializedBody, outputStream);
    }


}
