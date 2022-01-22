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
package org.apache.dubbo.metadata.report;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;
public class MetadataReportFactory$Adaptive implements org.apache.dubbo.metadata.report.MetadataReportFactory {
public org.apache.dubbo.metadata.report.MetadataReport getMetadataReport(org.apache.dubbo.common.URL arg0)  {
if (arg0 == null) throw new IllegalArgumentException("url == null");
org.apache.dubbo.common.URL url = arg0;
String extName = ( url.getProtocol() == null ? "redis" : url.getProtocol() );
if(extName == null) throw new IllegalStateException("Failed to get extension (org.apache.dubbo.metadata.report.MetadataReportFactory) name from url (" + url.toString() + ") use keys([protocol])");
ScopeModel scopeModel = ScopeModelUtil.getOrDefault(url.getScopeModel(), org.apache.dubbo.metadata.report.MetadataReportFactory.class);
org.apache.dubbo.metadata.report.MetadataReportFactory extension = (org.apache.dubbo.metadata.report.MetadataReportFactory)scopeModel.getExtensionLoader(org.apache.dubbo.metadata.report.MetadataReportFactory.class).getExtension(extName);
return extension.getMetadataReport(arg0);
}
public void destroy()  {
throw new UnsupportedOperationException("The method public abstract void org.apache.dubbo.metadata.report.MetadataReportFactory.destroy() of interface org.apache.dubbo.metadata.report.MetadataReportFactory is not adaptive method!");
}
}
