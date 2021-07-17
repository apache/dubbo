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
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.support.AbstractMetadataReportFactory;

import static org.apache.dubbo.common.constants.CommonConstants.CONFIG_NAMESPACE_KEY;

/**
 * metadata report factory impl for nacos
 */
public class NacosMetadataReportFactory extends AbstractMetadataReportFactory {
    @Override
    protected MetadataReport createMetadataReport(URL url) {
        URL nacosURL = url;
        if (CommonConstants.DUBBO.equals(url.getParameter(CONFIG_NAMESPACE_KEY))) {
            // ignore "dubbo" namespace, make the behavior equivalent to configcenter
            nacosURL = url.removeParameter(CONFIG_NAMESPACE_KEY);
        }
        return new NacosMetadataReport(nacosURL);
    }
}
