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
package org.apache.dubbo.remoting;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;
public class Codec2$Adaptive implements org.apache.dubbo.remoting.Codec2 {
public java.lang.Object decode(org.apache.dubbo.remoting.Channel arg0, org.apache.dubbo.remoting.buffer.ChannelBuffer arg1) throws java.io.IOException {
if (arg0 == null) throw new IllegalArgumentException("org.apache.dubbo.remoting.Channel argument == null");
if (arg0.getUrl() == null) throw new IllegalArgumentException("org.apache.dubbo.remoting.Channel argument getUrl() == null");
org.apache.dubbo.common.URL url = arg0.getUrl();
String extName = url.getParameter("codec", "adaptive");
if(extName == null) throw new IllegalStateException("Failed to get extension (org.apache.dubbo.remoting.Codec2) name from url (" + url.toString() + ") use keys([codec])");
ScopeModel scopeModel = ScopeModelUtil.getOrDefault(url.getScopeModel(), org.apache.dubbo.remoting.Codec2.class);
org.apache.dubbo.remoting.Codec2 extension = (org.apache.dubbo.remoting.Codec2)scopeModel.getExtensionLoader(org.apache.dubbo.remoting.Codec2.class).getExtension(extName);
return extension.decode(arg0, arg1);
}
public void encode(org.apache.dubbo.remoting.Channel arg0, org.apache.dubbo.remoting.buffer.ChannelBuffer arg1, java.lang.Object arg2) throws java.io.IOException {
if (arg0 == null) throw new IllegalArgumentException("org.apache.dubbo.remoting.Channel argument == null");
if (arg0.getUrl() == null) throw new IllegalArgumentException("org.apache.dubbo.remoting.Channel argument getUrl() == null");
org.apache.dubbo.common.URL url = arg0.getUrl();
String extName = url.getParameter("codec", "adaptive");
if(extName == null) throw new IllegalStateException("Failed to get extension (org.apache.dubbo.remoting.Codec2) name from url (" + url.toString() + ") use keys([codec])");
ScopeModel scopeModel = ScopeModelUtil.getOrDefault(url.getScopeModel(), org.apache.dubbo.remoting.Codec2.class);
org.apache.dubbo.remoting.Codec2 extension = (org.apache.dubbo.remoting.Codec2)scopeModel.getExtensionLoader(org.apache.dubbo.remoting.Codec2.class).getExtension(extName);
extension.encode(arg0, arg1, arg2);
}
}
