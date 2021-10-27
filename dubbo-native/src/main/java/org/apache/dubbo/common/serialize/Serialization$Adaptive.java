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
package org.apache.dubbo.common.serialize;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;
public class Serialization$Adaptive implements org.apache.dubbo.common.serialize.Serialization {
public byte getContentTypeId()  {
throw new UnsupportedOperationException("The method public abstract byte org.apache.dubbo.common.serialize.Serialization.getContentTypeId() of interface org.apache.dubbo.common.serialize.Serialization is not adaptive method!");
}
public java.lang.String getContentType()  {
throw new UnsupportedOperationException("The method public abstract java.lang.String org.apache.dubbo.common.serialize.Serialization.getContentType() of interface org.apache.dubbo.common.serialize.Serialization is not adaptive method!");
}
public org.apache.dubbo.common.serialize.ObjectInput deserialize(org.apache.dubbo.common.URL arg0, java.io.InputStream arg1) throws java.io.IOException {
if (arg0 == null) throw new IllegalArgumentException("url == null");
org.apache.dubbo.common.URL url = arg0;
String extName = url.getParameter("serialization", "hessian2");
if(extName == null) throw new IllegalStateException("Failed to get extension (org.apache.dubbo.common.serialize.Serialization) name from url (" + url.toString() + ") use keys([serialization])");
ScopeModel scopeModel = ScopeModelUtil.getOrDefault(url.getScopeModel(), org.apache.dubbo.common.serialize.Serialization.class);
org.apache.dubbo.common.serialize.Serialization extension = (org.apache.dubbo.common.serialize.Serialization)scopeModel.getExtensionLoader(org.apache.dubbo.common.serialize.Serialization.class).getExtension(extName);
return extension.deserialize(arg0, arg1);
}
public org.apache.dubbo.common.serialize.ObjectOutput serialize(org.apache.dubbo.common.URL arg0, java.io.OutputStream arg1) throws java.io.IOException {
if (arg0 == null) throw new IllegalArgumentException("url == null");
org.apache.dubbo.common.URL url = arg0;
String extName = url.getParameter("serialization", "hessian2");
if(extName == null) throw new IllegalStateException("Failed to get extension (org.apache.dubbo.common.serialize.Serialization) name from url (" + url.toString() + ") use keys([serialization])");
ScopeModel scopeModel = ScopeModelUtil.getOrDefault(url.getScopeModel(), org.apache.dubbo.common.serialize.Serialization.class);
org.apache.dubbo.common.serialize.Serialization extension = (org.apache.dubbo.common.serialize.Serialization)scopeModel.getExtensionLoader(org.apache.dubbo.common.serialize.Serialization.class).getExtension(extName);
return extension.serialize(arg0, arg1);
}
}
