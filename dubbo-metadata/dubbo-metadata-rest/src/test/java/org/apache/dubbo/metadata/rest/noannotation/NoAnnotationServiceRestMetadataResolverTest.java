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
package org.apache.dubbo.metadata.rest.noannotation;

import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.metadata.rest.DefaultRestService;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;
import org.apache.dubbo.metadata.rest.noannotaion.NoAnnotationServiceRestMetadataResolver;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NoAnnotationServiceRestMetadataResolverTest {
    private NoAnnotationServiceRestMetadataResolver instance =
            new NoAnnotationServiceRestMetadataResolver(ApplicationModel.defaultModel());

    @Test
    void testResolve() {

        List<String> jsons = Arrays.asList(
                "{\"argInfos\":[{\"annotationNameAttribute\":\"form\",\"formContentType\":false,\"index\":0,\"paramAnnotationType\":\"org.apache.dubbo.metadata.rest.tag.NoAnnotationTag\",\"paramName\":\"form\",\"paramType\":\"java.lang.String\",\"urlSplitIndex\":0}],\"codeStyle\":\"org.apache.dubbo.metadata.rest.noannotaion.NoAnnotationServiceRestMetadataResolver\",\"indexToName\":{0:[\"form\"]},\"method\":{\"annotations\":[],\"parameters\":[]},\"request\":{\"consumes\":[\"application/json\"],\"headerNames\":[],\"headers\":{},\"method\":\"POST\",\"paramNames\":[],\"params\":{},\"path\":\"/org.apache.dubbo.metadata.rest.RestService/form\",\"produces\":[\"application/json\"]}}",
                "{\"argInfos\":[{\"annotationNameAttribute\":\"header\",\"formContentType\":false,\"index\":0,\"paramAnnotationType\":\"org.apache.dubbo.metadata.rest.tag.NoAnnotationTag\",\"paramName\":\"header\",\"paramType\":\"java.lang.String\",\"urlSplitIndex\":0},{\"annotationNameAttribute\":\"header2\",\"formContentType\":false,\"index\":1,\"paramAnnotationType\":\"org.apache.dubbo.metadata.rest.tag.NoAnnotationTag\",\"paramName\":\"header2\",\"paramType\":\"java.lang.String\",\"urlSplitIndex\":0},{\"annotationNameAttribute\":\"param\",\"formContentType\":false,\"index\":2,\"paramAnnotationType\":\"org.apache.dubbo.metadata.rest.tag.NoAnnotationTag\",\"paramName\":\"param\",\"paramType\":\"java.lang.Integer\",\"urlSplitIndex\":0}],\"codeStyle\":\"org.apache.dubbo.metadata.rest.noannotaion.NoAnnotationServiceRestMetadataResolver\",\"indexToName\":{0:[\"header\"],1:[\"header2\"],2:[\"param\"]},\"method\":{\"annotations\":[],\"parameters\":[]},\"request\":{\"consumes\":[\"application/json\"],\"headerNames\":[],\"headers\":{},\"method\":\"POST\",\"paramNames\":[],\"params\":{},\"path\":\"/org.apache.dubbo.metadata.rest.RestService/headers\",\"produces\":[\"application/json\"]}}",
                "{\"argInfos\":[{\"annotationNameAttribute\":\"user\",\"formContentType\":false,\"index\":0,\"paramAnnotationType\":\"org.apache.dubbo.metadata.rest.tag.NoAnnotationTag\",\"paramName\":\"user\",\"paramType\":\"org.apache.dubbo.metadata.rest.User\",\"urlSplitIndex\":0}],\"codeStyle\":\"org.apache.dubbo.metadata.rest.noannotaion.NoAnnotationServiceRestMetadataResolver\",\"indexToName\":{0:[\"user\"]},\"method\":{\"annotations\":[],\"parameters\":[]},\"request\":{\"consumes\":[\"application/json\"],\"headerNames\":[],\"headers\":{},\"method\":\"POST\",\"paramNames\":[],\"params\":{},\"path\":\"/org.apache.dubbo.metadata.rest.RestService/noAnnotationFormBody\",\"produces\":[\"application/json\"]}}",
                "{\"argInfos\":[{\"annotationNameAttribute\":\"user\",\"formContentType\":false,\"index\":0,\"paramAnnotationType\":\"org.apache.dubbo.metadata.rest.tag.NoAnnotationTag\",\"paramName\":\"user\",\"paramType\":\"org.apache.dubbo.metadata.rest.User\",\"urlSplitIndex\":0}],\"codeStyle\":\"org.apache.dubbo.metadata.rest.noannotaion.NoAnnotationServiceRestMetadataResolver\",\"indexToName\":{0:[\"user\"]},\"method\":{\"annotations\":[],\"parameters\":[]},\"request\":{\"consumes\":[\"application/json\"],\"headerNames\":[],\"headers\":{},\"method\":\"POST\",\"paramNames\":[],\"params\":{},\"path\":\"/org.apache.dubbo.metadata.rest.RestService/noAnnotationJsonBody\",\"produces\":[\"application/json\"]}}",
                "{\"argInfos\":[{\"annotationNameAttribute\":\"text\",\"formContentType\":false,\"index\":0,\"paramAnnotationType\":\"org.apache.dubbo.metadata.rest.tag.NoAnnotationTag\",\"paramName\":\"text\",\"paramType\":\"java.lang.String\",\"urlSplitIndex\":0}],\"codeStyle\":\"org.apache.dubbo.metadata.rest.noannotaion.NoAnnotationServiceRestMetadataResolver\",\"indexToName\":{0:[\"text\"]},\"method\":{\"annotations\":[],\"parameters\":[]},\"request\":{\"consumes\":[\"application/json\"],\"headerNames\":[],\"headers\":{},\"method\":\"POST\",\"paramNames\":[],\"params\":{},\"path\":\"/org.apache.dubbo.metadata.rest.RestService/noAnnotationParam\",\"produces\":[\"application/json\"]}}",
                "{\"argInfos\":[{\"annotationNameAttribute\":\"param\",\"formContentType\":false,\"index\":0,\"paramAnnotationType\":\"org.apache.dubbo.metadata.rest.tag.NoAnnotationTag\",\"paramName\":\"param\",\"paramType\":\"java.lang.String\",\"urlSplitIndex\":0}],\"codeStyle\":\"org.apache.dubbo.metadata.rest.noannotaion.NoAnnotationServiceRestMetadataResolver\",\"indexToName\":{0:[\"param\"]},\"method\":{\"annotations\":[],\"parameters\":[]},\"request\":{\"consumes\":[\"application/json\"],\"headerNames\":[],\"headers\":{},\"method\":\"POST\",\"paramNames\":[],\"params\":{},\"path\":\"/org.apache.dubbo.metadata.rest.RestService/param\",\"produces\":[\"application/json\"]}}",
                "{\"argInfos\":[{\"annotationNameAttribute\":\"a\",\"formContentType\":false,\"index\":0,\"paramAnnotationType\":\"org.apache.dubbo.metadata.rest.tag.NoAnnotationTag\",\"paramName\":\"a\",\"paramType\":\"int\",\"urlSplitIndex\":0},{\"annotationNameAttribute\":\"b\",\"formContentType\":false,\"index\":1,\"paramAnnotationType\":\"org.apache.dubbo.metadata.rest.tag.NoAnnotationTag\",\"paramName\":\"b\",\"paramType\":\"java.lang.String\",\"urlSplitIndex\":0}],\"codeStyle\":\"org.apache.dubbo.metadata.rest.noannotaion.NoAnnotationServiceRestMetadataResolver\",\"indexToName\":{0:[\"a\"],1:[\"b\"]},\"method\":{\"annotations\":[],\"parameters\":[]},\"request\":{\"consumes\":[\"application/json\"],\"headerNames\":[],\"headers\":{},\"method\":\"POST\",\"paramNames\":[],\"params\":{},\"path\":\"/org.apache.dubbo.metadata.rest.RestService/params\",\"produces\":[\"application/json\"]}}",
                "{\"argInfos\":[{\"annotationNameAttribute\":\"path1\",\"formContentType\":false,\"index\":0,\"paramAnnotationType\":\"org.apache.dubbo.metadata.rest.tag.NoAnnotationTag\",\"paramName\":\"path1\",\"paramType\":\"java.lang.String\",\"urlSplitIndex\":0},{\"annotationNameAttribute\":\"path2\",\"formContentType\":false,\"index\":1,\"paramAnnotationType\":\"org.apache.dubbo.metadata.rest.tag.NoAnnotationTag\",\"paramName\":\"path2\",\"paramType\":\"java.lang.String\",\"urlSplitIndex\":0},{\"annotationNameAttribute\":\"param\",\"formContentType\":false,\"index\":2,\"paramAnnotationType\":\"org.apache.dubbo.metadata.rest.tag.NoAnnotationTag\",\"paramName\":\"param\",\"paramType\":\"java.lang.String\",\"urlSplitIndex\":0}],\"codeStyle\":\"org.apache.dubbo.metadata.rest.noannotaion.NoAnnotationServiceRestMetadataResolver\",\"indexToName\":{0:[\"path1\"],1:[\"path2\"],2:[\"param\"]},\"method\":{\"annotations\":[],\"parameters\":[]},\"request\":{\"consumes\":[\"application/json\"],\"headerNames\":[],\"headers\":{},\"method\":\"POST\",\"paramNames\":[],\"params\":{},\"path\":\"/org.apache.dubbo.metadata.rest.RestService/pathVariables\",\"produces\":[\"application/json\"]}}",
                "{\"argInfos\":[{\"annotationNameAttribute\":\"data\",\"formContentType\":false,\"index\":0,\"paramAnnotationType\":\"org.apache.dubbo.metadata.rest.tag.NoAnnotationTag\",\"paramName\":\"data\",\"paramType\":\"java.util.Map\",\"urlSplitIndex\":0},{\"annotationNameAttribute\":\"param\",\"formContentType\":false,\"index\":1,\"paramAnnotationType\":\"org.apache.dubbo.metadata.rest.tag.NoAnnotationTag\",\"paramName\":\"param\",\"paramType\":\"java.lang.String\",\"urlSplitIndex\":0}],\"codeStyle\":\"org.apache.dubbo.metadata.rest.noannotaion.NoAnnotationServiceRestMetadataResolver\",\"indexToName\":{0:[\"data\"],1:[\"param\"]},\"method\":{\"annotations\":[],\"parameters\":[]},\"request\":{\"consumes\":[\"application/json\"],\"headerNames\":[],\"headers\":{},\"method\":\"POST\",\"paramNames\":[],\"params\":{},\"path\":\"/org.apache.dubbo.metadata.rest.RestService/requestBodyMap\",\"produces\":[\"application/json\"]}}",
                "{\"argInfos\":[{\"annotationNameAttribute\":\"user\",\"formContentType\":false,\"index\":0,\"paramAnnotationType\":\"org.apache.dubbo.metadata.rest.tag.NoAnnotationTag\",\"paramName\":\"user\",\"paramType\":\"org.apache.dubbo.metadata.rest.User\",\"urlSplitIndex\":0}],\"codeStyle\":\"org.apache.dubbo.metadata.rest.noannotaion.NoAnnotationServiceRestMetadataResolver\",\"indexToName\":{0:[\"user\"]},\"method\":{\"annotations\":[],\"parameters\":[]},\"request\":{\"consumes\":[\"application/json\"],\"headerNames\":[],\"headers\":{},\"method\":\"POST\",\"paramNames\":[],\"params\":{},\"path\":\"/org.apache.dubbo.metadata.rest.RestService/requestBodyUser\",\"produces\":[\"application/json\"]}}");

        boolean supports = instance.supports(DefaultRestService.class);

        Assertions.assertEquals(true, supports);

        ServiceRestMetadata serviceRestMetadata = instance.resolve(DefaultRestService.class);

        List<String> jsonsTmp = new ArrayList<>();
        for (RestMethodMetadata restMethodMetadata : serviceRestMetadata.getMeta()) {
            restMethodMetadata.setReflectMethod(null);
            restMethodMetadata.setMethod(null);
            jsonsTmp.add(JsonUtils.toJson(restMethodMetadata));
        }

        Comparator<String> comparator = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.length() - o2.length();
            }
        };
        jsons.sort(comparator);
        jsonsTmp.sort(comparator);

        for (int i = 0; i < jsons.size(); i++) {
            assertEquals(jsons.get(i), jsonsTmp.get(i));
        }
    }
}
