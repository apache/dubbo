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
package org.apache.dubbo.metadata.store.nacos;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.support.AbstractMetadataReportFactory;

import static org.apache.dubbo.metadata.MetadataConstants.NAMESPACE_KEY;

/**
 * metadata report factory impl for nacos
 */
public class NacosMetadataReportFactory extends AbstractMetadataReportFactory {
    @Override
    protected MetadataReport createMetadataReport(URL url) {
        return new NacosMetadataReport(url);
    }

    @Override
    protected String toMetadataReportKey(URL url) {
        String namespace = url.getParameter(NAMESPACE_KEY);
        if (!StringUtils.isEmpty(namespace)) {
            return URL.valueOf(url.toServiceString())
                    .addParameter(NAMESPACE_KEY, namespace)
                    .toString();
        }
        return super.toMetadataReportKey(url);
    }
}
