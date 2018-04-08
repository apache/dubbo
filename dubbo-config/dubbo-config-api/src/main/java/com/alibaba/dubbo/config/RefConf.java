/*
 * Copyright 1999-2012 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.config;

/**
 * Reference  注解对应的实体配置类
 * 
 * @author zhouzhipeng
 */
public class RefConf {

   public Class<?> interfaceClass = void.class;

   public String interfaceName = "";

   public String version = "";

   public String group = "";

   public String url = "";

   public String client = "";

   public boolean generic = false;

   public boolean injvm = false;

   public boolean check = true;

   public boolean init = false;

   public boolean lazy = false;

   public boolean stubevent = false;

   public String reconnect = "";

   public boolean sticky = false;

   public String proxy = "";

   public String stub = "";

   public String cluster = "";

   public int connections = 0;

   public int callbacks = 0;

   public String onconnect = "";

   public String ondisconnect = "";

   public String owner = "";

   public String layer = "";

   public int retries = 0;

   public String loadbalance = "";

   public boolean async = false;

   public int actives = 0;

   public boolean sent = false;

   public String mock = "";

   public String validation = "";

   public int timeout = 0;

   public String cache = "";

   public String[] filter = {};

   public String[] listener = {};

   public String[] parameters = {};

   public String application = "";

   public String module = "";

   public String consumer = "";

   public String monitor = "";

   public String protocol = "";

   public String[] registry = {};

}
