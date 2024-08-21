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
package org.apache.dubbo.common.json.impl;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.json.impl.FastJsonImpl.ReaderConfig;
import org.apache.dubbo.common.json.impl.FastJsonImpl.WriterConfig;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.JSONLexer;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.ExtraProcessor;
import com.alibaba.fastjson.parser.deserializer.ExtraTypeProvider;
import com.alibaba.fastjson.parser.deserializer.FieldTypeResolver;
import com.alibaba.fastjson.parser.deserializer.ParseProcess;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializeFilter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.util.TypeUtils;

@Activate(order = 200, onClass = "com.alibaba.fastjson.JSON")
public class FastJsonImpl extends CustomizableJsonUtil<ReaderConfig, WriterConfig> {

    @Override
    public String getName() {
        return "fastjson";
    }

    @Override
    public boolean isJson(String json) {
        try {
            Object obj = JSON.parse(json);
            return obj instanceof JSONObject || obj instanceof JSONArray;
        } catch (JSONException e) {
            return false;
        }
    }

    @Override
    public <T> T toJavaObject(String json, Type type) {
        if (hasCustomizer()) {
            ReaderConfig conf = getReaderConfig();
            return JSON.parseObject(
                    json, type, conf.getConfig(), conf.getProcessor(), conf.getFeatureValues(), conf.getFeatures());
        }

        return JSON.parseObject(json, type);
    }

    @Override
    public <T> List<T> toJavaList(String json, Class<T> clazz) {
        if (hasCustomizer()) {
            ReaderConfig conf = getReaderConfig();
            return parseArray(
                    json, clazz, conf.getConfig(), conf.getProcessor(), conf.getFeatureValues(), conf.getFeatures());
        }

        return JSON.parseArray(json, clazz);
    }

    @Override
    public String toJson(Object obj) {
        if (hasCustomizer()) {
            WriterConfig conf = getWriterConfig();
            int features = conf.getDefaultFeatures();
            features |= SerializerFeature.DisableCircularReferenceDetect.getMask();
            return JSON.toJSONString(
                    obj, conf.getConfig(), conf.getFilters(), conf.getDateFormat(), features, conf.getFeatures());
        }

        return JSON.toJSONString(obj, SerializerFeature.DisableCircularReferenceDetect);
    }

    @Override
    public String toPrettyJson(Object obj) {
        if (hasCustomizer()) {
            WriterConfig conf = getWriterConfig();
            int features = conf.getDefaultFeatures();
            features |= SerializerFeature.DisableCircularReferenceDetect.getMask();
            features |= SerializerFeature.PrettyFormat.getMask();
            return JSON.toJSONString(
                    obj, conf.getConfig(), conf.getFilters(), conf.getDateFormat(), features, conf.getFeatures());
        }

        return JSON.toJSONString(obj, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat);
    }

    @Override
    public Object convertObject(Object obj, Type type) {
        if (hasCustomizer()) {
            return TypeUtils.cast(obj, type, getReaderConfig().getConfig());
        }

        return TypeUtils.cast(obj, type, ParserConfig.getGlobalInstance());
    }

    @Override
    public Object convertObject(Object obj, Class<?> clazz) {
        if (hasCustomizer()) {
            return TypeUtils.cast(obj, clazz, getReaderConfig().getConfig());
        }

        return TypeUtils.cast(obj, clazz, ParserConfig.getGlobalInstance());
    }

    public static <T> List<T> parseArray(
            String text,
            Class<T> clazz,
            ParserConfig config,
            ParseProcess processor,
            int featureValues,
            Feature... features) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        if (features != null) {
            for (Feature feature : features) {
                featureValues |= feature.mask;
            }
        }

        DefaultJSONParser parser = new DefaultJSONParser(text, config, featureValues);

        if (processor != null) {
            if (processor instanceof ExtraTypeProvider) {
                parser.getExtraTypeProviders().add((ExtraTypeProvider) processor);
            }

            if (processor instanceof ExtraProcessor) {
                parser.getExtraProcessors().add((ExtraProcessor) processor);
            }

            if (processor instanceof FieldTypeResolver) {
                parser.setFieldTypeResolver((FieldTypeResolver) processor);
            }
        }

        List<T> list;

        JSONLexer lexer = parser.lexer;
        int token = lexer.token();
        if (token == JSONToken.NULL) {
            lexer.nextToken();
            list = null;
        } else if (token == JSONToken.EOF && lexer.isBlankInput()) {
            list = null;
        } else {
            list = new ArrayList<T>();
            parser.parseArray(clazz, list);

            parser.handleResovleTask(list);
        }

        parser.close();

        return list;
    }

    protected ReaderConfig getReaderConfig() {
        return getFirst();
    }

    protected WriterConfig getWriterConfig() {
        return getSecond();
    }

    @Override
    protected ReaderConfig newFirst() {
        return new ReaderConfig();
    }

    @Override
    protected WriterConfig newSecond() {
        return new WriterConfig();
    }

    public static final class ReaderConfig {

        private ParserConfig config;
        private ParseProcess processor;
        private int featureValues = JSON.DEFAULT_PARSER_FEATURE;
        private Feature[] features;

        public ParserConfig getConfig() {
            return config;
        }

        public void setConfig(ParserConfig config) {
            this.config = config;
        }

        public ParseProcess getProcessor() {
            return processor;
        }

        public void setProcessor(ParseProcess processor) {
            this.processor = processor;
        }

        public int getFeatureValues() {
            return featureValues;
        }

        public void setFeatureValues(int featureValues) {
            this.featureValues = featureValues;
        }

        public Feature[] getFeatures() {
            return features;
        }

        public void setFeatures(Feature... features) {
            this.features = features;
        }
    }

    public static final class WriterConfig {

        private SerializeConfig config;
        private SerializeFilter[] filters;
        private String dateFormat;
        private int defaultFeatures = JSON.DEFAULT_GENERATE_FEATURE;
        private SerializerFeature[] features;

        public SerializeConfig getConfig() {
            return config;
        }

        public void setConfig(SerializeConfig config) {
            this.config = config;
        }

        public SerializeFilter[] getFilters() {
            return filters;
        }

        public void setFilters(SerializeFilter[] filters) {
            this.filters = filters;
        }

        public String getDateFormat() {
            return dateFormat;
        }

        public void setDateFormat(String dateFormat) {
            this.dateFormat = dateFormat;
        }

        public int getDefaultFeatures() {
            return defaultFeatures;
        }

        public void setDefaultFeatures(int defaultFeatures) {
            this.defaultFeatures = defaultFeatures;
        }

        public SerializerFeature[] getFeatures() {
            return features;
        }

        public void setFeatures(SerializerFeature... features) {
            this.features = features;
        }
    }
}
