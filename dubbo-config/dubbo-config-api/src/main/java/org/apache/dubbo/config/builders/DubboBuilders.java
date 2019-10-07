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
package org.apache.dubbo.config.builders;

/**
 * The tools for creat builder
 *
 * @since 2.7
 */
public class DubboBuilders {
    public static <T> ServiceBuilder<T> serviceBuilder() {
        return new ServiceBuilder<>();
    }

    public static ApplicationBuilder applicationBuilder() {
        return new ApplicationBuilder();
    }

    public static ConfigCenterBuilder configCenterBuilder() {
        return new ConfigCenterBuilder();
    }

    public static ConsumerBuilder consumerBuilder() {
        return new ConsumerBuilder();
    }

    public static MetadataReportBuilder metadataReportBuilder() {
        return new MetadataReportBuilder();
    }

    public static MethodBuilder methodBuilder() {
        return new MethodBuilder();
    }

    public static MonitorBuilder monitorBuilder() {
        return new MonitorBuilder();
    }

    public static ProviderBuilder providerBuilder() {
        return new ProviderBuilder();
    }

    public static ProtocolBuilder protocolBuilder() {
        return new ProtocolBuilder();
    }

    public static <T> ReferenceBuilder<T> referenceBuilder() {
        return new ReferenceBuilder<T>();
    }

    public static RegistryBuilder registryBuilder() {
        return new RegistryBuilder();
    }

    public static ArgumentBuilder argumentBuilder() {
        return new ArgumentBuilder();
    }
}
