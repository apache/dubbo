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
package org.apache.dubbo.remoting.zookeeper;
import org.apache.dubbo.common.extension.ExtensionLoader;
public class ZookeeperTransporter$Adaptive implements org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter {
public org.apache.dubbo.remoting.zookeeper.ZookeeperClient connect(org.apache.dubbo.common.URL arg0)  {
if (arg0 == null) throw new IllegalArgumentException("url == null");
org.apache.dubbo.common.URL url = arg0;
String extName = url.getParameter("client", url.getParameter("transporter", "curator"));
if(extName == null) throw new IllegalStateException("Failed to get extension (org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter) name from url (" + url.toString() + ") use keys([client, transporter])");
org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter extension = (org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter)ExtensionLoader.getExtensionLoader(org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter.class).getExtension(extName);
return extension.connect(arg0);
}
}
